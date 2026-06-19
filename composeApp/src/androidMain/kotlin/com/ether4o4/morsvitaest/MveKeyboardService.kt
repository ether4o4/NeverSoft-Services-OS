package com.ether4o4.morsvitaest

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView

/**
 * MVE's own on-screen keyboard. Its whole reason to exist: it reserves the taskbar's
 * height as bottom padding, so its keys rest on the **top edge of the taskbar** instead
 * of underneath it — the way the system keyboard rests on the navigation pill. That's
 * the only way (short of root) to make a fixed bottom taskbar coexist with the keyboard,
 * because the active keyboard is the one thing that gets to decide where its own bottom is.
 *
 * Deliberately a compact, classic-View QWERTY (no Compose) so it stays light and reliable
 * inside an InputMethodService. The user opts in by selecting it as their keyboard; they
 * can switch back to their usual one any time.
 */
class MveKeyboardService : InputMethodService() {

    private var shifted = false
    private var symbols = false
    private var rootView: LinearLayout? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun navBarHeightPx(): Int {
        val id = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }

    /** The strip the taskbar occupies at the very bottom; the keys sit above it. */
    private fun taskbarReservePx(): Int = dp(TASKBAR_DP) + navBarHeightPx()

    private val letterRows = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫"),
        listOf("?123", ",", "space", ".", "⏎"),
    )

    private val symbolRows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("@", "#", "\$", "_", "&", "-", "+", "(", ")"),
        listOf("*", "\"", "'", ":", ";", "!", "?", "/", "⌫"),
        listOf("ABC", ",", "space", ".", "⏎"),
    )

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0E1117"))
            // The crucial bit: reserve the taskbar strip so the keys rest on its top edge.
            setPadding(dp(3), dp(6), dp(3), taskbarReservePx() + dp(4))
        }
        rootView = root
        renderRows()
        return root
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        // Padding could change if the nav bar geometry changed since last shown.
        rootView?.setPadding(dp(3), dp(6), dp(3), taskbarReservePx() + dp(4))
    }

    private fun renderRows() {
        val root = rootView ?: return
        root.removeAllViews()
        val rows = if (symbols) symbolRows else letterRows
        for (row in rows) {
            val rowView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            for (key in row) {
                val weight = when (key) {
                    "space" -> 4f
                    "⇧", "⌫", "?123", "ABC", "⏎" -> 1.6f
                    else -> 1f
                }
                rowView.addView(
                    keyView(key),
                    LinearLayout.LayoutParams(0, dp(48), weight).apply {
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

    private fun isSpecial(key: String) = key in setOf("⇧", "⌫", "?123", "ABC", "⏎")

    private fun keyView(key: String): TextView = TextView(this).apply {
        text = label(key)
        setTextColor(Color.WHITE)
        textSize = if (key.length > 1 && key != "space") 13f else 18f
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            cornerRadius = dp(7).toFloat()
            setColor(Color.parseColor(if (isSpecial(key)) "#21262F" else "#363C49"))
        }
        isClickable = true
        setOnClickListener { onKey(key) }
    }

    private fun label(key: String): String =
        if (!symbols && shifted && key.length == 1 && key[0].isLetter()) key.uppercase() else key

    private fun onKey(key: String) {
        val ic = currentInputConnection ?: return
        when (key) {
            "⌫" -> ic.deleteSurroundingText(1, 0)
            "space" -> ic.commitText(" ", 1)
            "⇧" -> { shifted = !shifted; renderRows() }
            "?123" -> { symbols = true; renderRows() }
            "ABC" -> { symbols = false; renderRows() }
            "⏎" -> {
                val opts = currentInputEditorInfo?.imeOptions ?: 0
                val action = opts and EditorInfo.IME_MASK_ACTION
                if (action != EditorInfo.IME_ACTION_NONE && (opts and EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) {
                    ic.performEditorAction(action)
                } else {
                    ic.commitText("\n", 1)
                }
            }
            else -> {
                val out = if (!symbols && shifted && key.length == 1) key.uppercase() else key
                ic.commitText(out, 1)
                if (shifted && !symbols) { shifted = false; renderRows() }
            }
        }
    }

    companion object {
        // Matches OverlayTaskbarService.BAR_HEIGHT_DP so the keys land on the taskbar's top.
        private const val TASKBAR_DP = 50
    }
}
