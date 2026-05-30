package com.ether4o4.morsvitaest.ui.settings

import androidx.compose.runtime.Composable

// GGUF runtime is Android-only (needs the Linux sandbox). No-op on desktop.
@Composable
actual fun PlatformGgufModelsCard() {}
