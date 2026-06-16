package com.ether4o4.morsvitaest.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ether4o4.morsvitaest.SystemSetting

private data class GuideStep(
    val tag: String,
    val title: String,
    val body: String,
    val actionLabel: String? = null,
    val onAction: () -> Unit = {},
    // Settings screens open as desktop windows, so we close the tour when we
    // jump to one (it would otherwise sit on top of the window). System screens
    // open as a separate Android activity, so the tour stays put for those.
    val finishOnAction: Boolean = false,
)

/**
 * A guided, one-card-at-a-time tour. It starts with first-run setup/permissions
 * (each button opens the exact Android screen) and then walks through the
 * launcher's appearance settings and the system/AI settings. Re-launchable any
 * time from the Start menu.
 */
@Composable
fun SettingsGuide(
    onOpenAppearance: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onOpenSystemSetting: (SystemSetting) -> Unit,
    onFinish: () -> Unit,
) {
    val steps = remember {
        listOf(
            GuideStep(
                tag = "WELCOME",
                title = "Welcome — let's get set up",
                body = "A quick tour of first-time setup and every settings area. " +
                    "Use Back / Next to move; tap a blue button to jump straight to a screen.",
            ),
            GuideStep(
                tag = "SETUP · 1 of 4",
                title = "Set NeverSoft as your Home launcher",
                body = "Makes this your home screen so it opens with the Home button. " +
                    "Pick NeverSoft and set it as Always.",
                actionLabel = "Open Home settings",
                onAction = { onOpenSystemSetting(SystemSetting.HomeLauncher) },
            ),
            GuideStep(
                tag = "SETUP · 2 of 4",
                title = "Allow restricted settings",
                body = "Sideloaded apps stay limited until you unlock this. On the App-info " +
                    "page tap the ⋮ menu (top-right) → \"Allow restricted settings\".",
                actionLabel = "Open App info",
                onAction = { onOpenSystemSetting(SystemSetting.AppDetails) },
            ),
            GuideStep(
                tag = "SETUP · 3 of 4",
                title = "Allow notifications",
                body = "Lets the assistant and the heartbeat post updates and reminders.",
                actionLabel = "Open notification settings",
                onAction = { onOpenSystemSetting(SystemSetting.AppNotifications) },
            ),
            GuideStep(
                tag = "SETUP · 4 of 4",
                title = "Allow Calendar, SMS & Contacts",
                body = "On the App-info page tap \"Permissions\" and allow Calendar, SMS and " +
                    "Contacts so the assistant can read and act on them.",
                actionLabel = "Open Permissions",
                onAction = { onOpenSystemSetting(SystemSetting.AppDetails) },
            ),
            GuideStep(
                tag = "APPEARANCE",
                title = "Make it yours",
                body = "Appearance settings hold your wallpaper, the Start orb, the theme / " +
                    "glass look, taskbar pins and icon labels. Open it from the Settings tile " +
                    "on the desktop any time.",
                actionLabel = "Open Appearance settings",
                onAction = onOpenAppearance,
                finishOnAction = true,
            ),
            GuideStep(
                tag = "SYSTEM · AI",
                title = "Set up the assistant",
                body = "System settings cover Services (model providers / local GGUF), the " +
                    "Agent's memory, Tools, the Linux Sandbox, and Integrations. Open it from " +
                    "the Models tile.",
                actionLabel = "Open System settings",
                onAction = onOpenSystemSettings,
                finishOnAction = true,
            ),
            GuideStep(
                tag = "ALL SET",
                title = "You're ready",
                body = "That's the whole tour. You can replay it any time from the Start menu — " +
                    "tap the ? button next to the power icon.",
            ),
        )
    }

    var step by remember { mutableIntStateOf(0) }
    val current = steps[step]
    val isLast = step == steps.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            // Swallow taps so they don't fall through to the desktop.
            .clickable(enabled = false) {}
            .systemBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xF20E1726))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
                .padding(22.dp),
        ) {
            Text(current.tag, color = Color(0xFF00D4FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(current.title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(current.body, color = Color.White.copy(alpha = 0.72f), fontSize = 14.sp)

            if (current.actionLabel != null) {
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF3B82F6), Color(0xFF2563EB))))
                        .clickable {
                            current.onAction()
                            if (current.finishOnAction) onFinish()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(current.actionLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Progress dots.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                steps.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (i == step) Color(0xFF00D4FF) else Color.White.copy(alpha = 0.22f),
                            ),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) {
                    Text(
                        "Back",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { step-- }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Skip",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onFinish() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
                Spacer(Modifier.size(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isLast) Color(0xFF1EC64A) else Color.White.copy(alpha = 0.16f))
                        .clickable { if (isLast) onFinish() else step++ }
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                ) {
                    Text(
                        if (isLast) "Done" else "Next",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
