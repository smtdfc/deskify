package com.smtdfc.deskify.core.detection

import android.content.Context
import android.content.pm.PackageManager

/**
 * Detects whether the *app itself* holds the permissions required for
 * the freeform / desktop auto-enable flow (stage 2 of the project).
 *
 * This is kept separate from [CapabilityDetector] because the two
 * answer different questions:
 *   - [CapabilityDetector]: "does the *device* support X?"
 *   - [PermissionDetector] : "does *this app* hold the permission to
 *     *change* X?"
 *
 * The auto-enable logic in stage 2 will combine the two: if the device
 * supports freeform but the app lacks `WRITE_SECURE_SETTINGS`, the
 * report must say so explicitly (the "don't fake capabilities" rule).
 */
object PermissionDetector {

    /**
     * Checks whether the app holds [android.Manifest.permission.WRITE_SECURE_SETTINGS].
     *
     * @return a [CheckResult]. PASS when held; FAIL when not (with a
     *         detail that includes the exact ADB grant command).
     */
    fun checkWriteSecureSettings(context: Context): CheckResult {
        val held = context.checkSelfPermission(
            android.Manifest.permission.WRITE_SECURE_SETTINGS,
        ) == PackageManager.PERMISSION_GRANTED
        return when (held) {
            true -> CheckResult.pass(
                checkId = "permission.write_secure_settings",
                label = "WRITE_SECURE_SETTINGS held by app",
                detail = "The app holds android.permission.WRITE_SECURE_SETTINGS. " +
                    "It can read *and* write the Settings.Global rows used to " +
                    "enable freeform / desktop modes.",
            )
            false -> CheckResult.fail(
                checkId = "permission.write_secure_settings",
                label = "WRITE_SECURE_SETTINGS held by app",
                detail = "The app does NOT hold WRITE_SECURE_SETTINGS. It can " +
                    "read the Settings.Global rows but cannot write them. To " +
                    "grant it: " +
                    "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS " +
                    "(a Shizuku alternative is planned for a future stage).",
            )
        }
    }
}
