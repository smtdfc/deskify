package com.smtdfc.deskify.core.detection

import android.content.Context
import com.smtdfc.deskify.core.detection.CheckResult

/**
 * Unit-testable fake for [CapabilityDetector].
 *
 * Production code should use [CapabilityDetector] directly. This fake
 * exists so that the app module's diagnostics screen can be exercised
 * in Robolectric / instrumentation tests without a real display.
 */
object FakeCapabilityDetector {

    /**
     * Returns a [CapabilityReport] that pretends every check passed.
     * Useful for testing the "happy path" UI.
     */
    fun allPass(context: Context): CapabilityReport = CapabilityReport(
        androidSdk = 36,
        oem = "TestOem",
        externalDisplayConnected = true,
        checks = listOf(
            CheckResult.pass("display.external", "External display connected", "Test display #1"),
            CheckResult.pass("display.metrics", "Per-display metrics", "Test display #1 = 1920x1080 @ 213dpi / 60Hz [DISPLAY_CATEGORY_EXTERNAL]"),
            CheckResult.pass("display.hostsActivities", "External display can host activities", "Yes"),
            CheckResult.pass("freeform.feature", "Freeform window management feature", "Set"),
            CheckResult.pass("freeform.enable_freeform_support", "enable_freeform_support", "1"),
            CheckResult.pass("freeform.force_desktop_mode_on_external_displays", "force_desktop_mode_on_external_displays", "1"),
            CheckResult.pass("freeform.force_resizable_activities", "force_resizable_activities", "1"),
            CheckResult.pass("desktop.vendor", "Native vendor desktop mode", "None (AOSP)"),
            CheckResult.pass("desktop.android14", "Native desktop windowing (Android 14+)", "Available"),
            CheckResult.pass("permission.write_secure_settings", "WRITE_SECURE_SETTINGS held by app", "Granted"),
        ),
        verdict = SupportLevel.SUPPORTED,
    )

    /**
     * Returns a [CapabilityReport] that pretends the external display
     * is absent and the permission is missing. Useful for testing the
     * "unsupported / setup needed" UI.
     */
    fun unsupported(context: Context): CapabilityReport = CapabilityReport(
        androidSdk = 30,
        oem = "TestOem",
        externalDisplayConnected = false,
        checks = listOf(
            CheckResult.fail("display.external", "External display connected", "No display attached."),
            CheckResult.unavailable("display.metrics", "Per-display metrics", "No displays."),
            CheckResult.unavailable("display.hostsActivities", "External display can host activities", "No display to test."),
            CheckResult.fail("freeform.feature", "Freeform window management feature", "Not set."),
            CheckResult.fail("freeform.enable_freeform_support", "enable_freeform_support", "0"),
            CheckResult.fail("freeform.force_desktop_mode_on_external_displays", "force_desktop_mode_on_external_displays", "0"),
            CheckResult.unavailable("freeform.force_resizable_activities", "force_resizable_activities", "Setting does not exist on this version."),
            CheckResult.fail("desktop.vendor", "Native vendor desktop mode", "None found."),
            CheckResult.unavailable("desktop.android14", "Native desktop windowing (Android 14+)", "Not available on this version."),
            CheckResult.fail("permission.write_secure_settings", "WRITE_SECURE_SETTINGS held by app", "Not granted. Run: adb shell pm grant com.smtdfc.deskify android.permission.WRITE_SECURE_SETTINGS"),
        ),
        verdict = SupportLevel.UNSUPPORTED,
    )
}
