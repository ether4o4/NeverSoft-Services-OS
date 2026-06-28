package com.ether4o4.morsvitaest.ui.launcher

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ether4o4.morsvitaest.data.AppSettings
import org.koin.compose.koinInject

private const val APPWIDGET_HOST_ID = 0x4D5645 // "MVE"

/** One row in the widget picker: the provider plus its pre-resolved widget + app labels. */
private data class WidgetChoice(
    val info: AppWidgetProviderInfo,
    val widgetLabel: String,
    val appLabel: String,
)

/**
 * Android implementation: hosts the user's chosen home-screen app widgets via an
 * [AppWidgetHost] and lets them add a widget from ANY installed app.
 *
 * Adding flow (works whether or not MorsVitaEst is the default launcher):
 *   1. Show a custom picker listing every provider from [AppWidgetManager.getInstalledProviders].
 *   2. Allocate an id and try [AppWidgetManager.bindAppWidgetIdIfAllowed]. When MorsVitaEst
 *      holds the (default-launcher) bind permission this succeeds silently; otherwise it
 *      returns false and we launch the system `ACTION_APPWIDGET_BIND` consent dialog so the
 *      user can grant binding for that one widget.
 *   3. Run the widget's configure activity if it declares one, then host it.
 */
@Composable
actual fun AppWidgetsSection(contentColor: Color, modifier: Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val settings = koinInject<AppSettings>()
    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val host = remember { AppWidgetHost(context, APPWIDGET_HOST_ID) }

    // Receive widget updates only while this page is on screen.
    DisposableEffect(host) {
        runCatching { host.startListening() }
        onDispose { runCatching { host.stopListening() } }
    }

    var widgetIds by remember { mutableStateOf(settings.getHostedWidgetIds()) }
    var pendingConfigureId by remember { mutableStateOf(-1) }
    var pendingBindId by remember { mutableStateOf(-1) }
    var showPicker by remember { mutableStateOf(false) }

    fun add(id: Int) {
        if (id !in widgetIds) {
            val next = widgetIds + id
            widgetIds = next
            settings.setHostedWidgetIds(next)
        }
    }

    fun drop(id: Int) {
        runCatching { host.deleteAppWidgetId(id) }
        val next = widgetIds - id
        widgetIds = next
        settings.setHostedWidgetIds(next)
    }

    // The widget's own configuration screen (when it declares one), run after a successful bind.
    val configureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val id = pendingConfigureId
        pendingConfigureId = -1
        if (id != -1) {
            if (result.resultCode == Activity.RESULT_OK) add(id) else runCatching { host.deleteAppWidgetId(id) }
        }
    }

    // Once an id is bound to a provider, run its configure activity (if any) or host it directly.
    fun configureOrAdd(id: Int) {
        val info = appWidgetManager.getAppWidgetInfo(id)
        val configure = info?.configure
        if (configure != null) {
            pendingConfigureId = id
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            }
            if (runCatching { configureLauncher.launch(intent) }.isFailure) add(id)
        } else {
            add(id)
        }
    }

    // System consent dialog for binding — needed when MorsVitaEst isn't the default launcher.
    val bindLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val id = pendingBindId
        pendingBindId = -1
        if (id != -1) {
            if (result.resultCode == Activity.RESULT_OK) configureOrAdd(id) else runCatching { host.deleteAppWidgetId(id) }
        }
    }

    fun pick(info: AppWidgetProviderInfo) {
        showPicker = false
        val id = host.allocateAppWidgetId()
        val allowed = runCatching { appWidgetManager.bindAppWidgetIdIfAllowed(id, info.provider) }.getOrDefault(false)
        if (allowed) {
            configureOrAdd(id)
        } else {
            pendingBindId = id
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            }
            if (runCatching { bindLauncher.launch(bindIntent) }.isFailure) {
                pendingBindId = -1
                runCatching { host.deleteAppWidgetId(id) }
            }
        }
    }

    Column(modifier = modifier) {
        for (widgetId in widgetIds) {
            key(widgetId) {
                val info = remember(widgetId) { appWidgetManager.getAppWidgetInfo(widgetId) }
                if (info != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(contentColor.copy(alpha = 0.08f))
                            .border(1.dp, contentColor.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                    ) {
                        AndroidView(
                            factory = { ctx -> host.createView(ctx, widgetId, info) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                            update = { view ->
                                val minW = (info.minWidth / density).toInt().coerceAtLeast(40)
                                val minH = (info.minHeight / density).toInt().coerceAtLeast(40)
                                runCatching { view.updateAppWidgetSize(Bundle.EMPTY, minW, minH, minW, minH) }
                            },
                        )
                        Text(
                            text = "✕",
                            color = contentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.40f))
                                .clickable { drop(widgetId) }
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, contentColor.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                .clickable { showPicker = true }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("＋  Add a widget", color = contentColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    if (showPicker) {
        WidgetPickerDialog(
            appWidgetManager = appWidgetManager,
            onPick = { pick(it) },
            onDismiss = { showPicker = false },
        )
    }
}

/** Lists every installed widget provider on the device (label + owning app) so the user can
 *  add a widget from any app — not just the ones a default-launcher picker would surface. */
@Composable
private fun WidgetPickerDialog(
    appWidgetManager: AppWidgetManager,
    onPick: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    // Each row carries the resolved labels so the list doesn't re-resolve them on every recompose.
    val choices = remember {
        runCatching {
            appWidgetManager.installedProviders.map { info ->
                val widgetLabel = runCatching { info.loadLabel(pm) }.getOrNull()?.takeIf { it.isNotBlank() }
                    ?: info.provider.shortClassName.substringAfterLast('.')
                val appLabel = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
                }.getOrNull() ?: info.provider.packageName
                WidgetChoice(info, widgetLabel, appLabel)
            }.sortedWith(compareBy({ it.appLabel.lowercase() }, { it.widgetLabel.lowercase() }))
        }.getOrDefault(emptyList())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Add a widget") },
        text = {
            if (choices.isEmpty()) {
                Text("No widgets are available on this device.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 440.dp)) {
                    items(choices) { choice ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPick(choice.info) }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                        ) {
                            Text(choice.widgetLabel, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(choice.appLabel, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        },
    )
}
