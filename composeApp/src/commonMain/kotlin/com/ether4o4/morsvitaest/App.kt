@file:OptIn(ExperimentalMaterial3Api::class)

package com.ether4o4.morsvitaest

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.svg.SvgDecoder
import com.ether4o4.morsvitaest.data.AppSettings
import com.ether4o4.morsvitaest.data.ThemeMode
import com.ether4o4.morsvitaest.tools.CalendarPermissionController
import com.ether4o4.morsvitaest.tools.NotificationPermissionController
import com.ether4o4.morsvitaest.tools.SetupCalendarPermissionHandler
import com.ether4o4.morsvitaest.tools.SetupNotificationPermissionHandler
import com.ether4o4.morsvitaest.tools.SetupSmsPermissionHandler
import com.ether4o4.morsvitaest.tools.SetupSmsSendPermissionHandler
import com.ether4o4.morsvitaest.tools.SmsPermissionController
import com.ether4o4.morsvitaest.tools.SmsSendPermissionController
import com.ether4o4.morsvitaest.ui.DarkColorScheme
import com.ether4o4.morsvitaest.ui.LightColorScheme
import com.ether4o4.morsvitaest.ui.Theme
import com.ether4o4.morsvitaest.ui.chat.ChatViewModel
import com.ether4o4.morsvitaest.ui.components.FullScreenImageHost
import com.ether4o4.morsvitaest.ui.foundry.FoundryDestination
import com.ether4o4.morsvitaest.ui.foundry.FoundryHome
import com.ether4o4.morsvitaest.ui.foundry.FoundryHomeViewModel
import com.ether4o4.morsvitaest.ui.foundry.FoundryPlaceholderScreen
import com.ether4o4.morsvitaest.ui.handCursor
import com.ether4o4.morsvitaest.ui.help.HelpAssistantSheet
import com.ether4o4.morsvitaest.ui.help.HelpBubble
import com.ether4o4.morsvitaest.ui.launcher.HudShellScreen
import com.ether4o4.morsvitaest.ui.launcher.LauncherAppShell
import com.ether4o4.morsvitaest.ui.launcher.LauncherScreen
import com.ether4o4.morsvitaest.ui.launcher.LauncherSettingsScreen
import com.ether4o4.morsvitaest.ui.onboarding.WelcomeTour
import com.ether4o4.morsvitaest.ui.sandbox.SandboxFilesContent
import com.ether4o4.morsvitaest.ui.sandbox.SandboxPackagesContent
import com.ether4o4.morsvitaest.ui.settings.SettingsScreen
import com.ether4o4.morsvitaest.ui.settings.SettingsTab
import com.ether4o4.morsvitaest.ui.withBlackBackground
import com.ether4o4.morsvitaest.ui.workspace.WorkspaceScreen
import com.ether4o4.morsvitaest.ui.workspace.WorkspaceTab
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import morsvitaest.composeapp.generated.resources.Res
import morsvitaest.composeapp.generated.resources.tab_home
import morsvitaest.composeapp.generated.resources.tab_workspace
import nl.marc_apps.tts.TextToSpeechInstance
import nl.marc_apps.tts.experimental.ExperimentalVoiceApi
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration

// NeverSoft OS desktop — the macOS-style shell (menu bar + Dock + Launchpad)
// that boots the app. Its tiles open the real engine routes below.
@Serializable
@SerialName("launcher")
object Launcher

// Optional [path] deep-links the sandbox file browser to a directory (used by
// desktop icons linked to a file path).
@Serializable
@SerialName("files")
data class Files(val path: String? = null)

@Serializable
@SerialName("packages")
object Packages

// Launcher Settings — the NeverSoft OS shell's own settings (wallpaper, desktop
// icons). Distinct from the MVE engine's AI settings, which live under Settings.
@Serializable
@SerialName("launcher.settings")
object LauncherSettings

// The sci-fi HUD shell — the Alpine terminal in a glowing console frame.
@Serializable
@SerialName("hud.shell")
object Shell

@Serializable
@SerialName("home")
object Home

@Serializable
@SerialName("settings")
data class Settings(val tab: String? = null)

