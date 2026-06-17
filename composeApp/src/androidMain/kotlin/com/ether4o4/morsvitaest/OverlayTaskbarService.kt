package com.ether4o4.morsvitaest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ether4o4.morsvitaest.ui.overlay.OverlayWidgetPanel
import java.util.Calendar

/**
 * A permanent, system-wide taskbar drawn as an overlay window so it stays on
 * screen over every other app (Messages, Phone, browsers, …) and never
 * disappears. Once the service is running the bar is always visible — it does
 * not depend on any per-app show command.
 *
 * The bar hosts a Start orb (brings MorsVitaEst forward + opens the Start menu),
 * Phone + Messages shortcuts, and a clock. Tapping the clock toggles a floating
 * widget/chat panel (the live agent chat + widgets) that also floats over other
 * apps and only closes when the clock (or its ✕) is tapped again.
 *
 * Acts as the lifecycle / view-model-store / saved-state owner for the Compose
 * panel hosted in the overlay. Requires the "Display over other apps" permission
 * and runs as a foreground service so the OS keeps it alive.
 */
class OverlayTaskbarService :
    Service(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var barView: View? = null
    private var clockView: TextView? = null
    private var panelView: ComposeView? = null

    private val handler = Handler(Looper.getMainLooper())
    private val clockTick = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 30_000L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        startAsForeground()
        ensureBar()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Any other start command just (re-)asserts the permanent bar.
        ensureBar()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(clockTick)
        hidePanel()
        removeBar()
        store.clear()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    // ---- Foreground notification ------------------------------------------

    private fun startAsForeground() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Taskbar",
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "Keeps the MorsVitaEst taskbar on screen." }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val launch = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("MorsVitaEst taskbar")
            .setContentText("Tap to open MorsVitaEst")
            .setSmallIcon(android.R.drawable.ic_menu_sort_by_size)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            // If the typed foreground start is rejected on this OS, fall back to an
            // untyped one so the service still runs foreground and survives the app
            // going to the background (otherwise the bar is killed when you leave MVE).
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (e2: Exception) {
                Toast.makeText(this, "Taskbar: foreground start failed (${e2.message})", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ---- Permanent overlay bar --------------------------------------------

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun wm(): WindowManager = windowManager ?: (getSystemService(Context.WINDOW_SERVICE) as WindowManager).also { windowManager = it }

    private fun ensureBar() {
        if (barView != null) return
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Taskbar needs the “Display over other apps” permission", Toast.LENGTH_LONG).show()
            return
        }

        val bar = buildBar()
        barView = bar
        // Visible immediately and permanently — never hidden while the service runs.
        bar.visibility = View.VISIBLE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dp(BAR_HEIGHT_DP),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
        }
        try {
            wm().addView(bar, params)
            Toast.makeText(this, "MVE taskbar active", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            barView = null
            Toast.makeText(this, "Taskbar couldn't draw (${e.message})", Toast.LENGTH_LONG).show()
        }
        updateClock()
        handler.removeCallbacks(clockTick)
        handler.postDelayed(clockTick, 30_000L)
    }

    private fun removeBar() {
        val bar = barView ?: return
        try {
            windowManager?.removeView(bar)
        } catch (_: Exception) {
        }
        barView = null
        clockView = null
    }

    private fun buildBar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply { setColor(0xF20E1014.toInt()) }
            setPadding(dp(10), dp(4), dp(10), dp(4))
            elevation = dp(8).toFloat()
        }

        bar.addView(
            orbButton("≡") { bringAppToFront() },
            LinearLayout.LayoutParams(dp(38), dp(38)).apply { marginEnd = dp(8) },
        )
        bar.addView(
            glyphButton("✆") { openDialer() },
            LinearLayout.LayoutParams(dp(38), dp(38)).apply { marginEnd = dp(8) },
        )
        bar.addView(
            glyphButton("✉") { openMessages() },
            LinearLayout.LayoutParams(dp(38), dp(38)),
        )

        bar.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))

        // Tapping the clock toggles the floating widget/chat panel.
        val clock = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(9).toFloat()
                setColor(0x22FFFFFF)
            }
            isClickable = true
            setOnClickListener { togglePanel() }
        }
        clockView = clock
        bar.addView(
            clock,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        return bar
    }

    private fun orbButton(glyph: String, onClick: () -> Unit): TextView = TextView(this).apply {
        text = glyph
        setTextColor(Color.WHITE)
        textSize = 17f
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            colors = intArrayOf(0xFF8FE6FF.toInt(), 0xFF1E7FD0.toInt(), 0xFF0B3C73.toInt())
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = dp(22).toFloat()
        }
        isClickable = true
        setOnClickListener { onClick() }
    }

    private fun glyphButton(glyph: String, onClick: () -> Unit): TextView = TextView(this).apply {
        text = glyph
        setTextColor(Color.WHITE)
        textSize = 16f
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(9).toFloat()
            setColor(0x22FFFFFF)
            setStroke(dp(1), 0x33FFFFFF)
        }
        isClickable = true
        setOnClickListener { onClick() }
    }

    private fun updateClock() {
        val c = Calendar.getInstance()
        val hour24 = c.get(Calendar.HOUR_OF_DAY)
        val h12 = if (hour24 % 12 == 0) 12 else hour24 % 12
        val minute = c.get(Calendar.MINUTE).toString().padStart(2, '0')
        val ampm = if (hour24 < 12) "AM" else "PM"
        clockView?.text = "$h12:$minute $ampm"
    }

    // ---- Floating widget/chat panel (Compose) -----------------------------

    private fun togglePanel() {
        if (panelView != null) hidePanel() else showPanel()
    }

    private fun showPanel() {
        if (panelView != null || !Settings.canDrawOverlays(this)) return
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayTaskbarService)
            setViewTreeViewModelStoreOwner(this@OverlayTaskbarService)
            setViewTreeSavedStateRegistryOwner(this@OverlayTaskbarService)
            setContent {
                OverlayWidgetPanel(onClose = { hidePanel() })
            }
        }
        panelView = view

        val screenHeight = resources.displayMetrics.heightPixels
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            (screenHeight * 0.72f).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Focusable (no NOT_FOCUSABLE) so the keyboard works in the chat input;
            // NOT_TOUCH_MODAL lets taps outside the panel reach the app behind it.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            y = dp(BAR_HEIGHT_DP)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        try {
            wm().addView(view, params)
        } catch (_: Exception) {
            panelView = null
        }
    }

    private fun hidePanel() {
        val view = panelView ?: return
        try {
            windowManager?.removeView(view)
        } catch (_: Exception) {
        }
        panelView = null
    }

    // ---- Button actions ---------------------------------------------------

    private fun bringAppToFront() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            // Ask the launcher to open the Start menu when it comes to the front.
            putExtra(EXTRA_OPEN_START_MENU, true)
        } ?: return
        try {
            startActivity(intent)
        } catch (_: Exception) {
        }
    }

    private fun openDialer() = launch(Intent(Intent.ACTION_DIAL))

    private fun openMessages() = launch(Intent(Intent.ACTION_VIEW, "sms:".toUri()))

    private fun launch(intent: Intent) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val CHANNEL_ID = "mve_taskbar_overlay"
        private const val NOTIFICATION_ID = 9102
        private const val BAR_HEIGHT_DP = 46
        const val ACTION_SHOW = "com.ether4o4.morsvitaest.taskbar.SHOW"
        const val ACTION_STOP = "com.ether4o4.morsvitaest.taskbar.STOP"

        private fun send(context: Context, action: String) {
            val intent = Intent(context, OverlayTaskbarService::class.java).setAction(action)
            ContextCompat.startForegroundService(context, intent)
        }

        /** Start (if needed) the permanent overlay bar. */
        fun show(context: Context) = send(context, ACTION_SHOW)

        /** Tear the overlay down entirely. */
        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, OverlayTaskbarService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}
