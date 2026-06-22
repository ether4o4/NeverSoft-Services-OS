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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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

/**
 * Android implementation: hosts the user's chosen home-screen app widgets via an
 * [AppWidgetHost] and lets them pick more through the system widget picker.
 *
 * The picker (ACTION_APPWIDGET_PICK) is the launcher-app flow: the system binds the
 * chosen widget to the id we allocate, then we run its configure activity if it has
 * one. Works best when MorsVitaEst is the device's default launcher.
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

    // Step 2 — the widget's own configuration screen (when it declares one).
    val configureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val id = pendingConfigureId
        pendingConfigureId = -1
        if (id != -1) {
            if (result.resultCode == Activity.RESULT_OK) add(id) else runCatching { host.deleteAppWidgetId(id) }
        }
    }

    // Step 1 — the system widget picker. On OK the system has bound the widget to the
    // allocated id; configure it if it asks, otherwise add it straight away.
    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (result.resultCode == Activity.RESULT_OK && id != -1) {
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
        } else if (id != -1) {
            runCatching { host.deleteAppWidgetId(id) }
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
                .clickable {
                    val newId = host.allocateAppWidgetId()
                    val pick = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newId)
                        // Empty custom lists keep some OEM pickers from NPE-ing.
                        putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, ArrayList<AppWidgetProviderInfo>())
                        putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, ArrayList<Bundle>())
                    }
                    if (runCatching { pickLauncher.launch(pick) }.isFailure) {
                        runCatching { host.deleteAppWidgetId(newId) }
                    }
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("＋  Add a widget", color = contentColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
