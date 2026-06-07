package com.ether4o4.morsvitaest.ui.foundry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Generic placeholder for tile destinations that don't have a real screen yet.
 * Renders the brushed-metal back button + a centered "coming soon" card so the
 * tap → page → back → home pattern feels correct even before each destination
 * is implemented.
 */
@Composable
fun FoundryPlaceholderScreen(
    title: String,
    description: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Foundry.background)
            .padding(Foundry.pagePadding),
    ) {
        FoundryPill(
            label = "← BACK",
            onClick = onBack,
            intent = FoundryIntent.Neutral,
        )
        Spacer(Modifier.height(Foundry.gridGap))

        FoundryCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(20.dp),
        ) {
            Text(
                text = title,
                color = Foundry.labelPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = description,
                color = Foundry.labelSecondary,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                    .padding(12.dp),
            ) {
                Text(
                    text = "Coming soon — Phase 2 / 3 of the Foundry build.",
                    color = Foundry.labelMuted,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
