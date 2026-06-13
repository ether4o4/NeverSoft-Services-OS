package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ether4o4.morsvitaest.ui.sandbox.SandboxTabsContent
import com.ether4o4.morsvitaest.ui.settings.SandboxUiState
import com.ether4o4.morsvitaest.ui.settings.SandboxViewModel
import kotlin.random.Random
import kotlinx.coroutines.delay
import morsvitaest.composeapp.generated.resources.Res
import morsvitaest.composeapp.generated.resources.ns_mascot
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

private val HudCyan = Color(0xFF22E0FF)
private val MatrixGlyphs = "0123456789ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾊﾋﾌﾍﾎABCDEF#$%&".toList()

/**
 * The Shell as a glass command center: a translucent main console (the *real*
 * Alpine sandbox terminal) framed in glowing cyan, flanked by side rails
 * streaming Matrix rain — the look stitched together from the user's
 * holo-tablet / sci-fi console references. Opened from the Computer desktop
 * icon and the Terminal dock tile.
 */
@Composable
fun HudShellScreen(onClose: () -> Unit) {
    val vm = koinViewModel<SandboxViewModel>()
    val state: SandboxUiState = vm.state.collectAsStateWithLifecycle().value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF04090F), Color(0xFF020508), Color(0xFF000000)),
                ),
            )
            .systemBarsPadding()
            .padding(8.dp),
    ) {
        // Faint techno grid behind everything.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 28.dp.toPx()
            val line = HudCyan.copy(alpha = 0.05f)
            var x = 0f
            while (x < size.width) {
                drawLine(line, Offset(x, 0f), Offset(x, size.height), 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(line, Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // HUD header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, HudCyan.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                    .background(HudCyan.copy(alpha = 0.07f))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("◢", color = HudCyan, fontSize = 13.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "NEVERSOFT // COMMAND",
                    color = HudCyan,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "C:\\>",
                    color = HudCyan.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, HudCyan.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", color = HudCyan, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.size(8.dp))

            // Command-center body: matrix rail | glass console | matrix rail
            Row(modifier = Modifier.fillMaxSize()) {
                MatrixRail(
                    modifier = Modifier
                        .width(34.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, HudCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .background(Color(0xCC020608)),
                    seed = 1,
                )
                Spacer(Modifier.width(6.dp))

                // Main usable screen — translucent "clear glass" console hosting
                // the live terminal.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, HudCyan.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xB80A141E),
                                    Color(0xC2060D14),
                                    Color(0xCC04090F),
                                ),
                            ),
                        ),
                ) {
                    // Glass glare sweep across the top of the pane.
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawLine(
                            Color.White.copy(alpha = 0.10f),
                            Offset(size.width * 0.08f, 0f),
                            Offset(size.width * 0.55f, size.height * 0.30f),
                            strokeWidth = 60f,
                        )
                    }
                    SandboxTabsContent(
                        sandboxState = state,
                        onSetupSandbox = { vm.onSetupSandbox() },
                        onCancelSandbox = { vm.onCancelSandbox() },
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                    )
                }

                Spacer(Modifier.width(6.dp))
                MatrixRail(
                    modifier = Modifier
                        .width(34.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, HudCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .background(Color(0xCC020608)),
                    seed = 7,
                )
            }
        }

        // The NS guy hangs out on the glass — runs across the top, leans on the
        // code wall, and glitch-teleports while the shell is open.
        ActiveMascot(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .align(Alignment.TopStart)
                .padding(top = 46.dp, start = 44.dp, end = 44.dp),
        )

        // Sci-fi corner brackets on the outer frame.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val len = 22.dp.toPx()
            val inset = 2.dp.toPx()
            val w = 2.5f
            val c = HudCyan
            drawLine(c, Offset(inset, inset), Offset(inset + len, inset), w)
            drawLine(c, Offset(inset, inset), Offset(inset, inset + len), w)
            drawLine(c, Offset(size.width - inset, inset), Offset(size.width - inset - len, inset), w)
            drawLine(c, Offset(size.width - inset, inset), Offset(size.width - inset, inset + len), w)
            drawLine(c, Offset(inset, size.height - inset), Offset(inset + len, size.height - inset), w)
            drawLine(c, Offset(inset, size.height - inset), Offset(inset, size.height - inset - len), w)
            drawLine(c, Offset(size.width - inset, size.height - inset), Offset(size.width - inset - len, size.height - inset), w)
            drawLine(c, Offset(size.width - inset, size.height - inset), Offset(size.width - inset, size.height - inset - len), w)
        }
    }
}

/**
 * The NS mascot living on the shell: runs along the top of the glass, leans
 * against the rail, then glitch-teleports (flicker + ghost) to a new spot.
 */
