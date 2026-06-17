package com.ether4o4.morsvitaest.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ether4o4.morsvitaest.ui.foundry.Foundry
import com.ether4o4.morsvitaest.ui.foundry.FoundryCard
import com.ether4o4.morsvitaest.ui.foundry.FoundryIntent
import com.ether4o4.morsvitaest.ui.foundry.FoundryPill
import com.ether4o4.morsvitaest.ui.handCursor
import morsvitaest.composeapp.generated.resources.Res
import morsvitaest.composeapp.generated.resources.ns_mascot_face
import org.jetbrains.compose.resources.painterResource

private data class TourStep(
    val glyph: String,
    val title: String,
    val body: String,
)

private val tourSteps = listOf(
    TourStep(
        glyph = "🤖",
        title = "Meet your built-in AI",
        body = "MorsVitaEst comes with a free assistant that's already on — no account, no API key. " +
            "Open the Workspace and just start chatting.",
    ),
    TourStep(
        glyph = "?",
        title = "Tap the help bubble anytime",
        body = "The round “?” bubble in the corner opens your assistant. It explains anything, and it " +
            "can set things up for you — adding AI models and connecting tool servers.",
    ),
    TourStep(
        glyph = "▦",
        title = "One workspace, three tabs",
        body = "Chat talks to the AI. Multi chat compares two models side by side. Shell is a safe " +
            "sandbox terminal. The ⚙ gear in the Workspace is where Settings live.",
    ),
    TourStep(
        glyph = "➕",
        title = "Add more when you want",
        body = "Bring your own models in Settings ▸ Services, and plug in tools via Settings ▸ Tools (MCP). " +
            "On the Home screen, each box's gear jumps straight there.",
    ),
)

/**
 * First-run welcome + tour. A full-screen scrim over the app with a stepped card
 * that introduces the built-in AI and points out the harder-to-find areas (the
 * help bubble, the Workspace tabs, and the settings gear). Shown once on first
 * launch; the help panel can replay it on demand.
 */
@Composable
fun WelcomeTour(
    onFinish: () -> Unit,
    onAskAssistant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var index by remember { mutableStateOf(0) }
    val step = tourSteps[index]
    val isLast = index == tourSteps.lastIndex

    Box(
        modifier = modifier
            .fillMaxSize()
            // Scrim — block taps to the app underneath while the tour is up.
            .background(Color(0xCC0A0303))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        FoundryCard(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            contentPadding = PaddingValues(24.dp),
            shape = Foundry.cardShapeLarge,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = "Skip",
                    color = Foundry.labelMuted,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .handCursor()
                        .clickable(onClick = onFinish)
                        .padding(4.dp),
                )
            }

            // The MVE agent greets you for the intro.
            Image(
                painter = painterResource(Res.drawable.ns_mascot_face),
                contentDescription = "MVE",
                modifier = Modifier.size(88.dp).clip(CircleShape),
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(Foundry.tileShape)
                    .background(brush = Foundry.brushedRadial, shape = Foundry.tileShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = step.glyph, fontSize = 30.sp)
            }
            Spacer(Modifier.height(16.dp))

            Text(
                text = step.title,
                color = Foundry.labelPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = step.body,
                color = Foundry.labelSecondary,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(20.dp))

            // Progress dots.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                tourSteps.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (i == index) 9.dp else 7.dp)
                            .clip(Foundry.pillShape)
                            .background(
                                if (i == index) Color(0xFFE53935) else Foundry.labelMuted,
                                Foundry.pillShape,
                            ),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (index > 0) {
                    FoundryPill(
                        label = "BACK",
                        onClick = { index-- },
                        intent = FoundryIntent.Neutral,
                        modifier = Modifier.weight(1f),
                    )
                }
                FoundryPill(
                    label = if (isLast) "GET STARTED" else "NEXT",
                    onClick = { if (isLast) onFinish() else index++ },
                    intent = FoundryIntent.Primary,
                    modifier = Modifier.weight(1f),
                )
            }

            // Bridge into the conversational helper at any point — finishes the tour
            // and opens the assistant so the AI can take it from here.
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Rather just ask? Chat with the assistant →",
                color = Color(0xFFE5484D),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .handCursor()
                    .clickable(onClick = onAskAssistant)
                    .padding(vertical = 4.dp),
            )
        }
    }
}
