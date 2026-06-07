@file:OptIn(ExperimentalMaterial3Api::class)

package com.ether4o4.morsvitaest.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ether4o4.morsvitaest.BackIcon
import com.ether4o4.morsvitaest.SandboxController
import com.ether4o4.morsvitaest.Version
import com.ether4o4.morsvitaest.data.EmailAccount
import com.ether4o4.morsvitaest.data.HeartbeatLogEntry
import com.ether4o4.morsvitaest.data.ImportSection
import com.ether4o4.morsvitaest.data.MemoryEntry
import com.ether4o4.morsvitaest.data.ScheduledTask
import com.ether4o4.morsvitaest.data.Service
import com.ether4o4.morsvitaest.data.SharedJson
import com.ether4o4.morsvitaest.data.TaskStatus
import com.ether4o4.morsvitaest.data.TaskTrigger
import com.ether4o4.morsvitaest.data.ThemeMode
import com.ether4o4.morsvitaest.data.detectImportSections
import com.ether4o4.morsvitaest.formatFileSize
import com.ether4o4.morsvitaest.inference.DevicePerformance
import com.ether4o4.morsvitaest.inference.DownloadError
import com.ether4o4.morsvitaest.inference.LocalModel
import com.ether4o4.morsvitaest.inference.calculateDevicePerformance
import com.ether4o4.morsvitaest.inference.estimateGpuMemoryMb
import com.ether4o4.morsvitaest.mcp.PopularMcpServer
import com.ether4o4.morsvitaest.network.tools.ToolInfo
import com.ether4o4.morsvitaest.saveFileToDevice
import com.ether4o4.morsvitaest.ui.MorsVitaEstClearableTextField
import com.ether4o4.morsvitaest.ui.MorsVitaEstOutlinedTextField
import com.ether4o4.morsvitaest.ui.components.MorsVitaEstSlider
import com.ether4o4.morsvitaest.ui.components.SettingsListItem
import com.ether4o4.morsvitaest.ui.components.VerticalScrollbarForScroll
import com.ether4o4.morsvitaest.ui.handCursor
import com.ether4o4.morsvitaest.ui.icons.DragIndicator
import com.ether4o4.morsvitaest.ui.icons.Replay
import com.ether4o4.morsvitaest.ui.icons.Visibility
import com.ether4o4.morsvitaest.ui.icons.VisibilityOff
import com.ether4o4.morsvitaest.ui.morsvitaestAdaptiveCardBorder
import com.ether4o4.morsvitaest.ui.morsvitaestAdaptiveCardColors
import com.ether4o4.morsvitaest.ui.morsvitaestAdaptiveCardSurface
import com.ether4o4.morsvitaest.ui.sandbox.SandboxProgressRow
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.jsonObject
import morsvitaest.composeapp.generated.resources.Res
import morsvitaest.composeapp.generated.resources.default_soul
import morsvitaest.composeapp.generated.resources.github_mark
import morsvitaest.composeapp.generated.resources.ic_arrow_drop_down
import morsvitaest.composeapp.generated.resources.litert_cancel
import morsvitaest.composeapp.generated.resources.litert_context_size
import morsvitaest.composeapp.generated.resources.litert_download
import morsvitaest.composeapp.generated.resources.litert_error_download_incomplete
import morsvitaest.composeapp.generated.resources.litert_error_network
import morsvitaest.composeapp.generated.resources.litert_error_not_enough_disk_space
import morsvitaest.composeapp.generated.resources.litert_free_space
import morsvitaest.composeapp.generated.resources.litert_on_device_description
import morsvitaest.composeapp.generated.resources.litert_performance_good
import morsvitaest.composeapp.generated.resources.litert_performance_ok
import morsvitaest.composeapp.generated.resources.litert_performance_poor
import morsvitaest.composeapp.generated.resources.litert_recommended
import morsvitaest.composeapp.generated.resources.litert_tool_support
import morsvitaest.composeapp.generated.resources.settings_add_service
import morsvitaest.composeapp.generated.resources.settings_ai_mistakes_warning
import morsvitaest.composeapp.generated.resources.settings_api_key_label
import morsvitaest.composeapp.generated.resources.settings_api_key_optional_label
import morsvitaest.composeapp.generated.resources.settings_base_url_label
import morsvitaest.composeapp.generated.resources.settings_daemon_mode
import morsvitaest.composeapp.generated.resources.settings_daemon_mode_description
import morsvitaest.composeapp.generated.resources.settings_documentation
import morsvitaest.composeapp.generated.resources.settings_dynamic_ui
import morsvitaest.composeapp.generated.resources.settings_dynamic_ui_description
import morsvitaest.composeapp.generated.resources.settings_export
import morsvitaest.composeapp.generated.resources.settings_export_import_description
import morsvitaest.composeapp.generated.resources.settings_export_import_title
import morsvitaest.composeapp.generated.resources.settings_export_preview_title
import morsvitaest.composeapp.generated.resources.settings_free_fallback
import morsvitaest.composeapp.generated.resources.settings_heartbeat_recent
import morsvitaest.composeapp.generated.resources.settings_import
import morsvitaest.composeapp.generated.resources.settings_import_error
import morsvitaest.composeapp.generated.resources.settings_import_partial
import morsvitaest.composeapp.generated.resources.settings_import_preview_title
import morsvitaest.composeapp.generated.resources.settings_import_replace_all
import morsvitaest.composeapp.generated.resources.settings_import_replace_all_description
import morsvitaest.composeapp.generated.resources.settings_import_section_conversations
import morsvitaest.composeapp.generated.resources.settings_import_section_email
import morsvitaest.composeapp.generated.resources.settings_import_section_heartbeat
import morsvitaest.composeapp.generated.resources.settings_import_section_mcp
import morsvitaest.composeapp.generated.resources.settings_import_section_memory
import morsvitaest.composeapp.generated.resources.settings_import_section_scheduling
import morsvitaest.composeapp.generated.resources.settings_import_section_services
import morsvitaest.composeapp.generated.resources.settings_import_section_soul
import morsvitaest.composeapp.generated.resources.settings_import_section_tools
import morsvitaest.composeapp.generated.resources.settings_import_success
import morsvitaest.composeapp.generated.resources.settings_mcp_cancel
import morsvitaest.composeapp.generated.resources.settings_memories
import morsvitaest.composeapp.generated.resources.settings_memories_all_title
import morsvitaest.composeapp.generated.resources.settings_memories_delete
import morsvitaest.composeapp.generated.resources.settings_memories_description
import morsvitaest.composeapp.generated.resources.settings_memories_edit_cancel
import morsvitaest.composeapp.generated.resources.settings_memories_edit_save
import morsvitaest.composeapp.generated.resources.settings_memories_edit_title
import morsvitaest.composeapp.generated.resources.settings_memories_show_all
import morsvitaest.composeapp.generated.resources.settings_open_github_issue
import morsvitaest.composeapp.generated.resources.settings_openai_compatible_or_other_service
import morsvitaest.composeapp.generated.resources.settings_openai_compatible_providers
import morsvitaest.composeapp.generated.resources.settings_openai_compatible_setup_ollama
import morsvitaest.composeapp.generated.resources.settings_remove_service
import morsvitaest.composeapp.generated.resources.settings_reorder_content_description
import morsvitaest.composeapp.generated.resources.settings_request_integration_description
import morsvitaest.composeapp.generated.resources.settings_request_integration_title
import morsvitaest.composeapp.generated.resources.settings_sandbox_cancel
import morsvitaest.composeapp.generated.resources.settings_sandbox_description
import morsvitaest.composeapp.generated.resources.settings_sandbox_disk_usage
import morsvitaest.composeapp.generated.resources.settings_sandbox_install
import morsvitaest.composeapp.generated.resources.settings_sandbox_install_packages
import morsvitaest.composeapp.generated.resources.settings_sandbox_subtab_files
import morsvitaest.composeapp.generated.resources.settings_sandbox_subtab_packages
import morsvitaest.composeapp.generated.resources.settings_sandbox_subtab_terminal
import morsvitaest.composeapp.generated.resources.settings_sandbox_uninstall
import morsvitaest.composeapp.generated.resources.settings_sandbox_uninstall_confirm
import morsvitaest.composeapp.generated.resources.settings_scheduled_tasks
import morsvitaest.composeapp.generated.resources.settings_scheduled_tasks_cancel
import morsvitaest.composeapp.generated.resources.settings_scheduled_tasks_description
import morsvitaest.composeapp.generated.resources.settings_sign_in_copy_api_key_from
import morsvitaest.composeapp.generated.resources.settings_sms
import morsvitaest.composeapp.generated.resources.settings_soul
import morsvitaest.composeapp.generated.resources.settings_soul_description
import morsvitaest.composeapp.generated.resources.settings_soul_reset
import morsvitaest.composeapp.generated.resources.settings_soul_reset_cancel
import morsvitaest.composeapp.generated.resources.settings_soul_reset_confirm
import morsvitaest.composeapp.generated.resources.settings_soul_save
import morsvitaest.composeapp.generated.resources.settings_status_checking
import morsvitaest.composeapp.generated.resources.settings_status_connected
import morsvitaest.composeapp.generated.resources.settings_status_error
import morsvitaest.composeapp.generated.resources.settings_status_error_connection_failed
import morsvitaest.composeapp.generated.resources.settings_status_error_invalid_key
import morsvitaest.composeapp.generated.resources.settings_status_error_quota_exhausted
import morsvitaest.composeapp.generated.resources.settings_status_error_rate_limited
import morsvitaest.composeapp.generated.resources.settings_tab_agent
import morsvitaest.composeapp.generated.resources.settings_tab_general
import morsvitaest.composeapp.generated.resources.settings_tab_integrations
import morsvitaest.composeapp.generated.resources.settings_tab_sandbox
import morsvitaest.composeapp.generated.resources.settings_tab_services
import morsvitaest.composeapp.generated.resources.settings_tab_tools
import morsvitaest.composeapp.generated.resources.settings_task_details_consecutive_failures
import morsvitaest.composeapp.generated.resources.settings_task_details_created
import morsvitaest.composeapp.generated.resources.settings_task_details_last_result
import morsvitaest.composeapp.generated.resources.settings_task_details_next_run
import morsvitaest.composeapp.generated.resources.settings_task_details_no_heartbeat_runs
import morsvitaest.composeapp.generated.resources.settings_task_details_no_runs
import morsvitaest.composeapp.generated.resources.settings_task_details_on_every_heartbeat
import morsvitaest.composeapp.generated.resources.settings_task_details_schedule
import morsvitaest.composeapp.generated.resources.settings_task_details_scheduled_for
import morsvitaest.composeapp.generated.resources.settings_task_details_status
import morsvitaest.composeapp.generated.resources.settings_task_details_trigger
import morsvitaest.composeapp.generated.resources.settings_theme
import morsvitaest.composeapp.generated.resources.settings_theme_dark
import morsvitaest.composeapp.generated.resources.settings_theme_description
import morsvitaest.composeapp.generated.resources.settings_theme_light
import morsvitaest.composeapp.generated.resources.settings_theme_oled
import morsvitaest.composeapp.generated.resources.settings_theme_system
import morsvitaest.composeapp.generated.resources.settings_tools_description
import morsvitaest.composeapp.generated.resources.settings_tools_none_available
import morsvitaest.composeapp.generated.resources.settings_ui_scale
import morsvitaest.composeapp.generated.resources.settings_version
import morsvitaest.composeapp.generated.resources.snackbar_email_removed
import morsvitaest.composeapp.generated.resources.snackbar_mcp_server_removed
import morsvitaest.composeapp.generated.resources.snackbar_memory_deleted
import morsvitaest.composeapp.generated.resources.snackbar_service_removed
import morsvitaest.composeapp.generated.resources.snackbar_task_cancelled
import morsvitaest.composeapp.generated.resources.snackbar_undo
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableColumn
import kotlin.math.roundToInt
import kotlin.time.Instant

