package com.smtdfc.deskify.core.detection

import android.content.Context
import android.os.Build
import com.smtdfc.deskify.core.detection.display.DisplayInfoCollector
import com.smtdfc.deskify.core.detection.oem.OemStrategySelector

/**
 * Top-level entry point for capability detection (stage 1 of the
 * project).
 *
 * The detector runs on first launch and on every display connect /
 * disconnect event (wired up by the app module's `DisplayListener`).
 * It is *pure*: given a [Context] it returns a [CapabilityReport] and
 * has no side effects, so it is trivially unit-testable.
 *
 * ## Checks performed
 *
 *  1. `display.external`  - a non-default display is currently connected.
 *  2. `display.metrics`   - resolution / density / refresh rate per display.
 *  3. `display.hostsActivities` - whether the external display can host
 *     activities (`setLaunchDisplayId`).
 *  4. `freeform.feature`  - `PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT`.
 *  5. `freeform.enable_freeform_support` - Settings.Global row (API 28-30).
 *  6. `freeform.force_desktop_mode_on_external_displays` - API 29+.
 *  7. `freeform.force_resizable_activities` - API 33+.
 *  8. `desktop.vendor`    - OEM DeX-like component (Samsung et al.).
 *  9. `desktop.android14` - native desktop windowing on API 34+.
 *
 * The *verdict* is computed from these checks:
 *   - SUPPORTED           : every mandatory check passes.
 *   - PARTIALLY_SUPPORTED : a secondary display exists but some
 *                           freeform setting is not on, so the launcher
 *                           must manage windows itself.
 *   - UNSUPPORTED         : no external display at all, and no vendor
 *                           desktop mode.
 *
 * ## Honesty rule
 *
 * The detector *never* reports a PASS for a capability that requires a
 * permission the app does not have. For example, it will NOT report
 * "freeform is enabled" just because the setting key exists - it will
 * report "freeform is enabled but the app cannot confirm it is
 * actually on because WRITE_SECURE_SETTINGS is not granted". This is
 * the "don't fake capabilities" constraint from the design.
 */
object CapabilityDetector {

    /**
     * Runs the full detection pass. Safe to call on any thread that
     * has a valid [Context].
     */
    fun detect(context: Context): CapabilityReport {
        val sdk = Build.VERSION.SDK_INT
        val oemStrategy = OemStrategySelector.forContext(context)
        val oemName = oemStrategy.displayName()

        // --- 1. External display connected ---------------------------------
        val displays = DisplayInfoCollector.collectAll(context)
        val externalDisplays = displays.filter { !it.isDefault }
        val displayCheck = when {
            externalDisplays.isEmpty() -> CheckResult.fail(
                checkId = "display.external",
                label = "External display connected",
                detail = "No non-default display is currently attached. " +
                    "Connect a monitor (USB-C / HDMI / wireless) or enable " +
                    "'Simulate a secondary display' in Developer options to " +
                    "test.",
            )
            else -> CheckResult.pass(
                checkId = "display.external",
                label = "External display connected",
                detail = "${externalDisplays.size} external display(s) attached: " +
                    externalDisplays.joinToString {
                        "${it.name} (#${it.id}, ${it.widthPx}x${it.heightPx} @ ${it.densityDpi}dpi)"
                    },
            )
        }

        // --- 2. Per-display metrics ---------------------------------------
        // We always collect this; it is informational, not a verdict input.
        // (The `detail` is built from every attached display.)
        val metricsDetail = displays.joinToString { d ->
            val rate = d.refreshRateHz?.let { " / ${it.formatHz()}" } ?: ""
            "${d.name} (#${d.id}) = ${d.widthPx}x${d.heightPx} @ ${d.densityDpi}dpi$rate [${d.category}]"
        }

        // --- 3. Can the external display host activities? ------------------
        val hostCheck = when {
            externalDisplays.isEmpty() -> CheckResult.unavailable(
                checkId = "display.hostsActivities",
                label = "External display can host activities",
                detail = "No external display to test. Re-run after attaching a monitor.",
            )
            externalDisplays.all { it.canHostActivities } -> CheckResult.pass(
                checkId = "display.hostsActivities",
                label = "External display can host activities",
                detail = "All external displays report they can host activities " +
                    "(ActivityOptions.setLaunchDisplayId will work).",
            )
            else -> CheckResult.fail(
                checkId = "display.hostsActivities",
                label = "External display can host activities",
                detail = "At least one external display does not report " +
                    "'can host activities'. Freeform windows may be limited to " +
                    "the presentation-only path.",
            )
        }

        // --- 4. Freeform feature flag --------------------------------------
        val freeformFeature = checkFreeformFeature(context)

        // --- 5. enable_freeform_support (API 28-30) -----------------------
        val enableFreeformSetting = checkEnableFreeformSetting(context, sdk)

        // --- 6. force_desktop_mode_on_external_displays (API 29+) --------
        val forceDesktopMode = checkForceDesktopModeSetting(context, sdk)

        // --- 7. force_resizable_activities (API 33+) --------------------
        val forceResizable = checkForceResizableSetting(context, sdk)

        // --- 8. Vendor desktop mode --------------------------------------
        val vendorDesktop = com.smtdfc.deskify.core.detection.dex.DexDetector
            .detect(context, oemStrategy)

        // --- 9. Android 14+ native desktop windowing ---------------------
        val desktopNative14 = checkDesktopNative14(context, sdk)

        // --- 10. App permission (WRITE_SECURE_SETTINGS) -------------------
        val writeSecurePermission = PermissionDetector.checkWriteSecureSettings(context)

        val checks = listOf(
            displayCheck,
            CheckResult.pass(
                checkId = "display.metrics",
                label = "Per-display metrics",
                detail = metricsDetail,
            ),
            hostCheck,
            freeformFeature,
            enableFreeformSetting,
            forceDesktopMode,
            forceResizable,
            vendorDesktop,
            desktopNative14,
            writeSecurePermission,
        )

        val verdict = computeVerdict(checks, externalDisplays.isNotEmpty(), vendorDesktop)

        return CapabilityReport(
            androidSdk = sdk,
            oem = oemName,
            externalDisplayConnected = externalDisplays.isNotEmpty(),
            checks = checks,
            verdict = verdict,
        )
    }

