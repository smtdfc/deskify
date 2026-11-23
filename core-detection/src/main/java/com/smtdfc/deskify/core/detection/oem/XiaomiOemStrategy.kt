package com.smtdfc.deskify.core.detection.oem

import android.content.Context

/**
 * Xiaomi / Redmi / POCO strategy.
 *
 * Xiaomi HyperOS / MIUI does *not* ship a user-facing desktop mode
 * (no public DeX equivalent). The closest thing is "PC mode" on some
 * tablets, which is a UI reskin, not a freeform window manager. We
 * therefore report `hasNativeDesktopMode = false` and rely on the
 * standard AOSP freeform settings.
 *
 * Keeping the class around (instead of just falling through to
 * [GenericOemStrategy]) is deliberate: it documents the decision, and
 * gives us a single place to add a key override if a future MIUI
 * ships a desktop mode.
 */
class XiaomiOemStrategy : OemStrategy {

    override fun displayName(): String = "Xiaomi"

    override fun hasNativeDesktopMode(context: Context): Boolean = false

    override fun desktopComponentPackage(context: Context): String? = null

    override fun freeformSettingKeyOverrides(context: Context): List<String> = emptyList()
}
