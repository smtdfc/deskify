package com.smtdfc.deskify.core.detection.oem

import android.content.Context

/**
 * OEM strategy interface (requirement 6 of the design: "handle OEM
 * differences with a strategy pattern").
 *
 * Every vendor hook that the detector might need is a method on this
 * interface so that adding a new vendor = adding one new [OemStrategy]
 * class, never touching the detector. The default [GenericOemStrategy]
 * implements all methods as "unknown/unsupported" so a new OEM that
 * has not been profiled yet degrades gracefully.
 */
interface OemStrategy {

    /** Short human-readable vendor name, e.g. "Samsung". */
    fun displayName(): String

    /**
     * Whether the OEM ships a *native* desktop mode that the user can
     * enable in its own settings (Samsung DeX, Xiaomi MeeGo, etc.).
     *
     * Returning `true` here means: the device has a vendor desktop
     * experience we should tell the user about, and the "system can
     * do desktop natively" check should count it as a PASS.
     */
    fun hasNativeDesktopMode(context: Context): Boolean

    /**
     * The package name of the vendor desktop-mode component, if any.
     * Used by the detector to report which component was found.
     *
     * Returning `null` means "no vendor desktop component".
     */
    fun desktopComponentPackage(context: Context): String? = null

    /**
     * Vendor-specific quirks that affect which of the global settings
     * keys are meaningful. e.g. some ROMs gate the freeform window
     * setting behind a different key name. The detector consults this
     * before reading the settings, so a vendor can say "I use
     * `my_freeform_key` instead of `enable_freeform_support`".
     *
     * The returned list is in priority order; the first non-null value
     * wins. An empty list means "use the stock AOSP key".
     */
    fun freeformSettingKeyOverrides(context: Context): List<String> = emptyList()
}

/**
 * Picks the right [OemStrategy] for the running device.
 *
 * The selection is a simple `Manufacturer` switch. The mapping is
 * intentionally small and conservative: we only special-case vendors
 * that have a *documented, user-visible* desktop mode. Everything else
 * falls through to [GenericOemStrategy] which reports "unknown".
 *
 * NOTE: `Manufacturer` is the `Build.MANUFACTURER` string, which OEMs
 * set themselves. It is not a reliable vendor identifier for all
 * devices (e.g. a Huawei device may report `HUAWEI` or `HISILICON`
 * depending on the board). We treat it as a hint, not a truth - every
 * strategy method is still defensive and may return `null`/`false`.
 */
object OemStrategySelector {

    fun forContext(context: Context): OemStrategy {
        val manufacturer = android.os.Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("samsung") -> SamsungOemStrategy()
            manufacturer.contains("xiaomi") || manufacturer.contains("mi") ->
                XiaomiOemStrategy()
            manufacturer.contains("huawei") || manufacturer.contains("hisilicon") ->
                HuaweiOemStrategy()
            manufacturer.contains("google") -> GoogleOemStrategy()
            else -> GenericOemStrategy()
        }
    }

    /**
     * Exposed for unit tests: lets a test force a specific strategy
     * without depending on `Build.MANUFACTURER` (which is fixed at
     * build time on the test host).
     */
    fun override(known: OemStrategy): OemStrategy = known
}
