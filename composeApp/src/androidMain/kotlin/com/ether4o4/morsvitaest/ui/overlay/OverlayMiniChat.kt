package com.ether4o4.morsvitaest.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ether4o4.morsvitaest.ui.chat.ChatViewModel
import com.ether4o4.morsvitaest.ui.chat.History
import org.koin.compose.viewmodel.koinViewModel

/**
 * A minimal chat for the floating overlay panel: message list + input + send,
 * bound to the shared [ChatViewModel] (same conversation as everywhere). Crucially
 * it does NOT use the full ChatScreenContent — that pulls in a file-picker
 * (`rememberFilePickerLauncher`) which registers an activity-result launcher and
 * crashes when hosted in a Service overlay (no Activity). This stays activity-free.
 */
@Composable
fun OverlayMiniChat(modifier: Modifier = Modifier) {
    val viewModel: ChatViewModel = koinViewModel()
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val messages = remember(uiState.history) {
        uiState.history.filter {
            (it.role == History.Role.USER || it.role == History.Role.ASSISTANT) &&
                it.content.isNotBlank() && !it.isThinking && !it.isStatusMessage
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    var text by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages, key = { it.id }) { msg -> MiniBubble(msg) }
        }
        if (uiState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message the assistant…") },
                maxLines = 4,
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val t = text.trim()
                    if (t.isNotEmpty()) {
                        uiState.actions.ask(t)
                        text = ""
                    }
                },
                enabled = !uiState.isLoading,
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
private fun MiniBubble(msg: History) {
    val isUser = msg.role == History.Role.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isUser) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = msg.content,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}
