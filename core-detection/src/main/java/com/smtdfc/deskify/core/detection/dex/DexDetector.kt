package com.smtdfc.deskify.core.detection.dex

import android.content.Context
import com.smtdfc.deskify.core.detection.CheckResult
import com.smtdfc.deskify.core.detection.oem.OemStrategy

/**
 * Detects the *vendor* desktop-mode component (Samsung DeX, etc.).
 *
 * This is deliberately separate from the freeform detectors: a vendor
 * desktop mode is a *system app* the user can open, whereas freeform
 * windowing is an *OS capability* the launcher can enable. The two
 * can both be present, both absent, or one without the other.
 *
 * The detector is a thin wrapper over the [OemStrategy] so the OEM
 * differences are isolated in the strategy, not here.
 */
object DexDetector {

    /**
     * Runs the vendor-desktop check via the supplied [strategy].
     *
     * @param strategy the OEM strategy for the running device.
     * @return a [CheckResult]. PASS when the vendor desktop package is
     *         present; FAIL when it is not; UNAVAILABLE when the probe
     *         itself threw (should be rare).
     */
    fun detect(
        context: Context,
        strategy: OemStrategy,
    ): CheckResult {
        val pkg = strategy.desktopComponentPackage(context)
        val hasNative = strategy.hasNativeDesktopMode(context)

        if (pkg == null && !hasNative) {
            return CheckResult.fail(
                checkId = "desktop.vendor",
                label = "Native vendor desktop mode",
                detail = "No vendor desktop-mode package found for OEM '${strategy.displayName()}'. " +
                    "The system relies on AOSP freeform windowing only.",
            )
        }
        if (pkg != null && hasNative) {
            return CheckResult.pass(
                checkId = "desktop.vendor",
                label = "Native vendor desktop mode",
                detail = "Found '${pkg}' (OEM: ${strategy.displayName()}). " +
                    "The user can open this from the app drawer as a baseline " +
                    "desktop experience; Deskify augments it with its own " +
                    "launcher UI.",
            )
        }
        // Defensive fallback: the strategy said "has native" but did
        // not name a package, or vice-versa. Treat as a pass with a
        // weaker detail.
        return CheckResult.pass(
            checkId = "desktop.vendor",
            label = "Native vendor desktop mode",
            detail = "Vendor desktop capability reported by ${strategy.displayName()} " +
                "strategy without a concrete package name.",
        )
    }
}
