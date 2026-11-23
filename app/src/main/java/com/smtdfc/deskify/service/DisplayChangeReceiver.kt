package com.smtdfc.deskify.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives the system's display connect / disconnect broadcasts.
 *
 * ## Why both a receiver AND a DisplayManager.DisplayListener
 *
 * Android 13+ removed the legacy `ACTION_USB_ACCESSORY` and
 * `ACTION_HEADSET_PLUG` broadcasts; the only reliable way to detect
 * a display change on modern Android is `DisplayManager.DisplayListener`.
 * On Android 9–12 the legacy `ACTION_DISPLAY_ADDED` /
 * `ACTION_DISPLAY_REMOVED` / `ACTION_DISPLAY_CHANGED` broadcasts are
 * still delivered, and they are useful as a *secondary* signal
 * (e.g. to trigger a detector re-run even if the service was not
 * started).
 *
 * This receiver is therefore a *belt-and-suspenders* mechanism. The
 * primary path is the [DeskifyDisplayService]'s
 * [com.smtdfc.deskify.core.detection.display.DisplayListenerWrapper].
 *
 * ## Stage 5 status
 *
 * In this stage the receiver just logs. Stage 5 will have it forward
 * the event to the service.
 */
class DisplayChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        android.util.Log.d("DisplayChangeReceiver", "Received: $action")

        // Forward to the service so it can react.
        when (action) {
            "android.intent.action.DISPLAY_ADDED",
            "android.intent.action.DISPLAY_REMOVED",
            "android.intent.action.DISPLAY_CHANGED",
            -> {
                val serviceIntent = Intent(context, DeskifyDisplayService::class.java)
                serviceIntent.putExtra("event", action)
                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
