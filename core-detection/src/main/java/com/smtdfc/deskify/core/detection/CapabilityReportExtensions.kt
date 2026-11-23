package com.smtdfc.deskify.core.detection

import com.smtdfc.deskify.core.detection.CheckResult
import com.smtdfc.deskify.core.detection.SupportLevel
import com.smtdfc.deskify.core.detection.CapabilityReport

/**
 * Convenience accessor for the *display-external* check.
 *
 * The detector returns a flat list of [CheckResult]s; this helper
 * finds the one with [CheckResult.checkId] equal to `"display.external"`
 * and returns it. Useful for the diagnostics UI's top-level banner.
 */
fun CapabilityReport.displayExternalCheck(): CheckResult =
    checks.firstOrNull { it.checkId == "display.external" }
        ?: CheckResult.unavailable(
            checkId = "display.external",
            label = "External display connected",
            detail = "The detector did not produce a display-external check.",
        )

/**
 * Convenience accessor for the *permission* check.
 */
fun CapabilityReport.permissionCheck(): CheckResult =
    checks.firstOrNull { it.checkId == "permission.write_secure_settings" }
        ?: CheckResult.unavailable(
            checkId = "permission.write_secure_settings",
            label = "WRITE_SECURE_SETTINGS held by app",
            detail = "The detector did not produce a permission check.",
        )

/**
 * Convenience accessor for the *verdict* as a short display string.
 * The diagnostics UI renders this in its banner.
 */
fun CapabilityReport.verdictLabel(): String = when (verdict) {
    SupportLevel.SUPPORTED -> "SUPPORTED"
    SupportLevel.PARTIALLY_SUPPORTED -> "PARTIALLY SUPPORTED"
    SupportLevel.UNSUPPORTED -> "UNSUPPORTED"
}
