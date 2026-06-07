package com.ether4o4.morsvitaest

import com.ether4o4.morsvitaest.data.AppSettings
import com.ether4o4.morsvitaest.data.BudgetManager
import com.ether4o4.morsvitaest.data.ConversationStorage
import com.ether4o4.morsvitaest.data.DataRepository
import com.ether4o4.morsvitaest.data.EmailStore
import com.ether4o4.morsvitaest.data.HeartbeatManager
import com.ether4o4.morsvitaest.data.MemoryStore
import com.ether4o4.morsvitaest.data.NotificationStore
import com.ether4o4.morsvitaest.data.RemoteDataRepository
import com.ether4o4.morsvitaest.data.SmsDraftStore
import com.ether4o4.morsvitaest.data.SmsStore
import com.ether4o4.morsvitaest.data.TaskScheduler
import com.ether4o4.morsvitaest.data.TaskStore
import com.ether4o4.morsvitaest.data.ToolExecutor
import com.ether4o4.morsvitaest.data.runMigrations
import com.ether4o4.morsvitaest.email.EmailPoller
import com.ether4o4.morsvitaest.inference.createLocalInferenceEngine
import com.ether4o4.morsvitaest.mcp.McpServerManager
import com.ether4o4.morsvitaest.network.Requests
import com.ether4o4.morsvitaest.notifications.NotificationReader
import com.ether4o4.morsvitaest.sms.SmsPoller
import com.ether4o4.morsvitaest.sms.SmsReader
import com.ether4o4.morsvitaest.sms.SmsSender
import com.ether4o4.morsvitaest.splinterlands.SplinterlandsApi
import com.ether4o4.morsvitaest.splinterlands.SplinterlandsBattleRunner
import com.ether4o4.morsvitaest.splinterlands.SplinterlandsStore
import com.ether4o4.morsvitaest.tools.CalendarPermissionController
import com.ether4o4.morsvitaest.tools.NotificationListenerController
import com.ether4o4.morsvitaest.tools.NotificationPermissionController
import com.ether4o4.morsvitaest.tools.SmsPermissionController
import com.ether4o4.morsvitaest.tools.SmsSendPermissionController
import com.ether4o4.morsvitaest.ui.chat.ChatViewModel
import com.ether4o4.morsvitaest.ui.compare.CompareViewModel
import com.ether4o4.morsvitaest.ui.foundry.FoundryHomeViewModel
import com.ether4o4.morsvitaest.ui.help.HelpAssistantViewModel
import com.ether4o4.morsvitaest.ui.sandbox.SandboxFileBrowserViewModel
import com.ether4o4.morsvitaest.ui.sandbox.SandboxPackagesViewModel
import com.ether4o4.morsvitaest.ui.sandbox.SandboxSessionViewModel
import com.ether4o4.morsvitaest.ui.settings.SandboxViewModel
import com.ether4o4.morsvitaest.ui.settings.SettingsViewModel
import com.ether4o4.morsvitaest.ui.settings.SplinterlandsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<CalendarPermissionController> { CalendarPermissionController() }
    single<NotificationPermissionController> { NotificationPermissionController() }
    single<SmsPermissionController> { SmsPermissionController() }
    single<SmsSendPermissionController> { SmsSendPermissionController() }
    single<SmsReader> { SmsReader() }
    single<SmsSender> { SmsSender() }
    single<NotificationListenerController> { NotificationListenerController() }
    single<NotificationReader> { NotificationReader() }
    single<AppSettings> {
        AppSettings(createSecureSettings()).also {
            it.runMigrations(createLegacySettings())
        }
    }
    single<Requests> {
        Requests()
    }
    single<ConversationStorage> {
        ConversationStorage(get())
    }
    single<ToolExecutor> {
        ToolExecutor()
    }
    single<MemoryStore> {
        MemoryStore(get())
    }
    single<TaskStore> {
        TaskStore(get())
    }
    single<EmailStore> {
        EmailStore(get())
    }
    single<EmailPoller> {
        EmailPoller(get<EmailStore>())
    }
    single<SmsStore> {
        SmsStore(get())
    }
    single<SmsPoller> {
        SmsPoller(get<SmsStore>(), get<SmsReader>())
    }
    single<SmsDraftStore> {
        SmsDraftStore(get())
    }
    single<NotificationStore> {
        NotificationStore(get())
    }
    single<SplinterlandsStore> {
        SplinterlandsStore(get())
    }
    single<SplinterlandsApi> {
        SplinterlandsApi()
    }
    single<HeartbeatManager> {
        HeartbeatManager(get(), get(), get(), get())
    }
    single<BudgetManager> {
        BudgetManager(get())
    }
    single<McpServerManager> {
        McpServerManager(get())
    }
    single<RemoteDataRepository> {
        RemoteDataRepository(
            requests = get(),
            appSettings = get(),
            conversationStorage = get(),
            toolExecutor = get(),
            memoryStore = get(),
            taskStore = get(),
            heartbeatManager = get(),
            emailStore = get(),
            emailPoller = get(),
            smsStore = get(),
            smsPoller = get(),
            smsReader = get(),
            smsPermissionController = get(),
            smsSendPermissionController = get(),
            smsSender = get(),
            smsDraftStore = get(),
            notificationStore = get(),
            notificationListenerController = get(),
            mcpServerManager = get(),
            sandboxController = get(),
            localInferenceEngine = createLocalInferenceEngine(),
            budgetManager = get(),
        )
    }
    single<DataRepository> { get<RemoteDataRepository>() }
    single<SplinterlandsBattleRunner> {
        SplinterlandsBattleRunner(get(), get(), get<DataRepository>(), get<DaemonController>())
    }
    single<TaskScheduler> {
        TaskScheduler(
            get<DataRepository>(),
            get(),
            get(),
            get(),
            get(),
            get<EmailPoller>(),
            get<SmsStore>(),
            get<SmsPoller>(),
            get<NotificationStore>(),
            get<BudgetManager>(),
        )
    }
    single<DaemonController> { createDaemonController() }
    single<SandboxController> { createSandboxController() }
    viewModel { SettingsViewModel(get<DataRepository>(), get<DaemonController>(), get<NotificationPermissionController>(), get<TaskScheduler>()) }
    viewModel { SandboxViewModel(get<DataRepository>(), get<SandboxController>()) }
    viewModel { SandboxFileBrowserViewModel(get<SandboxController>()) }
    viewModel { SandboxPackagesViewModel(get<SandboxController>()) }
    viewModel { SandboxSessionViewModel(get<SandboxController>(), get<DataRepository>()) }
    viewModel { SplinterlandsViewModel(get<DataRepository>(), get(), get(), get<SplinterlandsApi>()) }
    viewModel { ChatViewModel(get<DataRepository>(), get<TaskScheduler>()) }
    viewModel { CompareViewModel(get<DataRepository>()) }
    viewModel { FoundryHomeViewModel(get<DataRepository>(), get<TaskScheduler>()) }
    viewModel { HelpAssistantViewModel(get<DataRepository>()) }
}
