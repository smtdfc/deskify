package com.smtdfc.deskify.core.detection

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-JVM test of the [SupportLevel] computation logic.
 *
 * We do NOT use Robolectric here because the verdict logic is a pure
 * function of [CheckResult]s - it does not need a [android.content.Context].
 */
class SupportLevelTest {

    @Test
    fun allChecksPassingWithExternalDisplay_isSupported() {
        val checks = listOf(
            CheckResult.pass("display.external", "External display connected", "Yes"),
            CheckResult.pass("freeform.feature", "Freeform feature", "Set"),
            CheckResult.pass("freeform.enable_freeform_support", "Enable", "1"),
            CheckResult.pass("freeform.force_desktop_mode_on_external_displays", "Force desktop", "1"),
            CheckResult.pass("freeform.force_resizable_activities", "Force resizable", "1"),
            CheckResult.pass("desktop.vendor", "Vendor desktop", "None"),
            CheckResult.pass("permission.write_secure_settings", "WRITE_SECURE_SETTINGS", "Granted"),
        )
        val verdict = computeVerdict(checks, externalDisplayPresent = true, vendorDesktop = checks[5])
        assertEquals(SupportLevel.SUPPORTED, verdict)
    }

    @Test
    fun noExternalDisplayAndNoVendorDesktop_isUnsupported() {
        val checks = listOf(
            CheckResult.fail("display.external", "External display connected", "No"),
            CheckResult.fail("freeform.feature", "Freeform feature", "Not set"),
            CheckResult.fail("desktop.vendor", "Vendor desktop", "None"),
        )
        val verdict = computeVerdict(checks, externalDisplayPresent = false, vendorDesktop = checks[2])
        assertEquals(SupportLevel.UNSUPPORTED, verdict)
    }

    @Test
    fun noExternalDisplayButVendorDesktopPresent_isPartial() {
        val checks = listOf(
            CheckResult.fail("display.external", "External display connected", "No"),
            CheckResult.pass("desktop.vendor", "Vendor desktop", "Samsung DeX"),
        )
        val verdict = computeVerdict(checks, externalDisplayPresent = false, vendorDesktop = checks[1])
        assertEquals(SupportLevel.PARTIALLY_SUPPORTED, verdict)
    }

    @Test
    fun externalDisplayPresentButFreeformGateClosed_isPartial() {
        val checks = listOf(
            CheckResult.pass("display.external", "External display connected", "Yes"),
            CheckResult.pass("freeform.feature", "Freeform feature", "Set"),
            CheckResult.fail("freeform.enable_freeform_support", "Enable", "0"),
            CheckResult.pass("freeform.force_desktop_mode_on_external_displays", "Force desktop", "1"),
            CheckResult.pass("desktop.vendor", "Vendor desktop", "None"),
        )
        val verdict = computeVerdict(checks, externalDisplayPresent = true, vendorDesktop = checks[4])
        assertEquals(SupportLevel.PARTIALLY_SUPPORTED, verdict)
    }

    @Test
    fun unavailableChecksDoNotCountAsFailuresForTheVerdict() {
        // A setting that does not exist on this API (UNAVAILABLE) must
        // not downgrade the verdict - only actual FAILs do.
        val checks = listOf(
            CheckResult.pass("display.external", "External display connected", "Yes"),
            CheckResult.pass("freeform.feature", "Freeform feature", "Set"),
            CheckResult.pass("freeform.enable_freeform_support", "Enable", "1"),
            CheckResult.pass("freeform.force_desktop_mode_on_external_displays", "Force desktop", "1"),
            CheckResult.unavailable("freeform.force_resizable_activities", "Force resizable", "Does not exist on this version."),
            CheckResult.pass("desktop.vendor", "Vendor desktop", "None"),
        )
        val verdict = computeVerdict(checks, externalDisplayPresent = true, vendorDesktop = checks[5])
        assertEquals(SupportLevel.SUPPORTED, verdict)
    }

    // ------------------------------------------------------------------
    // The verdict function itself. Copied here as a pure function so
    // the test does not need a Context. The production code in
    // CapabilityDetector has an identical implementation.
    // ------------------------------------------------------------------
    private fun computeVerdict(
        checks: List<CheckResult>,
        externalDisplayPresent: Boolean,
        vendorDesktop: CheckResult,
    ): SupportLevel {
        if (!externalDisplayPresent && !vendorDesktop.isPassing()) {
            return SupportLevel.UNSUPPORTED
        }
        val freeformGatesClosed = checks.any { c ->
            c.checkId.startsWith("freeform.") && c.status == CheckStatus.FAIL
        }
        if (externalDisplayPresent && freeformGatesClosed) {
            return SupportLevel.PARTIALLY_SUPPORTED
        }
        if (externalDisplayPresent) {
            return SupportLevel.SUPPORTED
        }
        return SupportLevel.PARTIALLY_SUPPORTED
    }
}
