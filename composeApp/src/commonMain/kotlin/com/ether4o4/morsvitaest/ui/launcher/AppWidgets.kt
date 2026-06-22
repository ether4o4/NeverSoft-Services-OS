package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * The "any app's widgets" board on the launcher Widgets page. On Android this hosts
 * real home-screen AppWidgets from any installed app (via an AppWidgetHost) and shows
 * an "add a widget" control; other platforms render nothing, since app widgets are an
 * Android-only system feature.
 *
 * [contentColor] is the launcher theme's content color so the frames and controls
 * match the rest of the themed Widgets page.
 */
@Composable
expect fun AppWidgetsSection(contentColor: Color, modifier: Modifier = Modifier)
