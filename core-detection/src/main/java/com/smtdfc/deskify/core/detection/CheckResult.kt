package com.smtdfc.deskify.core.detection

/**
 * Per-check result used throughout the diagnostics report.
 *
 * A check can be:
 *  - [PASS]          the capability is present and usable.
 *  - [FAIL]          the capability is absent (the reason states why).
 *  - [UNAVAILABLE]   the check could not be executed (e.g. a reflection
 *                    target missing on this Android version). The check
 *                    itself is *not* a capability failure - the caller
 *                    must treat it as "unknown" when scoring.
 *
 * [reason] is a human-readable, non-localized detail string. All UI-facing
 * text lives in the app module; the core module stays free of resources so
 * it can be unit-tested on the JVM without a resource table.
 */
data class CheckResult(
    val checkId: String,
    val label: String,
    val status: CheckStatus,
    val detail: String,
) {
    /** Whether this result counts as "the capability works". */
    fun isPassing(): Boolean = status == CheckStatus.PASS

    companion object {
        fun pass(checkId: String, label: String, detail: String = "") =
            CheckResult(checkId, label, CheckStatus.PASS, detail)

        fun fail(checkId: String, label: String, detail: String) =
            CheckResult(checkId, label, CheckStatus.FAIL, detail)

        fun unavailable(checkId: String, label: String, detail: String) =
            CheckResult(checkId, label, CheckStatus.UNAVAILABLE, detail)
    }
}

/**
 * Tri-state outcome for a single check.
 */
enum class CheckStatus { PASS, FAIL, UNAVAILABLE }