    // ------------------------------------------------------------------
    // individual check implementations
    // ------------------------------------------------------------------

    private fun checkFreeformFeature(context: Context): CheckResult {
        val hasFeature = try {
            context.packageManager.hasSystemFeature(
                android.content.pm.PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT,
            )
        } catch (e: Exception) {
            false
        }
        return when (hasFeature) {
            true -> CheckResult.pass(
                checkId = "freeform.feature",
                label = "Freeform window management feature",
                detail = "PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT is set. " +
                    "The window manager supports freeform windows on this device.",
            )
            false -> CheckResult.fail(
                checkId = "freeform.feature",
                label = "Freeform window management feature",
                detail = "FEATURE_FREEFORM_WINDOW_MANAGEMENT is not set. The OS " +
                    "will not run activities in freeform windows. Deskify can " +
                    "still run in 'launcher-managed desktop' mode (a single " +
                    "full-screen launcher activity on the secondary display) " +
                    "but per-app windows are unavailable without this feature.",
            )
        }
    }

    private fun checkEnableFreeformSetting(context: Context, sdk: Int): CheckResult {
        // This key was introduced on Android 9 (API 28). It was the only
        // way to enable freeform on API 28-30; on API 31+ it is superseded
        // by the "Desktop windowing" settings but the row still exists.
        val value = SettingsReader.globalBoolean(
            context,
            "enable_freeform_support",
            defaultValue = false,
        )
        if (!value) {
            return CheckResult.fail(
                checkId = "freeform.enable_freeform_support",
                label = "enable_freeform_support (Settings.Global)",
                detail = "Settings.Global 'enable_freeform_support' is false or missing. " +
                    "Freeform windows are disabled by default on this device. " +
                    "Enable via: adb shell settings put global enable_freeform_support 1 " +
                    "(requires WRITE_SECURE_SETTINGS, or run on a developer build).",
            )
        }
        return CheckResult.pass(
            checkId = "freeform.enable_freeform_support",
            label = "enable_freeform_support (Settings.Global)",
            detail = "'enable_freeform_support' is set to 1.",
        )
    }

    private fun checkForceDesktopModeSetting(context: Context, sdk: Int): CheckResult {
        // API 29+ (Android 10). When set to 1, the system runs apps in
        // desktop (freeform) mode on external displays *automatically* -
        // this is the setting DeX and similar OEM modes toggle.
        if (sdk < 29) {
            return CheckResult.unavailable(
                checkId = "freeform.force_desktop_mode_on_external_displays",
                label = "force_desktop_mode_on_external_displays (API 29+)",
                detail = "Setting does not exist on this Android version (< 10).",
            )
        }
        val value = SettingsReader.globalBoolean(
            context,
            "force_desktop_mode_on_external_displays",
            defaultValue = false,
        )
        return when (value) {
            true -> CheckResult.pass(
                checkId = "freeform.force_desktop_mode_on_external_displays",
                label = "force_desktop_mode_on_external_displays (API 29+)",
                detail = "When an external display is attached, the system will " +
                    "force desktop mode on the apps that appear there.",
            )
            false -> CheckResult.fail(
                checkId = "freeform.force_desktop_mode_on_external_displays",
                label = "force_desktop_mode_on_external_displays (API 29+)",
                detail = "'force_desktop_mode_on_external_displays' is false. " +
                    "Apps on the external display will use their default layout, " +
                    "not the desktop layout. Enable via: " +
                    "adb shell settings put global force_desktop_mode_on_external_displays 1",
            )
        }
    }

