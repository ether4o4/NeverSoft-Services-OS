package com.ether4o4.morsvitaest.ui.overlay

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ether4o4.morsvitaest.ui.DarkColorScheme
import com.ether4o4.morsvitaest.ui.Theme
import com.ether4o4.morsvitaest.ui.chat.ChatScreenContent
import com.ether4o4.morsvitaest.ui.chat.ChatViewModel
import org.koin.compose.KoinContext
import org.koin.compose.viewmodel.koinViewModel

/**
 * The chat shown inside the floating overlay window (over other apps). Reuses the
 * real [ChatScreenContent] bound to a [ChatViewModel] — which shares the same
 * persisted conversation as the in-app chat via the singleton DataRepository — so
 * scripts/answers can be copied straight out and pasted into whatever app is open.
 *
 * [onMinimize] collapses the window back to the taskbar; [onClose] dismisses it.
 */
@Composable
fun OverlayChatContent(onMinimize: () -> Unit, onClose: () -> Unit) {
    KoinContext {
        Theme(colorScheme = DarkColorScheme) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                // Compact window header — title + minimize/close, since the system
                // overlay has no title bar of its own.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(Color(0xFF14171D))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "MVE Assistant",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.weight(1f))
                    HeaderButton("—", onClick = onMinimize)
                    Spacer(Modifier.width(6.dp))
                    HeaderButton("✕", onClick = onClose)
                }

                val viewModel: ChatViewModel = koinViewModel()
                val uiState by viewModel.state.collectAsStateWithLifecycle()
                Box(modifier = Modifier.fillMaxSize()) {
                    ChatScreenContent(
                        uiState = uiState,
                        showSettingsButton = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderButton(glyph: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