@Composable
private fun ActiveMascot(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val mascotPx = with(density) { 64.dp.toPx() }
        val maxX = (widthPx - mascotPx).coerceAtLeast(1f)
        val x = remember { Animatable(0f) }
        var flipped by remember { mutableStateOf(false) }
        var leaning by remember { mutableStateOf(false) }
        var running by remember { mutableStateOf(true) }
        var visible by remember { mutableStateOf(true) }
        var ghost by remember { mutableStateOf(false) }

        // Fast little bounce that sells the "running" cycle.
        val runCycle = rememberInfiniteTransition()
        val bob by runCycle.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(240, easing = LinearEasing), RepeatMode.Reverse),
        )

        LaunchedEffect(maxX) {
            while (true) {
                // Sprint to the right rail.
                running = true; leaning = false; flipped = false
                x.animateTo(maxX, tween((maxX * 8).toInt().coerceAtLeast(1500), easing = LinearEasing))
                // Lean against the clear wall of code.
                running = false; leaning = true
                delay(2800)
                // Glitch across the screen — flicker, ghost, teleport.
                leaning = false; ghost = true
                repeat(3) {
                    visible = false
                    delay(70)
                    x.snapTo(Random.nextFloat() * maxX)
                    visible = true
                    delay(110)
                }
                ghost = false
                // Sprint back to the left rail.
                running = true; flipped = true
                x.animateTo(0f, tween((maxX * 8).toInt().coerceAtLeast(1500), easing = LinearEasing))
                running = false; leaning = true
                delay(2200)
                leaning = false; ghost = true
                repeat(2) {
                    visible = false
                    delay(70)
                    x.snapTo(Random.nextFloat() * maxX)
                    visible = true
                    delay(110)
                }
                ghost = false
                x.snapTo(0f)
                flipped = false
            }
        }

        val yBob = if (running) bob * 6f else 0f
        val tilt = when {
            leaning -> if (flipped) -14f else 14f
            running -> (bob - 0.5f) * 8f
            else -> 0f
        }

        if (ghost) {
            Image(
                painter = painterResource(Res.drawable.ns_mascot),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .offset { IntOffset((x.value + 7f).toInt(), (6 + yBob).toInt()) }
                    .graphicsLayer {
                        alpha = 0.25f
                        scaleX = if (flipped) -1f else 1f
                    },
            )
        }
        if (visible) {
            Image(
                painter = painterResource(Res.drawable.ns_mascot),
                contentDescription = "NS",
                modifier = Modifier
                    .size(64.dp)
                    .offset { IntOffset(x.value.toInt(), (6 + yBob).toInt()) }
                    .graphicsLayer {
                        scaleX = if (flipped) -1f else 1f
                        rotationZ = tilt
                    },
            )
        }
    }
}

/**
 * A narrow vertical rail of Matrix rain: per-column falling glyph streams with
 * a bright head and a fading tail, shimmering as they fall.
 */
@Composable
private fun MatrixRail(modifier: Modifier, seed: Int, columns: Int = 2) {
    val textMeasurer = rememberTextMeasurer()
    var frameNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameNanos = it }
        }
    }
    // Deterministic per-column speeds/phases from the seed.
    val speeds = remember(seed) { List(columns) { 90f + ((seed * 37 + it * 53) % 90) } }
    val phases = remember(seed) { List(columns) { ((seed * 101 + it * 211) % 1000) * 7f } }
    val glyphStyle = remember {
        TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }

    Canvas(modifier = modifier) {
        val t = frameNanos / 1_000_000_000f
        val cellH = 14.dp.toPx()
        val colW = size.width / columns
        val tail = 9
        for (c in 0 until columns) {
            val travel = size.height + tail * cellH
            val headY = ((t * speeds[c] + phases[c]) % travel) - tail * cellH / 2f
            for (i in 0 until tail) {
                val y = headY - i * cellH
                if (y < -cellH || y > size.height) continue
                // Shimmering glyph choice, deterministic per cell+tick.
                val tick = (t * 9).toInt()
                val g = MatrixGlyphs[((y / cellH).toInt() * 31 + c * 17 + seed * 13 + tick * 7).mod(MatrixGlyphs.size)]
                val alpha = if (i == 0) 0.95f else (0.75f - i * 0.08f).coerceAtLeast(0.08f)
                val color = if (i == 0) Color(0xFFCFFFF6) else HudCyan.copy(alpha = alpha)
                drawText(
                    textMeasurer = textMeasurer,
                    text = g.toString(),
                    topLeft = Offset(c * colW + colW * 0.22f, y),
                    style = glyphStyle.copy(color = color),
                )
            }
        }
    }
}
