package com.ether4o4.morsvitaest.ui.overlay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ether4o4.morsvitaest.InstalledApp
import com.ether4o4.morsvitaest.getInstalledApps
import com.ether4o4.morsvitaest.launchApp
import com.ether4o4.morsvitaest.ui.DarkColorScheme
import com.ether4o4.morsvitaest.ui.Theme
import org.koin.compose.KoinContext

/**
 * The Start menu shown as a floating system overlay (over other apps), toggled by
 * the taskbar's Start orb. Because it's an overlay it draws ON TOP of freeform app
 * windows — unlike the in-app Start menu, which lives in the launcher's home window
 * and is therefore hidden *behind* floating app windows.
 *
 * Deliberately a focused, dialog-free launcher (search + installed-app grid): a
 * Compose [androidx.compose.material3.AlertDialog] crashes when shown from a Service
 * overlay (no Activity window token), so the full in-app drawer (which uses pop-up
 * dialogs for pinning) can't be hosted here. MVE's own apps/settings live behind the
 * "Open MVE" button, which brings the launcher forward.
 *
 * @param onClose hide this overlay.
 * @param onOpenMve bring the MorsVitaEst launcher to the front (for its own apps/settings).
 */
@Composable
fun OverlayStartMenu(onClose: () -> Unit, onOpenMve: () -> Unit) {
    KoinContext {
        Theme(colorScheme = DarkColorScheme) {
            var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
            LaunchedEffect(Unit) { apps = getInstalledApps() }
            var query by remember { mutableStateOf("") }
            val filtered = remember(apps, query) {
                val q = query.trim()
                if (q.isBlank()) apps else apps.filter { it.label.contains(q, ignoreCase = true) }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding()
                    .navigationBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(Color(0xFF14171D))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Start", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onOpenMve) { Text("Open MVE") }
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✕", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    placeholder = { Text("Search apps") },
                    singleLine = true,
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        AppTile(app) {
                            launchApp(app.packageName)
                            onClose()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppTile(app: InstalledApp, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable { onClick() }.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            val icon = app.icon
            if (icon != null) {
                Image(bitmap = icon, contentDescription = app.label, modifier = Modifier.size(40.dp))
            } else {
                Text(
                    app.label.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            app.label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
