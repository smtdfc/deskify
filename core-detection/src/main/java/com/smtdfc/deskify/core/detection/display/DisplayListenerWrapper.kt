package com.smtdfc.deskify.core.detection.display

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display

/**
 * Thin wrapper around [DisplayManager.DisplayListener] that the app's
 * foreground service (launcher-service module, stage 3) can register.
 *
 * ## Why a listener at all
 *
 * The detector is *pull-based*: you call [com.smtdfc.deskify.core.detection.CapabilityDetector.detect]
 * whenever you want a fresh report. The listener is *push-based*: it
 * tells you *when* a display has connected or disconnected so the app
 * can re-run the detector and update the diagnostics screen / start
 * the secondary-display launcher.
 *
 * ## Registration
 *
 * Call [register] on a main-thread handler. The returned
 * [UnregisterToken] is idempotent: calling [UnregisterToken.unregister]
 * more than once is safe.
 *
 * ## Threading
 *
 * The [DisplayManager.DisplayListener] callbacks fire on the handler
 * you pass to `registerDisplayListener`. Pass a
 * `Handler(Looper.getMainLooper())` to stay on the main thread.
 */
class DisplayListenerWrapper(
    private val context: Context,
    private val onDisplayChanged: (displays: List<Display>) -> Unit,
) {
    private var registered = false

    /**
     * The DisplayManager.DisplayListener implementation.
     *
     * This nested class captures only the callback (via the lambda),
     * not the outer `this`, so it has no reference to the wrapper's
     * mutable state.
     */
    private val listener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            onDisplayChanged(currentDisplays())
        }

        override fun onDisplayRemoved(displayId: Int) {
            onDisplayChanged(currentDisplays())
        }

        override fun onDisplayChanged(displayId: Int) {
            onDisplayChanged(currentDisplays())
        }
    }

    private fun currentDisplays(): List<Display> {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            ?: return emptyList()
        val arr = dm.displays
        return if (arr == null) emptyList() else arr.toList()
    }

    /**
     * Registers the listener. Must be called on a thread with a Looper.
     *
     * @param handler the [Handler] the callbacks should fire on.
     * @return a [UnregisterToken] you can hold onto to unregister later.
     */
    fun register(handler: Handler): UnregisterToken {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            ?: return UnregisterToken { }
        if (registered) return UnregisterToken { }
        dm.registerDisplayListener(listener, handler)
        registered = true
        return UnregisterToken { unregister() }
    }

    /**
     * Convenience overload: registers on the main looper.
     */
    fun registerOnMainLooper(): UnregisterToken =
        register(Handler(Looper.getMainLooper()))

    /**
     * Unregisters the listener. Idempotent.
     */
    fun unregister() {
        if (!registered) return
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            ?: return
        try {
            dm.unregisterDisplayListener(listener)
        } catch (e: IllegalArgumentException) {
            // The listener was not registered (e.g. context was
            // destroyed first). Safe to ignore.
            return
        }
        registered = false
    }

    /**
     * Returned by [register]. Calling [unregister] is idempotent.
     */
    fun interface UnregisterToken {
        fun unregister()
    }
}