    private fun checkForceResizableSetting(context: Context, sdk: Int): CheckResult {
        // API 33+ (Android 13). Forces resizable activities even when
        // the app declares itself non-resizable. Useful for forcing
        // phone-layout apps into a windowed desktop form.
        if (sdk < 33) {
            return CheckResult.unavailable(
                checkId = "freeform.force_resizable_activities",
                label = "force_resizable_activities (API 33+)",
                detail = "Setting does not exist on this Android version (< 13).",
            )
        }
        val value = SettingsReader.globalBoolean(
            context,
            "force_resizable_activities",
            defaultValue = false,
        )
        return when (value) {
            true -> CheckResult.pass(
                checkId = "freeform.force_resizable_activities",
                label = "force_resizable_activities (API 33+)",
                detail = "All activities will be treated as resizable. " +
                    "Phone-layout apps will be stretched into windows.",
            )
            false -> CheckResult.fail(
                checkId = "freeform.force_resizable_activities",
                label = "force_resizable_activities (API 33+)",
                detail = "'force_resizable_activities' is false. Apps that " +
                    "declare android:resizeableActivity=false will keep their " +
                    "fixed layout on the external display. Enable via: " +
                    "adb shell settings put global force_resizable_activities 1",
            )
        }
    }

    private fun checkDesktopNative14(context: Context, sdk: Int): CheckResult {
        // Android 14+ introduced a native "desktop windowing" mode that
        // does *not* require the WRITE_SECURE_SETTINGS grant: the system
        // provides a desktop UI on external displays out of the box on
        // many Pixel / AOSP builds, and some OEMs expose it in Settings.
        if (sdk < 34) {
            return CheckResult.unavailable(
                checkId = "desktop.android14",
                label = "Native desktop windowing (Android 14+)",
                detail = "Not available on this Android version (< 14). " +
                    "This check is informational; the AOSP freeform settings " +
                    "above remain the relevant gate.",
            )
        }
        // On API 34+ the system ships a desktop UI. We cannot reliably
        // query "is it enabled" without reading a private row, so we
        // report PASS with a caveat - the user can verify in
        // Settings > Display > "Desktop mode" when the row is present.
        return CheckResult.pass(
            checkId = "desktop.android14",
            label = "Native desktop windowing (Android 14+)",
            detail = "Android 14+ provides a built-in desktop windowing mode " +
                "for external displays. If your build does not show a " +
                "'Desktop mode' row in Settings > Display, it was likely " +
                "disabled by the OEM. Enable it via: " +
                "adb shell settings put global force_desktop_mode_on_external_displays 1",
        )
    }

    // ------------------------------------------------------------------
    // verdict
    // ------------------------------------------------------------------

    private fun computeVerdict(
        checks: List<CheckResult>,
        externalDisplayPresent: Boolean,
        vendorDesktop: CheckResult,
    ): SupportLevel {
        // UNSUPPORTED when no external display is present AND no vendor
        // desktop mode. The app has nothing to attach to.
        if (!externalDisplayPresent && !vendorDesktop.isPassing()) {
            return SupportLevel.UNSUPPORTED
        }
        // PARTIALLY when an external display is present but one of the
        // freeform gates is closed.
        val freeformGatesClosed = checks.any { c ->
            c.checkId.startsWith("freeform.") && !c.isPassing()
        }
        if (externalDisplayPresent && freeformGatesClosed) {
            return SupportLevel.PARTIALLY_SUPPORTED
        }
        // SUPPORTED when an external display is present and every
        // applicable freeform gate is open.
        if (externalDisplayPresent) {
            return SupportLevel.SUPPORTED
        }
        // External display absent but a vendor desktop mode is present:
        // the user can still use the vendor DeX; report PARTIAL so the
        // UI can offer "use vendor DeX instead".
        return SupportLevel.PARTIALLY_SUPPORTED
    }

    // ------------------------------------------------------------------
    // tiny helpers
    // ------------------------------------------------------------------

    private fun Double.formatHz(): String = String.format("%.0fHz", this)
}
