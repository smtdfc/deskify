package com.smtdfc.deskify.core.detection.oem

import android.content.Context

/**
 * Google Pixel / AOSP strategy.
 *
 * AOSP has no vendor "desktop mode" package; the only desktop
 * capability is the freeform window manager gated behind the
 * `enable_freeform_support` / `force_resizable_activities` global
 * settings. We therefore report `hasNativeDesktopMode = false` here
 * and rely on the generic freeform detectors in [CapabilityDetector].
 *
 * NOTE: Android 14+ adds a native desktop windowing mode for
 * *external displays* (no ADB grant needed on developer-preview
 * builds, and a Settings toggle on some OEMs). That is checked by the
 * [CapabilityDetector]'s Android-14+ check, not by this strategy -
 * this strategy only answers "does this vendor ship a DeX-like
 * package?".
 */
class GoogleOemStrategy : OemStrategy {

    override fun displayName(): String = "Google / AOSP"

    override fun hasNativeDesktopMode(context: Context): Boolean = false

    override fun desktopComponentPackage(context: Context): String? = null

    override fun freeformSettingKeyOverrides(context: Context): List<String> = emptyList()
}
