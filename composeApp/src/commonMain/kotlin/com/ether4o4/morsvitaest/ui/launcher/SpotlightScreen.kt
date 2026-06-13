package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ether4o4.morsvitaest.InstalledApp
import com.ether4o4.morsvitaest.getInstalledApps
import com.ether4o4.morsvitaest.launchApp
import com.ether4o4.morsvitaest.openUrl
import androidx.compose.foundation.Image

/**
 * Spotlight — a glass search surface. Type to filter the apps installed on the
 * device (real labels + icons, tap to launch) and to search the web. Opened
 * from the desktop Search icon.
 */
@Composable
fun SpotlightScreen(onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    LaunchedEffect(Unit) { installedApps = getInstalledApps() }

    val q = query.trim()
    val appMatches = if (q.isBlank()) emptyList()
    else installedApps.filter { it.label.contains(q, ignoreCase = true) }.take(20)

    LauncherAppShell(title = "Spotlight", onClose = onClose) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color(0xCC1A2030), Color(0xCC10141C)),
                    ),
                )
                .padding(16.dp),
        ) {
            // Search field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .padding(start = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(22.dp))
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        Text("Search apps, files, web…", color = Color.White.copy(alpha = 0.45f))
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF6AA9FF),
                    ),
                )
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (q.isNotBlank()) {
                    item { SpotlightSection("WEB") }
                    item {
                        SpotlightRow(
                            leading = { Icon(Icons.Filled.Search, null, tint = Color.White, modifier = Modifier.size(22.dp)) },
                            title = "Search the web for \"$q\"",
                            subtitle = "Opens your browser",
                        ) {
                            onClose()
                            openUrl("https://www.google.com/search?q=" + q.replace(" ", "+"))
                        }
                    }
                }
                if (appMatches.isNotEmpty()) {
                    item { SpotlightSection("APPS") }
                    items(appMatches, key = { it.packageName }) { app ->
                        SpotlightRow(
                            leading = {
                                val ic = app.icon
                                if (ic != null) {
                                    Image(bitmap = ic, contentDescription = app.label, modifier = Modifier.size(30.dp))
                                } else {
                                    Text(app.label.take(1).uppercase(), color = Color.White)
                                }
                            },
                            title = app.label,
                            subtitle = app.packageName,
                        ) {
                            onClose()
                            launchApp(app.packageName)
                        }
                    }
                }
                if (q.isBlank()) {
                    item {
                        Text(
                            "Type to search your apps and the web.",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 30.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotlightSection(text: String) {
    Text(
        text,
        color = Color.White.copy(alpha = 0.45f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp),
    )
}

@Composable
private fun SpotlightRow(
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) { leading() }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = Color.White, fontSize = 15.sp, maxLines = 1)
            Text(subtitle, color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp, maxLines = 1)
        }
    }
}
