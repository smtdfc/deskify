package com.smtdfc.deskify.core.detection.oem

import android.content.Context

/**
 * The default / unknown-vendor strategy.
 *
 * Every method returns the "no opinion" value. This is the fallback for
 * any `Build.MANUFACTURER` we have not explicitly profiled, and it is
 * also the *safe* behaviour: a strategy that claims a capability the
 * OEM does not actually ship would be a false positive, which the
 * detector would report to the user. Returning "unknown" keeps the
 * diagnostics honest.
 */
class GenericOemStrategy : OemStrategy {

    override fun displayName(): String = "Generic (unknown OEM)"

    override fun hasNativeDesktopMode(context: Context): Boolean = false

    override fun desktopComponentPackage(context: Context): String? = null

    override fun freeformSettingKeyOverrides(context: Context): List<String> = emptyList()
}