internal val StatusColorConnected = Color(0xFF4CAF50)
internal val StatusColorChecking = Color(0xFFFF9800)
internal val StatusColorError = Color(0xFFF44336)
internal val StatusColorUnknown = Color(0xFF9E9E9E)

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    sandboxViewModel: SandboxViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    navigationTabBar: (@Composable () -> Unit)? = null,
    initialTab: SettingsTab? = null,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val sandboxState by sandboxViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(initialTab) {
        if (initialTab != null) viewModel.actions.onSelectTab(initialTab)
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.onScreenVisible()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SettingsScreenContent(
        uiState = uiState,
        actions = viewModel.actions,
        sandboxState = sandboxState,
        onToggleSandbox = sandboxViewModel::onToggleSandbox,
        onSetupSandbox = sandboxViewModel::onSetupSandbox,
        onCancelSandbox = sandboxViewModel::onCancelSandbox,
        onResetSandbox = sandboxViewModel::onResetSandbox,
        onInstallPackages = sandboxViewModel::onInstallPackages,
        onNavigateBack = onNavigateBack,
        navigationTabBar = navigationTabBar,
    )
}

@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    actions: SettingsActions = SettingsActions.NoOp,
    sandboxState: SandboxUiState = SandboxUiState(),
    onToggleSandbox: (Boolean) -> Unit = {},
    onSetupSandbox: () -> Unit = {},
    onCancelSandbox: () -> Unit = {},
    onResetSandbox: () -> Unit = {},
    onInstallPackages: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    navigationTabBar: (@Composable () -> Unit)? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val undoLabel = stringResource(Res.string.snackbar_undo)
    val memoryDeletedMsg = stringResource(Res.string.snackbar_memory_deleted)
    val taskCancelledMsg = stringResource(Res.string.snackbar_task_cancelled)
    val emailRemovedMsg = stringResource(Res.string.snackbar_email_removed)
    val serviceRemovedMsg = stringResource(Res.string.snackbar_service_removed)
    val mcpServerRemovedMsg = stringResource(Res.string.snackbar_mcp_server_removed)

    LaunchedEffect(uiState.pendingDeletion) {
        val deletion = uiState.pendingDeletion ?: return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        val message = when (deletion) {
            is PendingDeletion.Memory -> memoryDeletedMsg
            is PendingDeletion.Task -> taskCancelledMsg
            is PendingDeletion.EmailAccount -> emailRemovedMsg
            is PendingDeletion.Service -> serviceRemovedMsg
            is PendingDeletion.McpServer -> mcpServerRemovedMsg
        }
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) {
            actions.onUndoDelete()
        }
    }

    val pendingDeletion = uiState.pendingDeletion
    val filteredMemories = remember(uiState.memories, pendingDeletion) {
        if (pendingDeletion is PendingDeletion.Memory) uiState.memories.filter { it.key != pendingDeletion.key }.toImmutableList() else uiState.memories
    }
    val filteredTasks = remember(uiState.scheduledTasks, pendingDeletion) {
        if (pendingDeletion is PendingDeletion.Task) uiState.scheduledTasks.filter { it.id != pendingDeletion.id }.toImmutableList() else uiState.scheduledTasks
    }
    val filteredEmailAccounts = remember(uiState.emailAccounts, pendingDeletion) {
        if (pendingDeletion is PendingDeletion.EmailAccount) uiState.emailAccounts.filter { it.id != pendingDeletion.id }.toImmutableList() else uiState.emailAccounts
    }
    val filteredServices = remember(uiState.configuredServices, pendingDeletion) {
        if (pendingDeletion is PendingDeletion.Service) uiState.configuredServices.filter { it.instanceId != pendingDeletion.instanceId }.toImmutableList() else uiState.configuredServices
    }
    val filteredMcpServers = remember(uiState.mcpServers, pendingDeletion) {
        if (pendingDeletion is PendingDeletion.McpServer) uiState.mcpServers.filter { it.id != pendingDeletion.serverId }.toImmutableList() else uiState.mcpServers
    }

    val filteredUiState = remember(uiState, filteredMemories, filteredTasks, filteredEmailAccounts, filteredServices, filteredMcpServers) {
        uiState.copy(
            memories = filteredMemories,
            scheduledTasks = filteredTasks,
            emailAccounts = filteredEmailAccounts,
            configuredServices = filteredServices,
            mcpServers = filteredMcpServers,
        )
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding().statusBarsPadding().imePadding()) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = CenterHorizontally) {
            if (navigationTabBar != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = CenterVertically,
                ) {
                    navigationTabBar()
                }
            } else {
                TopBar(onNavigateBack = onNavigateBack)
            }

            val visibleTabs = remember(sandboxState.showSandbox) {
                SettingsTab.entries.filter { it != SettingsTab.Sandbox || sandboxState.showSandbox }.toImmutableList()
            }

            SettingsTabSelector(
                tabs = visibleTabs,
                currentTab = filteredUiState.currentTab,
                onSelectTab = actions.onSelectTab,
            )

            val settingsScrollState = rememberScrollState()
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(settingsScrollState),
                    horizontalAlignment = CenterHorizontally,
                ) {
                    Spacer(Modifier.height(16.dp))

                    val maxContentWidth = when (filteredUiState.currentTab) {
                        SettingsTab.Services -> 500.dp
                        else -> 900.dp
                    }
                    Column(
                        Modifier.widthIn(max = maxContentWidth).fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalAlignment = CenterHorizontally,
                    ) {
                        when (filteredUiState.currentTab) {
                            SettingsTab.General -> {
                                GeneralContent(uiState = filteredUiState, actions = actions)
                            }

                            SettingsTab.Agent -> {
                                AgentContent(uiState = filteredUiState, actions = actions)
                            }

                            SettingsTab.Projects -> {
                                ProjectsContent(uiState = filteredUiState, actions = actions)
                            }

                            SettingsTab.Services -> {
                                ServicesContent(uiState = filteredUiState, actions = actions)
                            }

                            SettingsTab.Integrations -> {
                                IntegrationsContent()
                            }

                            SettingsTab.Tools -> {
                                ToolsContent(
                                    tools = filteredUiState.tools,
                                    onToggleTool = actions.onToggleTool,
                                    mcpServers = filteredUiState.mcpServers,
                                    onAddMcpServer = actions.onAddMcpServer,
                                    onRemoveMcpServer = actions.onRemoveMcpServer,
                                    onToggleMcpServer = actions.onToggleMcpServer,
                                    onRefreshMcpServer = actions.onRefreshMcpServer,
                                    showAddMcpServerDialog = filteredUiState.showAddMcpServerDialog,
                                    onShowAddMcpServerDialog = actions.onShowAddMcpServerDialog,
                                    onAddPopularMcpServer = actions.onAddPopularMcpServer,
                                )
                            }

                            SettingsTab.Sandbox -> {
                                SandboxSettingsCard(
                                    sandboxState = sandboxState,
                                    onToggleSandbox = onToggleSandbox,
                                    onSetupSandbox = onSetupSandbox,
                                    onCancelSandbox = onCancelSandbox,
                                    onResetSandbox = onResetSandbox,
                                    onInstallPackages = onInstallPackages,
                                )
                                Spacer(Modifier.height(16.dp))
                                PlatformGgufModelsCard()
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }

                    Spacer(Modifier.weight(1f))

                    BottomInfo()
                }
                VerticalScrollbarForScroll(
                    scrollState = settingsScrollState,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
        ) { data ->
            Snackbar(snackbarData = data)
        }
    }
}

