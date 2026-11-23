package com.smtdfc.deskify.core.detection.oem

import android.content.Context

/**
 * Huawei / Honor strategy.
 *
 * EMUI / HarmonyOS does *not* expose a public desktop-mode API to
 * third-party apps. Huawei's "PC mode" on some tablets is a launcher
 * reskin. We report `hasNativeDesktopMode = false` and fall back to
 * the stock AOSP freeform settings.
 *
 * `Build.MANUFACTURER` for Huawei devices is typically "HUAWEI" or
 * "HISILICON" (the chip vendor); the selector matches both.
 */
class HuaweiOemStrategy : OemStrategy {

    override fun displayName(): String = "Huawei"

    override fun hasNativeDesktopMode(context: Context): Boolean = false

    override fun desktopComponentPackage(context: Context): String? = null

    override fun freeformSettingKeyOverrides(context: Context): List<String> = emptyList()
}
