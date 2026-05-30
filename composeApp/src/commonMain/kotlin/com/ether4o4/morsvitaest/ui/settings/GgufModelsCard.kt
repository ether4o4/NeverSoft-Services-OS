package com.ether4o4.morsvitaest.ui.settings

import androidx.compose.runtime.Composable

/**
 * Local GGUF model runtime card (download + run llama.cpp models on-device).
 *
 * Android-only: the real implementation lives in androidMain and drives
 * [com.ether4o4.morsvitaest.sandbox.GgufServerManager]. Every other platform
 * gets an empty actual so the shared settings screen still compiles.
 */
@Composable
expect fun PlatformGgufModelsCard()
