package com.smtdfc.deskify

import android.app.Application

/**
 * The process-wide [Application] subclass.
 *
 * Holds the singletons that outlive any single activity:
 *  - the settings store (used by stage 2 to read / write / revert
 *    Settings.Global rows),
 *  - the window manager (stage 4),
 *  - the app catalog (used by the start menu in stage 3).
 *
 * In this stage (detection) we only need the application instance to
 * exist; the fields are added as the later stages land.
 */
class DeskifyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        private var instance: DeskifyApplication? = null

        /** The singleton. Null until [onCreate] runs. */
        fun get(): DeskifyApplication = instance
            ?: throw IllegalStateException(
                "DeskifyApplication not initialized. " +
                    "Make sure the manifest declares android:name='.DeskifyApplication'.",
            )
    }
}
