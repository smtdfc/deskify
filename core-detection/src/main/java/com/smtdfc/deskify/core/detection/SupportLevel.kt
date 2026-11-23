package com.smtdfc.deskify.core.detection

/**
 * Overall verdict returned by [CapabilityDetector].
 *
 * The enum is deliberately small; the *reasons* for each check live in
 * [CapabilityReport]. Consumers should never derive "why" from the enum
 * alone - they must render the report.
 */
enum class SupportLevel {
    /**
     * Every mandatory capability passed. The app can offer the full
     * desktop experience (or the system can be switched into native
     * desktop mode).
     */
    SUPPORTED,

    /**
     * At least one mandatory capability failed but the app can still
     * run in a degraded mode (e.g. no freeform windows, but a
     * secondary display exists - the "launcher-managed desktop"
     * fallback).
     */
    PARTIALLY_SUPPORTED,

    /**
     * No usable secondary display and no system desktop mode. The app
     * cannot provide any meaningful desktop experience on this device.
     */
    UNSUPPORTED,
}
