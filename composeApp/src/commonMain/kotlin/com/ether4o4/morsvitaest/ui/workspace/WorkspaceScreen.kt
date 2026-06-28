package com.ether4o4.morsvitaest.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ether4o4.morsvitaest.ui.chat.ChatScreen
import com.ether4o4.morsvitaest.ui.chat.ChatViewModel
import com.ether4o4.morsvitaest.ui.compare.CompareScreen
import com.ether4o4.morsvitaest.ui.foundry.Foundry
import com.ether4o4.morsvitaest.ui.sandbox.SandboxTabsContent
import com.ether4o4.morsvitaest.ui.settings.SandboxUiState
import com.ether4o4.morsvitaest.ui.settings.SandboxViewModel
import nl.marc_apps.tts.TextToSpeechInstance
import org.koin.compose.viewmodel.koinViewModel

/**
 * The three modes the Workspace can show. Shell is only offered where the
 * sandbox runtime exists (Android); on other platforms the tab is dropped.
 */
enum class WorkspaceTab(val label: String) {
    Chat("CHAT"),
    MultiChat("MULTI LLM"),
    Shell("SHELL"),
}

/**
 * WorkspaceScreen — the unified "Page 2" box.
 *
 * One surface, one tab strip across the top:
 *
 *   [ CHAT ]  [ MULTI CHAT ]  [ SHELL ]   ⚙
 *
 * Each tab hosts an existing screen in place — the normal chat, the two-pane
 * Compare ("Multi chat"), and the Alpine sandbox terminal ("Shell") — instead of
 * scattering them across separate routes. The ⚙ always opens Settings so the work
 * tab's settings menu is reachable from every mode (this is one of the spots the
 * first-run tour points at).
 */
@Composable
fun WorkspaceScreen(
    chatViewModel: ChatViewModel,
    textToSpeech: TextToSpeechInstance?,
    onNavigateToSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    isSandboxAvailable: Boolean,
    navigationTabBar: (@Composable () -> Unit)? = null,
    initialTab: WorkspaceTab = WorkspaceTab.Chat,
    embedded: Boolean = false,
    onTabSelected: (WorkspaceTab) -> Unit = {},
) {
    val tabs = remember(isSandboxAvailable) {
        if (isSandboxAvailable) {
            listOf(WorkspaceTab.Chat, WorkspaceTab.Shell, WorkspaceTab.MultiChat)
        } else {
            listOf(WorkspaceTab.Chat, WorkspaceTab.MultiChat)
        }
    }
    // Persist as the enum name (String) so it survives config changes / returning
    // from Settings on every platform without needing a Parcelable enum saver.
    var selectedName by rememberSaveable {
        mutableStateOf((initialTab.takeIf { it in tabs } ?: WorkspaceTab.Chat).name)
    }
    val selected = WorkspaceTab.entries.firstOrNull { it.name == selectedName && it in tabs } ?: WorkspaceTab.Chat
    LaunchedEffect(selected) { onTabSelected(selected) }

    // On-device chat-engine persistence toggle: ON keeps the model loaded for instant resume,
    // OFF frees it from memory ~30s after the chat is idle (lighter on weaker devices).
    var chatPersistent by remember { mutableStateOf(chatViewModel.isChatEnginePersistent()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Embedded in the launcher clock pop-up: no opaque page background or
            // status-bar padding — the pop-up supplies the glass frame and insets.
            .then(if (embedded) Modifier else Modifier.background(Foundry.background))
            .then(if (embedded) Modifier else Modifier.statusBarsPadding()),
    ) {
        WorkspaceTabStrip(
            selected = selected,
            tabs = tabs,
            onSelect = { selectedName = it.name },
            onOpenSettings = onNavigateToSettings,
            onOpenHelp = onOpenHelp,
            chatEnginePersistent = chatPersistent,
            onToggleChatEnginePersistent = {
                chatPersistent = it
                chatViewModel.setChatEnginePersistent(it)
            },
            navigationTabBar = navigationTabBar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Foundry.pagePadding)
                .padding(top = Foundry.pagePadding, bottom = Foundry.gridGap),
        )

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (selected) {
                WorkspaceTab.Chat -> ChatScreen(
                    viewModel = chatViewModel,
                    textToSpeech = textToSpeech,
                    onNavigateToSettings = onNavigateToSettings,
                    // Shell is its own tab now, so hide the in-chat sandbox toggle.
                    isSandboxAvailable = false,
                    navigationTabBar = null,
                    // The tab strip already carries the settings gear — avoid a
                    // second one stacked right under it in the chat top bar.
                    showSettingsButton = false,
                )

                WorkspaceTab.MultiChat -> CompareScreen(
                    // Embedded: the tab strip is the navigation, so no back chip.
                    onNavigateBack = null,
                )

                WorkspaceTab.Shell -> ShellTab()
            }
        }
    }
}

@Composable
private fun WorkspaceTabStrip(
    selected: WorkspaceTab,
    tabs: List<WorkspaceTab>,
    onSelect: (WorkspaceTab) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    chatEnginePersistent: Boolean,
    onToggleChatEnginePersistent: (Boolean) -> Unit,
    navigationTabBar: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (navigationTabBar != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = Foundry.gridGap),
                horizontalArrangement = Arrangement.Center,
            ) {
                navigationTabBar()
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Compact, borderless text tabs — no metal pill / red+grey borders.
            tabs.forEach { tab ->
                val isSelected = tab == selected
                Text(
                    text = tab.label,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelect(tab) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            // Chat-engine persistence toggle: ⚡ bright = keep the on-device model loaded for
            // instant resume; dim = free it from memory ~30s after the chat is idle.
            Text(
                text = "⚡",
                color = if (chatEnginePersistent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                },
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onToggleChatEnginePersistent(!chatEnginePersistent) }
                    .padding(horizontal = 7.dp, vertical = 6.dp),
            )
            // Tap-to-help and Settings sit at the end of the strip.
            Text(
                text = "?",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onOpenHelp() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
            Text(
                text = "⚙",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun ShellTab(modifier: Modifier = Modifier) {
    val isPreview = LocalInspectionMode.current
    val sandboxViewModel = if (!isPreview) koinViewModel<SandboxViewModel>() else null
    val liveState = sandboxViewModel?.state?.collectAsStateWithLifecycle()?.value
    val sandboxState = liveState ?: SandboxUiState()
    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SandboxTabsContent(
            sandboxState = sandboxState,
            onSetupSandbox = sandboxViewModel?.let { { it.onSetupSandbox() } } ?: {},
            onCancelSandbox = sandboxViewModel?.let { { it.onCancelSandbox() } } ?: {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
