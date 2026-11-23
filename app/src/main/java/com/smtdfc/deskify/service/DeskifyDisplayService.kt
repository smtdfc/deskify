package com.smtdfc.deskify.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.smtdfc.deskify.core.detection.display.DisplayListenerWrapper

/**
 * Foreground service that owns the display-listener lifecycle.
 *
 * ## Why a foreground service at all
 *
 * The detector is pull-based; this service is the *push* side. It
 * keeps a [DisplayListenerWrapper] registered for the lifetime of the
 * process so that when a display connects or disconnects the app
 * reacts immediately (re-run the detector, start / stop the
 * [com.smtdfc.deskify.desktop.DesktopActivity], update the
 * diagnostics screen, etc.).
 *
 * ## Stage 5 status
 *
 * In this stage the service is a stub: it registers the listener and
 * starts the foreground notification. The "start DesktopActivity on
 * connect" and "move windows back to primary on disconnect" logic
 * lands in stage 5.
 */
class DeskifyDisplayService : Service() {

    private var listenerToken: DisplayListenerWrapper.UnregisterToken? = null
    private var listenerWrapper: DisplayListenerWrapper? = null

    override fun onCreate() {
        super.onCreate()
        // Register the display listener on the main looper so the
        // callbacks fire on the UI thread.
        val wrapper = DisplayListenerWrapper(this) { displays ->
            onDisplaysChanged(displays)
        }
        listenerWrapper = wrapper
        listenerToken = wrapper.registerOnMainLooper()

        // Start the foreground notification (required on Android 9+
        // for a foreground service that outlives the starting
        // activity).
        startForegroundCompat()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // The service is restarted by the system if it is killed.
        // We do not handle any specific intents in this stage.
        return START_STICKY
    }

    override fun onDestroy() {
        listenerToken?.unregister()
        listenerWrapper = null
        listenerToken = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ------------------------------------------------------------------
    // display-listener callback
    // ------------------------------------------------------------------

    private fun onDisplaysChanged(displays: List<android.view.Display>) {
        // Stage 5 will:
        //  - if an external display is now present, start
        //    DesktopActivity with ActivityOptions.setLaunchDisplayId
        //    on that display.
        //  - if an external display was removed, finish the
        //    DesktopActivity (the system will do this for us, but
        //    we also want to move any open windows back to the
        //    primary display).
        // For now, just log so the user can verify the listener is
        // working.
        android.util.Log.d("DeskifyDisplayService", "Displays changed: ${displays.map { it.displayId }}")
    }

    // ------------------------------------------------------------------
    // foreground notification
    // ------------------------------------------------------------------

    private fun startForegroundCompat() {
        val notification = buildNotification()
        startForeground(1, notification)
    }

    private fun buildNotification(): android.app.Notification {
        val channelId = "deskify_display_service"
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channel = android.app.NotificationChannel(
                channelId,
                "Deskify display service",
                android.app.NotificationManager.IMPORTANCE_LOW,
            )
            nm.createNotificationChannel(channel)
        }
        val contentIntent = android.content.Intent(this, com.smtdfc.deskify.feature.diagnostics.MainActivity::class.java)
        contentIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending = android.app.PendingIntent.getActivity(
            this, 0, contentIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        return if (android.os.Build.VERSION.SDK_INT >= 26) {
            android.app.Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(this)
        }
            .setContentTitle("Deskify")
            .setContentText("Watching external displays")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }
}
