package com.ether4o4.morsvitaest.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ether4o4.morsvitaest.ui.DarkColorScheme
import com.ether4o4.morsvitaest.ui.Theme
import com.ether4o4.morsvitaest.ui.launcher.WidgetsContent
import org.koin.compose.KoinContext

/**
 * The widget / chat panel shown as a floating system overlay (over other apps),
 * toggled by the taskbar clock. Hosts the real [WidgetsContent] — the live agent
 * chat at the top plus the widgets below — so it stays usable over Chrome,
 * Messages, Facebook, etc. and only closes when the user taps the clock again
 * (or the ✕ here).
 */
@Composable
fun OverlayWidgetPanel(onClose: () -> Unit) {
    KoinContext {
        Theme(colorScheme = DarkColorScheme) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.background),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(Color(0xFF14171D))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Assistant & widgets", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✕", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    WidgetsContent()
                }
            }
        }
    }
}
