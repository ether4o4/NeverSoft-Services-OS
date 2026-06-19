package com.ether4o4.morsvitaest

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import com.ether4o4.morsvitaest.data.AppSettings
import com.ether4o4.morsvitaest.ui.launcher.NeverSoftAccent
import com.ether4o4.morsvitaest.ui.launcher.resolveLauncherTheme
import org.koin.java.KoinJavaComponent.getKoin

/**
 * MVE's own on-screen keyboard. Its whole reason to exist: it reserves the taskbar's
 * height as bottom padding, so its keys rest on the **top edge of the taskbar** instead
 * of underneath it — the way the system keyboard rests on the navigation pill. That's
 * the only way (short of root) to make a fixed bottom taskbar coexist with the keyboard,
 * because the active keyboard is the one thing that gets to decide where its own bottom is.
 *
 * Three layouts, switched by the bottom-left key like a normal keyboard's `?123`:
 *  - ABC: a compact QWERTY.
 *  - ?123: symbols.
 *  - PC: a Hacker's-Keyboard-style layout for terminals — Esc, Tab, Ctrl, Alt, arrows,
 *    and a number row, with Ctrl/Alt as sticky modifiers that combine with the next key.
 *
 * Classic Views (no Compose) so it stays light and reliable inside an InputMethodService.
 * Opt-in: the user selects it as their keyboard and can switch back any time.
 */
class MveKeyboardService : InputMethodService() {

    private enum class Mode { ABC, SYMBOLS, PC }

    private var mode = Mode.ABC
    private var shifted = false
    private var ctrl = false
    private var alt = false
    private var rootView: LinearLayout? = null
    private var colors: KbColors = fallbackColors()

    /** Keyboard colors derived from the launcher theme, so it matches the taskbar / Start menu / widgets. */
    private data class KbColors(val bg: Int, val key: Int, val special: Int, val text: Int, val accent: Int)

    private fun themeColors(): KbColors = try {
        val theme = resolveLauncherTheme(getKoin().get<AppSettings>().getLauncherTheme())
        val bg = if (theme.glass) 0xFF0E1117.toInt() else (0xFF000000.toInt() or (theme.panel.toArgb() and 0xFFFFFF))
        val text = theme.content.toArgb()
        KbColors(
            bg = bg,
            key = blend(bg, text, 0.16f),
            special = blend(bg, text, 0.07f),
            text = text,
            accent = NeverSoftAccent.toArgb(),
        )
    } catch (_: Exception) {
        fallbackColors()
    }

    private fun blend(base: Int, over: Int, frac: Float): Int {
        val ir = (Color.red(base) * (1 - frac) + Color.red(over) * frac).toInt()
        val ig = (Color.green(base) * (1 - frac) + Color.green(over) * frac).toInt()
        val ib = (Color.blue(base) * (1 - frac) + Color.blue(over) * frac).toInt()
        return Color.rgb(ir, ig, ib)
    }

    private fun fallbackColors(): KbColors = KbColors(
        bg = 0xFF0E1117.toInt(),
        key = 0xFF363C49.toInt(),
        special = 0xFF21262F.toInt(),
        text = Color.WHITE,
        accent = 0xFF1E6FB0.toInt(),
    )

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun navBarHeightPx(): Int {
        val id = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }

    /** The strip the taskbar occupies at the very bottom; the keys sit above it. */
    private fun taskbarReservePx(): Int = dp(TASKBAR_DP) + navBarHeightPx()

