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
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import java.util.Calendar

/**
 * A persistent, system-wide taskbar drawn as a floating overlay window so it
 * stays on screen over every other app (Messages, Phone, browsers, …) — not just
 * inside the launcher. It hosts a Start button (brings MorsVitaEst to the front),
 * Phone + Messages shortcuts, and a live clock.
 *
 * Shown only while MorsVitaEst itself is in the background (the activity drives
 * [show]/[hide] from its start/stop), so the launcher's own in-app taskbar isn't
 * doubled up on the home screen.
 *
 * Requires the "Display over other apps" permission and runs as a foreground
 * service (with a minimal ongoing notification) so the OS keeps it alive.
 */
class OverlayTaskbarService : Service() {

    private var windowManager: WindowManager? = null
    private var barView: View? = null
    private var clockView: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val clockTick = object : Runnable {
        override fun run() {
            updateClock()
            // Re-run at the top of the next minute (≈ every 30s is plenty).
            handler.postDelayed(this, 30_000L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        ensureBar()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        ensureBar()
        when (intent?.action) {
            ACTION_SHOW -> setBarVisible(true)
            ACTION_HIDE -> setBarVisible(false)
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(clockTick)
        removeBar()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    // ---- Foreground notification ------------------------------------------

    private fun startAsForeground() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Taskbar",
            NotificationManager.IMPORTANCE_MIN,
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
            // If foregrounding fails (e.g. background-start restrictions) we still
            // try to keep the overlay; the bar itself is added separately.
        }
    }

    // ---- Overlay bar ------------------------------------------------------

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun ensureBar() {
        if (barView != null) return
        if (!Settings.canDrawOverlays(this)) {
            // No permission — nothing we can draw. Don't crash; just stay a no-op
            // foreground service until permission is granted and we're re-started.
            return
        }
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val bar = buildBar()
        barView = bar
        bar.visibility = View.GONE

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
            wm.addView(bar, params)
        } catch (_: Exception) {
            barView = null
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
            // Already detached.
        }
        barView = null
        clockView = null
    }

    private fun setBarVisible(visible: Boolean) {
        ensureBar()
        barView?.visibility = if (visible) View.VISIBLE else View.GONE
        if (visible) updateClock()
    }

    private fun buildBar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            // Dark glass with a subtle top edge highlight.
            background = GradientDrawable().apply {
                setColor(0xF20E1014.toInt())
            }
            setPadding(dp(10), dp(4), dp(10), dp(4))
            elevation = dp(8).toFloat()
        }

        // Start orb — brings MorsVitaEst to the foreground.
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

        // Flexible spacer pushes the clock to the right.
        bar.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))

        val clock = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
        }
        clockView = clock
        bar.addView(clock, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
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

    // ---- Button actions ---------------------------------------------------

    private fun bringAppToFront() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
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
        const val ACTION_HIDE = "com.ether4o4.morsvitaest.taskbar.HIDE"
        const val ACTION_STOP = "com.ether4o4.morsvitaest.taskbar.STOP"

        private fun send(context: Context, action: String) {
            val intent = Intent(context, OverlayTaskbarService::class.java).setAction(action)
            ContextCompat.startForegroundService(context, intent)
        }

        /** Start (if needed) and reveal the overlay bar — used when MVE is backgrounded. */
        fun show(context: Context) = send(context, ACTION_SHOW)

        /** Start (if needed) but keep the bar hidden — used when MVE is foregrounded. */
        fun hide(context: Context) = send(context, ACTION_HIDE)

        /** Tear the overlay down entirely. */
        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, OverlayTaskbarService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}
