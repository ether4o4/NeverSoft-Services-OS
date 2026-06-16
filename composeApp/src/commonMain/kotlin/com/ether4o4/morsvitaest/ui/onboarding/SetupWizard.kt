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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ether4o4.morsvitaest.SystemSetting
import com.ether4o4.morsvitaest.openSystemSetting

private data class SetupStep(
    val key: String,
    val title: String,
    val why: String,
    val action: String,
    val onOpen: () -> Unit,
)

/**
 * First-run setup wizard. Instead of telling users to dig through Android
 * settings, each step OPENS the exact system screen (home-launcher chooser,
 * this app's App-info page for "Allow restricted settings" + permissions, and
 * the notification settings).
 */
@Composable
fun SetupWizard(onFinish: () -> Unit) {
    val done = remember { mutableStateMapOf<String, Boolean>() }

    val steps = listOf(
        SetupStep(
            key = "home",
            title = "Set as Home launcher",
            why = "Makes NeverSoft OS your home screen so it opens with the Home button.",
            action = "Open Home settings",
            onOpen = { openSystemSetting(SystemSetting.HomeLauncher) },
        ),
        SetupStep(
            key = "restricted",
            title = "Allow restricted settings",
            why = "Sideloaded apps are limited until you unlock this. On the App-info page tap the ⋮ menu (top-right) → \"Allow restricted settings\".",
            action = "Open App info",
            onOpen = { openSystemSetting(SystemSetting.AppDetails) },
        ),
        SetupStep(
            key = "notifications",
            title = "Allow notifications",
            why = "Lets the assistant and heartbeat post updates.",
            action = "Open notification settings",
            onOpen = { openSystemSetting(SystemSetting.AppNotifications) },
        ),
        SetupStep(
            key = "permissions",
            title = "Allow permissions (Calendar, SMS, etc.)",
            why = "On the App-info page tap \"Permissions\" and allow Calendar, SMS, Contacts and anything else you want the assistant to use.",
            action = "Open Permissions",
            onOpen = { openSystemSetting(SystemSetting.AppDetails) },
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2070B12))
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text("Welcome to NeverSoft OS", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "A quick setup. Each button opens the exact Android screen — flip the switches, then come back here (Back gesture) and continue.",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(18.dp))

            steps.forEach { step ->
                val isDone = done[step.key] == true
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isDone) Color(0xFF1EC64A) else Color.White.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isDone) Text("✓", color = Color.White, fontSize = 13.sp)
                        }
                        Spacer(Modifier.size(10.dp))
                        Text(step.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(step.why, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(listOf(Color(0xFF3B82F6), Color(0xFF2563EB))),
                            )
                            .clickable {
                                step.onOpen()
                                done[step.key] = true
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(step.action, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    "Skip",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onFinish() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
                Spacer(Modifier.size(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1EC64A))
                        .clickable { onFinish() }
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                ) {
                    Text("Done", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