// Foundry home tile destinations. The workspace hosts chat / multi-chat /
// shell behind one tab strip; [tab] optionally deep-links to a specific mode.
@Serializable
@SerialName("foundry.chat")
data class FoundryChat(val tab: String? = null)

@Serializable
@SerialName("foundry.stub")
data class FoundryStub(val title: String, val description: String)

@Composable
fun App(
    navController: NavHostController,
    lightColorScheme: ColorScheme = LightColorScheme,
    darkColorScheme: ColorScheme = DarkColorScheme,
    textToSpeech: TextToSpeechInstance? = null,
    isKoinStarted: Boolean = false,
    onAppOpens: ((Int) -> Unit)? = null,
) {
    setSingletonImageLoaderFactory { context: PlatformContext ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
                add(SvgDecoder.Factory())
            }
            .build()
    }

    // Reuse global Koin if already started (Android Application class),
    // otherwise create a new instance (iOS, Desktop, Wasm).
    if (isKoinStarted) {
        AppContent(navController, lightColorScheme, darkColorScheme, textToSpeech, onAppOpens)
    } else {
        KoinApplication(
            configuration = koinConfiguration {
                modules(appModule)
            },
        ) {
            AppContent(navController, lightColorScheme, darkColorScheme, textToSpeech, onAppOpens)
        }
    }
}