@Composable
private fun TopBar(onNavigateBack: () -> Unit) {
    Row {
        IconButton(
            modifier = Modifier.handCursor(),
            onClick = onNavigateBack,
        ) {
            Icon(
                imageVector = BackIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun SettingsTabSelector(
    tabs: ImmutableList<SettingsTab>,
    currentTab: SettingsTab,
    onSelectTab: (SettingsTab) -> Unit,
) {
    Surface(
        modifier = Modifier.widthIn(max = 900.dp).fillMaxWidth().padding(vertical = 8.dp),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .padding(4.dp)
                .horizontalScroll(rememberScrollState()),
        ) {
            tabs.forEach { tab ->
                val isSelected = currentTab == tab
                Surface(
                    modifier = Modifier
                        .handCursor()
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelectTab(tab) },
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    } else {
                        Color.Transparent
                    },
                ) {
                    Text(
                        text = when (tab) {
                            SettingsTab.General -> stringResource(Res.string.settings_tab_general)
                            SettingsTab.Agent -> stringResource(Res.string.settings_tab_agent)
                            SettingsTab.Projects -> "Projects"
                            SettingsTab.Services -> stringResource(Res.string.settings_tab_services)
                            SettingsTab.Tools -> stringResource(Res.string.settings_tab_tools)
                            SettingsTab.Sandbox -> stringResource(Res.string.settings_tab_sandbox)
                            SettingsTab.Integrations -> stringResource(Res.string.settings_tab_integrations)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomInfo() {
    Text(
        text = stringResource(Res.string.settings_ai_mistakes_warning),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(Modifier.height(8.dp))

    val uriHandler = LocalUriHandler.current

    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.settings_version, Version.appVersion),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.width(8.dp))

        Icon(
            modifier = Modifier
                .clip(CircleShape)
                .size(24.dp)
                .clickable(onClick = {
                    uriHandler.openUri("https://github.com/ether4o4/MorsVitaEst")
                })
                .handCursor(),
            painter = painterResource(Res.drawable.github_mark),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = stringResource(Res.string.settings_documentation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { uriHandler.openUri("https://github.com/ether4o4/MorsVitaEst/tree/main/docs/") }
                .handCursor(),
        )
    }

    Spacer(Modifier.height(8.dp))
}

@Composable
internal fun SettingsCard(
    modifier: Modifier = Modifier,
    innerPadding: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        colors = morsvitaestAdaptiveCardColors(),
        border = morsvitaestAdaptiveCardBorder(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick).handCursor() else Modifier)
                .then(if (innerPadding) Modifier.padding(16.dp) else Modifier),
        ) {
            content()
        }
    }
}

@Composable
internal fun ToggleableHeadline(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val switchInteractionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = switchInteractionSource,
                indication = null,
            ) { onCheckedChange(!checked) }
            .handCursor(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        actions()
        Switch(
            checked = checked,
            onCheckedChange = null,
            interactionSource = switchInteractionSource,
        )
    }
    Spacer(Modifier.size(4.dp))
    Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
