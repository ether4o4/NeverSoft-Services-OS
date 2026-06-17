package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import morsvitaest.composeapp.generated.resources.Res
import morsvitaest.composeapp.generated.resources.ns_mascot_alive
import morsvitaest.composeapp.generated.resources.ns_pose_clock
import morsvitaest.composeapp.generated.resources.ns_pose_run
import morsvitaest.composeapp.generated.resources.ns_pose_shrug
import morsvitaest.composeapp.generated.resources.ns_pose_wrench
import org.jetbrains.compose.resources.painterResource
import kotlin.random.Random

private val mascotPoses = listOf(
    Res.drawable.ns_mascot_alive,
    Res.drawable.ns_pose_clock,
    Res.drawable.ns_pose_shrug,
    Res.drawable.ns_pose_run,
    Res.drawable.ns_pose_wrench,
)

/**
 * The NS mascot, alive — hangs from the top of whatever hosts it, swinging and
 * bobbing, and every few seconds flips/switches to a random pose (point, clock,
 * shrug, run, wrench) so he looks like he's goofing around in the box.
 */
@Composable
fun HangingMascot(modifier: Modifier = Modifier, sizeDp: Int = 128, onClick: (() -> Unit)? = null) {
    var poseIdx by remember { mutableIntStateOf(0) }
    var flipped by remember { mutableStateOf(false) }
    // Hoisted so the remember isn't called conditionally inside the modifier chain.
    val interaction = remember { MutableInteractionSource() }

    val motion = rememberInfiniteTransition()
    val swing by motion.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
    )
    val bob by motion.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2600, 4800))
            poseIdx = Random.nextInt(mascotPoses.size)
            flipped = Random.nextBoolean()
        }
    }

    Image(
        painter = painterResource(mascotPoses[poseIdx]),
        contentDescription = "NeverSoft assistant",
        modifier = modifier
            .size(sizeDp.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .offset { IntOffset(0, bob.toInt()) }
            .graphicsLayer {
                // Pivot at the top so he swings like he's hanging on the box.
                transformOrigin = TransformOrigin(0.5f, 0f)
                rotationZ = swing
                scaleX = if (flipped) -1f else 1f
            },
    )
}