@Composable
private fun AppContent(
    navController: NavHostController,
    lightColorScheme: ColorScheme,
    darkColorScheme: ColorScheme,
    textToSpeech: TextToSpeechInstance?,
    onAppOpens: ((Int) -> Unit)?,
) {
    val appSettings = koinInject<AppSettings>()

    // Track app opens after Koin is initialized
    onAppOpens?.let { callback ->
        LaunchedEffect(Unit) {
            callback(appSettings.trackAppOpen())
        }
    }

    // First-run welcome tour + the always-available help bubble/sheet.
    var showTour by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!appSettings.hasSeenWelcome()) showTour = true
    }

    // Set up permission handlers
    val calendarPermissionController = koinInject<CalendarPermissionController>()
    SetupCalendarPermissionHandler(calendarPermissionController)

    val notificationPermissionController = koinInject<NotificationPermissionController>()
    SetupNotificationPermissionHandler(notificationPermissionController)

    val smsPermissionController = koinInject<SmsPermissionController>()
    SetupSmsPermissionHandler(smsPermissionController)

    val smsSendPermissionController = koinInject<SmsSendPermissionController>()
    SetupSmsSendPermissionHandler(smsSendPermissionController)

    // Set TTS voice to match system language
    @OptIn(ExperimentalVoiceApi::class)
    LaunchedEffect(textToSpeech) {
        val tts = textToSpeech ?: return@LaunchedEffect
        val systemLanguage = Locale.current.language
        val matchingVoice = tts.voices
            .firstOrNull { it.languageTag.startsWith(systemLanguage) }
        if (matchingVoice != null) {
            tts.currentVoice = matchingVoice
        }
    }

    val uiScale by appSettings.uiScaleFlow.collectAsStateWithLifecycle()
    val defaultDensity = LocalDensity.current
    val scaledDensity = remember(defaultDensity, uiScale) {
        Density(defaultDensity.density * uiScale, defaultDensity.fontScale)
    }

    val themeMode by appSettings.themeModeFlow.collectAsStateWithLifecycle()
    val systemInDark = isSystemInDarkTheme()
    val effectiveColorScheme = when (themeMode) {
        ThemeMode.System -> if (systemInDark) darkColorScheme else lightColorScheme
        ThemeMode.Light -> lightColorScheme
        ThemeMode.Dark -> darkColorScheme
        ThemeMode.OledBlack -> darkColorScheme.withBlackBackground()
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        Theme(colorScheme = effectiveColorScheme) {
            FullScreenImageHost {
                Box(Modifier.fillMaxSize()) {
                    val chatViewModel: ChatViewModel = koinViewModel()
                    val showTabBar = currentPlatform !is Platform.Mobile
                    val currentBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = currentBackStackEntry?.destination?.route
                    val isHome = currentRoute == "home"

                    val navigationTabBar: @Composable () -> Unit = {
                        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                        val count = 2
                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = isHome,
                                onClick = {
                                    navController.navigate(Home) {
                                        popUpTo(Home) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = if (isRtl) count - 1 else 0, count = count),
                                modifier = Modifier.handCursor(),
                            ) {
                                Text(stringResource(Res.string.tab_home))
                            }
                            SegmentedButton(
                                selected = !isHome,
                                onClick = {
                                    navController.navigate(FoundryChat()) {
                                        popUpTo(Home)
                                        launchSingleTop = true
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = if (isRtl) 0 else count - 1, count = count),
                                modifier = Modifier.handCursor(),
                            ) {
                                Text(stringResource(Res.string.tab_workspace))
                            }
                        }
                    }

                    NavHost(
                        navController,
                        startDestination = Launcher,
                        modifier = Modifier.background(MaterialTheme.colorScheme.background),
                    ) {
                        composable<Launcher> {
                            // NeverSoft OS desktop. Each dock tile opens a real
                            // Mors Vita Est engine screen on the local-GGUF / sandbox stack.
                            // The left page is the live Foundry news feed.
                            val homeViewModel = koinViewModel<FoundryHomeViewModel>()
                            val homeState by homeViewModel.state.collectAsStateWithLifecycle()
                            LauncherScreen(
                                onOpenChat = {
                                    navController.navigate(FoundryChat(WorkspaceTab.Chat.name))
                                },
                                onOpenShell = { navController.navigate(Shell) },
                                onOpenFiles = { navController.navigate(Files()) },
                                onOpenFilesAt = { path -> navController.navigate(Files(path)) },
                                onOpenSandbox = { navController.navigate(Packages) },
                                onOpenModels = {
                                    navController.navigate(Settings(SettingsTab.Services.name))
                                },
                                onOpenLauncherSettings = {
                                    navController.navigate(LauncherSettings)
                                },
                                onOpenStub = { title, desc ->
                                    navController.navigate(FoundryStub(title, desc))
                                },
                                newsPage = {
                                    FoundryHome(
                                        onNavigate = { dest ->
                                            when (dest) {
                                                FoundryDestination.Chat ->
                                                    navController.navigate(FoundryChat(WorkspaceTab.Chat.name))

                                                FoundryDestination.Shell ->
                                                    navController.navigate(FoundryChat(WorkspaceTab.Shell.name))

                                                FoundryDestination.Compare ->
                                                    navController.navigate(FoundryChat(WorkspaceTab.MultiChat.name))

                                                FoundryDestination.Mcp ->
                                                    navController.navigate(Settings(SettingsTab.Tools.name))

                                                FoundryDestination.Projects ->
                                                    navController.navigate(Settings(SettingsTab.Projects.name))

                                                FoundryDestination.Services,
                                                FoundryDestination.Ollama,
                                                FoundryDestination.HuggingFace,
                                                FoundryDestination.LlmChooser,
                                                -> navController.navigate(Settings(SettingsTab.Services.name))

                                                else -> navController.navigate(Settings())
                                            }
                                        },
                                        onRefreshFeed = homeViewModel::refresh,
                                        isRefreshing = homeState.isRefreshing,
                                        feedItems = homeState.feed,
                                    )
                                },
                            )
                        }
                        composable<LauncherSettings> {
                            LauncherSettingsScreen(
                                onClose = { navController.navigateUp() },
                                onOpenAiSettings = {
                                    navController.navigate(Settings(SettingsTab.Services.name))
                                },
                            )
                        }
                        composable<Shell> {
                            HudShellScreen(onClose = { navController.navigateUp() })
                        }
                        composable<Files> { entry ->
                            val filesPath = entry.toRoute<Files>().path
                            LauncherAppShell(
                                title = "Files",
                                onClose = { navController.navigateUp() },
                            ) {
                                if (filesPath != null) {
                                    SandboxFilesContent(initialPath = filesPath)
                                } else {
                                    SandboxFilesContent()
                                }
                            }
                        }
                        composable<Packages> {
                            LauncherAppShell(
                                title = "Sandbox",
                                onClose = { navController.navigateUp() },
                            ) {
                                SandboxPackagesContent()
                            }
                        }
                        composable<Home> {
                            // Foundry brushed-metal home (Page 1): a live newsfeed (Heartbeat
                            // updates, pull-to-refresh) + integration boxes whose gears open the
                            // matching Settings tab. The Workspace tab leads to chat / sandbox.
                            val homeViewModel = koinViewModel<FoundryHomeViewModel>()
                            val homeState by homeViewModel.state.collectAsStateWithLifecycle()
                            FoundryHome(
                                onNavigate = { dest ->
                                    when (dest) {
                                        FoundryDestination.Chat ->
                                            navController.navigate(FoundryChat(WorkspaceTab.Chat.name))

                                        FoundryDestination.Shell ->
                                            navController.navigate(FoundryChat(WorkspaceTab.Shell.name))

                                        FoundryDestination.Compare ->
                                            navController.navigate(FoundryChat(WorkspaceTab.MultiChat.name))

                                        FoundryDestination.Mcp ->
                                            navController.navigate(Settings(SettingsTab.Tools.name))

                                        FoundryDestination.Projects ->
                                            navController.navigate(Settings(SettingsTab.Projects.name))

                                        FoundryDestination.Services,
                                        FoundryDestination.Ollama,
                                        FoundryDestination.HuggingFace,
                                        FoundryDestination.LlmChooser,
                                        -> navController.navigate(Settings(SettingsTab.Services.name))

                                        else -> navController.navigate(Settings())
                                    }
                                },
                                onRefreshFeed = homeViewModel::refresh,
                                isRefreshing = homeState.isRefreshing,
                                feedItems = homeState.feed,
                                navigationTabBar = navigationTabBar,
                            )
                        }
                        composable<FoundryChat> { entry ->
                            val initialTab = entry.toRoute<FoundryChat>().tab
                                ?.let { runCatching { WorkspaceTab.valueOf(it) }.getOrNull() }
                                ?: WorkspaceTab.Chat
                            WorkspaceScreen(
                                chatViewModel = chatViewModel,
                                textToSpeech = textToSpeech,
                                onNavigateToSettings = {
                                    navController.navigate(Settings())
                                },
                                onOpenHelp = { showHelp = true },
                                isSandboxAvailable = currentPlatform is Platform.Mobile.Android,
                                navigationTabBar = if (showTabBar) navigationTabBar else null,
                                initialTab = initialTab,
                            )
                        }
                        composable<FoundryStub> { entry ->
                            val stub: FoundryStub = entry.toRoute()
                            FoundryPlaceholderScreen(
                                title = stub.title,
                                description = stub.description,
                                onBack = { navController.navigateUp() },
                            )
                        }
                        composable<Settings> { entry ->
                            val initialTab = entry.toRoute<Settings>().tab
                                ?.let { runCatching { SettingsTab.valueOf(it) }.getOrNull() }
                            DisposableEffect(Unit) {
                                onDispose {
                                    chatViewModel.refreshSettings()
                                }
                            }
                            SettingsScreen(
                                initialTab = initialTab,
                                onNavigateBack = {
                                    chatViewModel.refreshSettings()
                                    navController.navigateUp()
                                },
                                navigationTabBar = null,
                            )
                        }
                    }

                    // Help bubble on the hub. The workspace carries its own "?" chip in
                    // the tab strip, and Settings has inline guide cards, so the floating
                    // bubble lives on Home — where it lands over the empty corner of the
                    // box grid rather than covering a control.
                    if (isHome) {
                        HelpBubble(
                            onClick = { showHelp = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .navigationBarsPadding()
                                .padding(16.dp),
                        )
                    }

                    if (showHelp) {
                        HelpAssistantSheet(
                            onDismiss = { showHelp = false },
                            onOpenServices = {
                                navController.navigate(Settings(SettingsTab.Services.name))
                            },
                            onOpenMcp = {
                                navController.navigate(Settings(SettingsTab.Tools.name))
                            },
                            onReplayTour = { showTour = true },
                        )
                    }

                    if (showTour) {
                        WelcomeTour(
                            onFinish = {
                                appSettings.setWelcomeSeen()
                                showTour = false
                            },
                            onAskAssistant = {
                                appSettings.setWelcomeSeen()
                                showTour = false
                                showHelp = true
                            },
                        )
                    }
                }
            }
        }
    }
}
