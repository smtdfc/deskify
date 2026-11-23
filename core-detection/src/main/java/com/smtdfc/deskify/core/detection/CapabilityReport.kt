package com.smtdfc.deskify.core.detection

/**
 * Structured, serializable result of a capability-detection pass.
 *
 * The report is the single source of truth for the diagnostics UI: it
 * contains both the individual [CheckResult]s and the [SupportLevel]
 * verdict. The [oem] and [androidSdk] fields let the UI annotate which
 * device/version the report was produced on (important because OEM
 * behaviour differs).
 */
data class CapabilityReport(
    val androidSdk: Int,
    val oem: String,
    val externalDisplayConnected: Boolean,
    val checks: List<CheckResult>,
    val verdict: SupportLevel,
) {
    /** Convenience access to all FAIL/UNAVAILABLE checks (for "why" panels). */
    val failures: List<CheckResult>
        get() = checks.filter { !it.isPassing() }

    /** Convenience access to all PASS checks. */
    val passes: List<CheckResult>
        get() = checks.filter { it.isPassing() }
}