    private val abcRows = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫"),
        listOf("?123", "PC", ",", "space", ".", "⏎"),
    )

    private val symbolRows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("@", "#", "\$", "_", "&", "-", "+", "(", ")"),
        listOf("*", "\"", "'", ":", ";", "!", "?", "/", "⌫"),
        listOf("ABC", "PC", ",", "space", ".", "⏎"),
    )

    // Hacker's-Keyboard-style terminal layout.
    private val pcRows = listOf(
        listOf("Esc", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "⌫"),
        listOf("Tab", "q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("Ctrl", "a", "s", "d", "f", "g", "h", "j", "k", "l", "⏎"),
        listOf("Alt", "z", "x", "c", "v", "b", "n", "m", "/", "↑"),
        listOf("ABC", "⇧", "Del", "space", "←", "↓", "→"),
    )

    override fun onCreateInputView(): View {
        colors = themeColors()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colors.bg)
            // The crucial bit: reserve the taskbar strip so the keys rest on its top edge.
            setPadding(dp(3), dp(6), dp(3), taskbarReservePx() + dp(4))
        }
        rootView = root
        renderRows()
        return root
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        // Re-read the theme each time, so changing the launcher theme reflects here too.
        colors = themeColors()
        rootView?.setBackgroundColor(colors.bg)
        rootView?.setPadding(dp(3), dp(6), dp(3), taskbarReservePx() + dp(4))
        renderRows()
    }

    private fun rowsFor(): List<List<String>> = when (mode) {
        Mode.ABC -> abcRows
        Mode.SYMBOLS -> symbolRows
        Mode.PC -> pcRows
    }

    private fun renderRows() {
        val root = rootView ?: return
        root.removeAllViews()
        val keyH = if (mode == Mode.PC) dp(42) else dp(48)
        for (row in rowsFor()) {
            val rowView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            for (key in row) {
                val weight = when (key) {
                    "space" -> if (mode == Mode.PC) 3f else 4f
                    "⇧", "⌫", "?123", "ABC", "PC", "⏎", "Ctrl", "Alt", "Tab", "Esc", "Del" -> 1.5f
                    else -> 1f
                }
                rowView.addView(
                    keyView(key),
                    LinearLayout.LayoutParams(0, keyH, weight).apply {
                        setMargins(dp(2), dp(3), dp(2), dp(3))
                    },
                )
            }
            root.addView(
                rowView,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
    }

    private fun isSpecial(key: String) =
        key in setOf("⇧", "⌫", "?123", "ABC", "PC", "⏎", "Ctrl", "Alt", "Tab", "Esc", "Del", "↑", "↓", "←", "→")

    /** Highlight active sticky modifiers. */
    private fun isActiveModifier(key: String) =
        (key == "Ctrl" && ctrl) || (key == "Alt" && alt) || (key == "⇧" && shifted)

    private fun keyView(key: String): TextView = TextView(this).apply {
        text = label(key)
        setTextColor(colors.text)
        textSize = if (key.length > 1 && key != "space") 12f else if (mode == Mode.PC) 15f else 18f
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            cornerRadius = dp(7).toFloat()
            setColor(
                when {
                    isActiveModifier(key) -> colors.accent
                    isSpecial(key) -> colors.special
                    else -> colors.key
                },
            )
        }
        isClickable = true
        setOnClickListener { onKey(key) }
    }

    private fun label(key: String): String =
        if (mode != Mode.SYMBOLS && shifted && key.length == 1 && key[0].isLetter()) key.uppercase() else key

    private fun onKey(key: String) {
        val ic = currentInputConnection ?: return
        when (key) {
            "⌫" -> sendKey(KeyEvent.KEYCODE_DEL)
            "Del" -> sendKey(KeyEvent.KEYCODE_FORWARD_DEL)
            "space" -> ic.commitText(" ", 1)
            "⇧" -> { shifted = !shifted; renderRows() }
            "?123" -> { mode = Mode.SYMBOLS; renderRows() }
            "ABC" -> { mode = Mode.ABC; ctrl = false; alt = false; renderRows() }
            "PC" -> { mode = Mode.PC; renderRows() }
            "Esc" -> sendKey(KeyEvent.KEYCODE_ESCAPE)
            "Tab" -> sendKey(KeyEvent.KEYCODE_TAB)
            "Ctrl" -> { ctrl = !ctrl; renderRows() }
            "Alt" -> { alt = !alt; renderRows() }
            "←" -> sendKey(KeyEvent.KEYCODE_DPAD_LEFT)
            "→" -> sendKey(KeyEvent.KEYCODE_DPAD_RIGHT)
            "↑" -> sendKey(KeyEvent.KEYCODE_DPAD_UP)
            "↓" -> sendKey(KeyEvent.KEYCODE_DPAD_DOWN)
            "⏎" -> {
                if (mode == Mode.PC) {
                    sendKey(KeyEvent.KEYCODE_ENTER)
                } else {
                    val opts = currentInputEditorInfo?.imeOptions ?: 0
                    val action = opts and EditorInfo.IME_MASK_ACTION
                    if (action != EditorInfo.IME_ACTION_NONE && (opts and EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) {
                        ic.performEditorAction(action)
                    } else {
                        ic.commitText("\n", 1)
                    }
                }
            }
            else -> typeKey(key)
        }
    }

    /** A normal character key. In PC mode with Ctrl/Alt held, send it as a key combo. */
    private fun typeKey(key: String) {
        val ic = currentInputConnection ?: return
        if (mode == Mode.PC && (ctrl || alt)) {
            val code = keyCodeFor(key)
            if (code != 0) {
                var meta = 0
                if (ctrl) meta = meta or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
                if (alt) meta = meta or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
                sendKey(code, meta)
                ctrl = false
                alt = false
                renderRows()
                return
            }
        }
        val out = if (mode != Mode.SYMBOLS && shifted && key.length == 1) key.uppercase() else key
        ic.commitText(out, 1)
        if (shifted && mode == Mode.ABC) { shifted = false; renderRows() }
    }

    private fun keyCodeFor(key: String): Int {
        if (key.length != 1) return 0
        val c = key[0]
        return when (c) {
            in 'a'..'z' -> KeyEvent.KEYCODE_A + (c - 'a')
            in '0'..'9' -> KeyEvent.KEYCODE_0 + (c - '0')
            else -> 0
        }
    }

    private fun sendKey(keyCode: Int, meta: Int = 0) {
        val ic = currentInputConnection ?: return
        val t = System.currentTimeMillis()
        ic.sendKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, keyCode, 0, meta))
        ic.sendKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_UP, keyCode, 0, meta))
    }

    companion object {
        // Matches OverlayTaskbarService.BAR_HEIGHT_DP so the keys land on the taskbar's top.
        private const val TASKBAR_DP = 50
    }
}
