package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState

/**
 * The set of apps that open as floating NeverSoft OS windows (over the desktop)
 * rather than as nav routes. Each value's [title] is shown in the window title
 * bar and the taskbar button.
 */
enum class DesktopApp(val title: String) {
    Assistant("Assistant"),
    Files("Files"),
    Sandbox("Sandbox"),
    Settings("Settings"),
    Terminal("Terminal"),
    Spotlight("Search"),
    Widgets("Widgets"),
    LauncherSettings("Launcher Settings"),
}

/**
 * Mutable state for one open window. New windows cascade by +28px each and own a
 * z-order used to stack and focus them; minimize hides without destroying state.
 */
class WinState(
    val app: DesktopApp,
    offsetX: Int,
    offsetY: Int,
    z: Int,
) {
    var offsetX by mutableIntStateOf(offsetX)
    var offsetY by mutableIntStateOf(offsetY)
    var minimized by mutableStateOf(false)
    var maximized by mutableStateOf(false)
    var z by mutableIntStateOf(z)
}

/**
 * A draggable, glass-framed floating window with a Vista-style title bar
 * (light-blue gradient, title left, minimize / maximize-restore / close right).
 * Dragging the title bar moves the window unless maximized. Tap anywhere on the
 * frame to bring it to front. The desktop shows behind, so no scrim is drawn.
 *
 * @param areaWidthPx width of the desktop area (above the taskbar), in px.
 * @param areaHeightPx height of the desktop area (above the taskbar), in px.
 */
@Composable
fun WindowFrame(
    win: WinState,
    areaWidthPx: Int,
    areaHeightPx: Int,
    onFocus: () -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    haze: HazeState? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current

    // Default floating size: 86% wide, 70% tall.
    val floatWidthPx = areaWidthPx * 0.86f
    val floatHeightPx = areaHeightPx * 0.70f
    val floatWidthDp = with(density) { floatWidthPx.toDp() }
    val floatHeightDp = with(density) { floatHeightPx.toDp() }

    val cornerRadius = if (win.maximized) 0.dp else 6.dp
    val shape = RoundedCornerShape(cornerRadius)

    val sizeModifier = if (win.maximized) {
        Modifier.fillMaxSize()
    } else {
        Modifier.width(floatWidthDp).height(floatHeightDp)
    }

    val offsetModifier = if (win.maximized) {
        Modifier
    } else {
        Modifier.offset { IntOffset(win.offsetX, win.offsetY) }
    }

    Box(
        modifier = Modifier
            .then(offsetModifier)
            .then(sizeModifier)
            .clip(shape)
            // Frosted-glass frame: blurs the wallpaper behind it + thin white border.
            .neverSoftGlassBlur(haze)
            .border(1.dp, Color.White.copy(alpha = 0.30f), shape)
            .pointerInput(win) { detectTapGestures(onPress = { onFocus() }) },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            WinTitleBar(
                title = win.app.title,
                maximized = win.maximized,
                onDrag = { dx, dy ->
                    onFocus()
                    if (!win.maximized) {
                        win.offsetX = (win.offsetX + dx.toInt())
                            .coerceIn(-(floatWidthPx * 0.5f).toInt(), (areaWidthPx - floatWidthPx * 0.5f).toInt())
                        win.offsetY = (win.offsetY + dy.toInt())
                            .coerceIn(0, (areaHeightPx - 40).coerceAtLeast(0))
                    }
                },
                onMinimize = onMinimize,
                onMaximizeToggle = {
                    onFocus()
                    win.maximized = !win.maximized
                },
                onClose = onClose,
            )
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

/**
 * The Vista Aero title bar: a light-blue gradient bar with the window title on
 * the left and minimize / maximize-restore / close controls on the right. The
 * whole bar is a drag handle (except the buttons) that moves the window.
 */
@Composable
private fun WinTitleBar(
    title: String,
    maximized: Boolean,
    onDrag: (Float, Float) -> Unit,
    onMinimize: () -> Unit,
    onMaximizeToggle: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(
                Brush.verticalGradient(listOf(Color(0xFFF2F7FF), Color(0xFFBFCCE6))),
            )
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    onDrag(drag.x, drag.y)
                }
            }
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            color = Color(0xFF1A2A45).copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        WinControl(glyph = "—", onClick = onMinimize)
        Spacer(Modifier.width(2.dp))
        WinControl(glyph = if (maximized) "❐" else "▢", onClick = onMaximizeToggle)
        Spacer(Modifier.width(2.dp))
        WinControl(glyph = "✕", isClose = true, onClick = onClose)
    }
}

/** A single 30x26dp title-bar control; the close button glows red on press. */
@Composable
private fun WinControl(
    glyph: String,
    isClose: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val bg = when {
        isClose && pressed -> Color(0xFFE81123)
        pressed -> Color(0xFF8FA8D6).copy(alpha = 0.6f)
        else -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .size(width = 30.dp, height = 26.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            glyph,
            color = if (isClose && pressed) Color.White else Color(0xFF1A2A45).copy(alpha = 0.75f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
