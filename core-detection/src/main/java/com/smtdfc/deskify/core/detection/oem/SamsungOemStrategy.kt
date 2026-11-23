package com.smtdfc.deskify.core.detection.oem

import android.content.Context

/**
 * Samsung DeX strategy.
 *
 * Samsung ships "DeX" (Desktop Experience) as the system component
 * `com.samsung.android.desktopmode`. On some newer One UI builds the
 * entry point is the `SemDesktopModeManager` service. Both are
 * *hidden / system* APIs - we can only *probe* them; we cannot link
 * against them.
 *
 * Why reflection + try/catch:
 *  - The classes exist only on Samsung devices; on every other device
 *    the `Class.forName` will throw `ClassNotFoundException`, which we
 *    swallow and report as "not present".
 *  - The exact class names have shifted between One UI versions
 *    (e.g. `SemDesktopModeManager` vs `DexManager`). We try a list of
 *    candidates and report whichever we find.
 *
 * The strategy never throws out of a probe - a missing class is an
 * *answer*, not an error.
 */
class SamsungOemStrategy : OemStrategy {

    override fun displayName(): String = "Samsung"

    /**
     * Candidate classes that indicate a Samsung desktop component is
     * installed. Order does not matter; we report the first one found.
     */
    private val desktopComponentCandidates = listOf(
        "com.samsung.android.desktopmode.SemDesktopModeManager",
        "com.samsung.android.desktopmode.DexManager",
    )

    override fun hasNativeDesktopMode(context: Context): Boolean {
        // Check 1: the package is installed on the device.
        val pkg = desktopComponentPackage(context) ?: return false
        val installed = try {
            context.packageManager.getPackageInfo(pkg, 0) != null
        } catch (e: Exception) {
            false
        }
        if (!installed) return false

        // Check 2: at least one candidate class is loadable. If the
        // package is installed but the class we probe is missing on
        // this One UI version, we still count it - the user can open
        // DeX from the app drawer.
        return desktopComponentCandidates.any { cls ->
            try {
                Class.forName(cls)
                true
            } catch (e: Throwable) {
                false
            }
        } ||
            // The presence of the package itself is enough to flag
            // "native desktop mode available". The class probe is a
            // bonus for the diagnostics detail string.
            true
    }

    override fun desktopComponentPackage(context: Context): String? {
        // Probe a fixed, well-known Samsung package name.
        val pkg = DESKTOP_PACKAGE
        return try {
            context.packageManager.getPackageInfo(pkg, 0)?.let { pkg }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Samsung uses the stock AOSP freeform key; no override needed.
     */
    override fun freeformSettingKeyOverrides(context: Context): List<String> = emptyList()

    companion object {
        const val DESKTOP_PACKAGE = "com.samsung.android.desktopmode"
    }
}
