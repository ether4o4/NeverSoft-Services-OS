package com.ether4o4.morsvitaest

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
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
    private var reserveView: View? = null
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
        listOf("?123", "PC", ",", "⌄", "space", ".", "⏎"),
    )

    private val symbolRows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("@", "#", "\$", "_", "&", "-", "+", "(", ")"),
        listOf("*", "\"", "'", ":", ";", "!", "?", "/", "⌫"),
        listOf("ABC", "PC", ",", "⌄", "space", ".", "⏎"),
    )

    // Hacker's-Keyboard-style terminal layout.
    private val pcRows = listOf(
        listOf("Esc", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "⌫"),
        listOf("Tab", "q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("Ctrl", "a", "s", "d", "f", "g", "h", "j", "k", "l", "⏎"),
        listOf("Alt", "z", "x", "c", "v", "b", "n", "m", "/", "↑"),
        listOf("ABC", "⇧", "Del", "⌄", "space", "←", "↓", "→"),
    )

    override fun onCreateInputView(): View {
        colors = themeColors()
        // The keys carry the keyboard background; below them sits a TRANSPARENT spacer the
        // height of the taskbar, so the persistent taskbar shows through it instead of being
        // hidden behind the keyboard's own background. The keys rest on the taskbar's top edge.
        val keys = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colors.bg)
            setPadding(dp(3), dp(6), dp(3), dp(4))
        }
        rootView = keys
        val reserve = View(this)
        reserveView = reserve
        renderRows()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                keys,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
            addView(reserve, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, taskbarReservePx()))
        }
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        // Re-read the theme each time, so changing the launcher theme reflects here too.
        colors = themeColors()
        rootView?.setBackgroundColor(colors.bg)
        reserveView?.let { v ->
            v.layoutParams = v.layoutParams.apply { height = taskbarReservePx() }
            v.requestLayout()
        }
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
        val keyH = if (mode == Mode.PC) dp(48) else dp(56)
        for (row in rowsFor()) {
            val rowView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            for (key in row) {
                val weight = when (key) {
                    "space" -> if (mode == Mode.PC) 3f else 4f
                    "⌄" -> 0.9f
                    "⇧", "⌫", "?123", "ABC", "PC", "⏎", "Ctrl", "Alt", "Tab", "Esc", "Del" -> 1.5f
                    else -> 1f
                }
                rowView.addView(
                    keyView(key),
                    LinearLayout.LayoutParams(0, keyH, weight).apply {
                        setMargins(dp(2), dp(4), dp(2), dp(4))
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

    private fun isSpecial(key: String) = key in setOf("⇧", "⌫", "?123", "ABC", "PC", "⏎", "Ctrl", "Alt", "Tab", "Esc", "Del", "⌄", "↑", "↓", "←", "→")

    /** Highlight active sticky modifiers. */
    private fun isActiveModifier(key: String) = (key == "Ctrl" && ctrl) || (key == "Alt" && alt) || (key == "⇧" && shifted)

    @SuppressLint("ClickableViewAccessibility")
    private fun keyView(key: String): TextView = TextView(this).apply {
        text = label(key)
        setTextColor(colors.text)
        textSize = if (key.length > 1 && key != "space") {
            12f
        } else if (mode == Mode.PC) {
            15f
        } else {
            18f
        }
        gravity = Gravity.CENTER
        // Resolve the fill here: inside GradientDrawable.apply, `colors` would bind to
        // GradientDrawable.colors (the gradient array), not this service's colors.
        val fill = when {
            isActiveModifier(key) -> colors.accent
            isSpecial(key) -> colors.special
            else -> colors.key
        }
        val normalBg = GradientDrawable().apply {
            cornerRadius = dp(7).toFloat()
            setColor(fill)
        }
        val pressedBg = GradientDrawable().apply {
            cornerRadius = dp(7).toFloat()
            setColor(blend(fill, Color.WHITE, 0.32f))
        }
        background = normalBg
        isClickable = true
        isHapticFeedbackEnabled = true
        // Fire on touch-DOWN like a real keyboard — snappier and far fewer missed taps
        // than waiting for a click — with a press highlight and a haptic tap so each key
        // feels hit.
        setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.background = pressedBg
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onKey(key)
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.background = normalBg
                    true
                }

                else -> false
            }
        }
    }

    private fun label(key: String): String = if (mode != Mode.SYMBOLS && shifted && key.length == 1 && key[0].isLetter()) key.uppercase() else key

    private fun onKey(key: String) {
        val ic = currentInputConnection ?: return
        when (key) {
            "⌫" -> sendKey(KeyEvent.KEYCODE_DEL)

            "Del" -> sendKey(KeyEvent.KEYCODE_FORWARD_DEL)

            "⌄" -> requestHideSelf(0)

            "space" -> handleSpace(ic)

            "⇧" -> {
                shifted = !shifted
                renderRows()
            }

            "?123" -> {
                mode = Mode.SYMBOLS
                renderRows()
            }

            "ABC" -> {
                mode = Mode.ABC
                ctrl = false
                alt = false
                renderRows()
            }

            "PC" -> {
                mode = Mode.PC
                renderRows()
            }

            "Esc" -> sendKey(KeyEvent.KEYCODE_ESCAPE)

            "Tab" -> sendKey(KeyEvent.KEYCODE_TAB)

            "Ctrl" -> {
                ctrl = !ctrl
                renderRows()
            }

            "Alt" -> {
                alt = !alt
                renderRows()
            }

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
        // Auto-capitalize the first letter of a sentence (light autocorrect).
        val autoCap = mode == Mode.ABC && !shifted && key.length == 1 && key[0].isLetter() && shouldAutoCap(ic)
        val cap = shifted || autoCap
        val out = if (mode != Mode.SYMBOLS && cap && key.length == 1) key.uppercase() else key
        ic.commitText(out, 1)
        if (shifted && mode == Mode.ABC) {
            shifted = false
            renderRows()
        }
    }

    /** Whether the next letter should be capitalized — start of text, or after a sentence end. */
    private fun shouldAutoCap(ic: InputConnection): Boolean {
        val before = ic.getTextBeforeCursor(2, 0)
        if (before.isNullOrEmpty()) return true
        val last = before.last()
        if (last == '\n') return true
        if (before.length >= 2 && last == ' ') {
            val prev = before[before.length - 2]
            if (prev == '.' || prev == '!' || prev == '?') return true
        }
        return false
    }

    /** Space key: fix a common typo in the just-typed word, and turn "word  " into "word. ". */
    private fun handleSpace(ic: InputConnection) {
        autocorrectLastWord(ic)
        val before = ic.getTextBeforeCursor(2, 0)
        if (before != null && before.length == 2 && before[1] == ' ' && before[0].isLetterOrDigit()) {
            // Double space → ". "
            ic.deleteSurroundingText(1, 0)
            ic.commitText(". ", 1)
        } else {
            ic.commitText(" ", 1)
        }
    }

    /** Replace the word right before the cursor if it's a known common typo/contraction. */
    private fun autocorrectLastWord(ic: InputConnection) {
        val text = ic.getTextBeforeCursor(40, 0)?.toString() ?: return
        if (text.isEmpty() || text.last().isWhitespace()) return
        val word = text.takeLastWhile { !it.isWhitespace() }
        if (word.isEmpty()) return
        val fix = AUTOCORRECT[word.lowercase()] ?: return
        val corrected = if (word.first().isUpperCase()) fix.replaceFirstChar { it.uppercaseChar() } else fix
        ic.deleteSurroundingText(word.length, 0)
        ic.commitText(corrected, 1)
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

        // A small, high-confidence autocorrect dictionary (common typos + contractions),
        // applied to the just-typed word when space is pressed. Deliberately conservative
        // so it rarely "corrects" something you meant.
        private val AUTOCORRECT = mapOf(
            "i" to "I",
            "teh" to "the", "hte" to "the", "taht" to "that", "waht" to "what", "adn" to "and",
            "nad" to "and", "jsut" to "just", "wnat" to "want", "knwo" to "know", "thnk" to "think",
            "agian" to "again", "becuase" to "because", "becasue" to "because", "wich" to "which",
            "thier" to "their", "recieve" to "receive", "seperate" to "separate",
            "definately" to "definitely", "occured" to "occurred", "untill" to "until",
            "tommorow" to "tomorrow", "wierd" to "weird", "freind" to "friend", "alot" to "a lot",
            "dont" to "don't", "cant" to "can't", "wont" to "won't", "isnt" to "isn't",
            "doesnt" to "doesn't", "didnt" to "didn't", "wasnt" to "wasn't", "couldnt" to "couldn't",
            "wouldnt" to "wouldn't", "shouldnt" to "shouldn't", "im" to "I'm", "ive" to "I've",
            "ill" to "I'll", "id" to "I'd", "youre" to "you're", "theyre" to "they're",
            "thats" to "that's", "whats" to "what's", "theres" to "there's", "hes" to "he's",
            "shes" to "she's",
        )
    }
}
