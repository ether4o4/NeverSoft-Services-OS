package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import morsvitaest.composeapp.generated.resources.Res
import morsvitaest.composeapp.generated.resources.ns_mascot
import org.jetbrains.compose.resources.painterResource

/**
 * The clock / notifications / widgets panel, opened from the desktop clock.
 * The MVE assistant lives at the top corner of this panel — tap him to chat.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun NotificationsPanel(
    onClose: () -> Unit,
    onOpenAssistant: () -> Unit,
) {
    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Clock.System.now()
            delay(1_000)
        }
    }
    val local = now.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = local.hour
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    val ampm = if (hour < 12) "AM" else "PM"
    val time = "$h12:${local.minute.toString().padStart(2, '0')}:${local.second.toString().padStart(2, '0')} $ampm"
    val date = "${local.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${local.day}, ${local.year}"

    LauncherAppShell(title = "Notifications", onClose = onClose) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0D11)),
        ) {
            // The MVE assistant drops in with a bounce and sits at the top
            // corner, legs dangling — a slow rock + bob sells the perch.
            val drop = remember { Animatable(-160f) }
            LaunchedEffect(Unit) {
                drop.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                )
            }
            val perch = rememberInfiniteTransition()
            val rock by perch.animateFloat(
                initialValue = -3.5f,
                targetValue = 3.5f,
                animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
            )
            val legBob by perch.animateFloat(
                initialValue = 0f,
                targetValue = 5f,
                animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
            )
            Image(
                painter = painterResource(Res.drawable.ns_mascot),
                contentDescription = "MVE Assistant",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp)
                    .size(92.dp)
                    .offset { IntOffset(0, (drop.value + legBob).toInt() - 14) }
                    .graphicsLayer {
                        rotationZ = rock
                        transformOrigin = TransformOrigin(0.5f, 0.15f)
                    }
                    .clickable { onOpenAssistant() },
            )

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(time, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                Text(date, color = Color.White.copy(alpha = 0.65f), fontSize = 16.sp)
                Spacer(Modifier.height(28.dp))
                Text(
                    "No new notifications",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Widgets coming soon",
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}
