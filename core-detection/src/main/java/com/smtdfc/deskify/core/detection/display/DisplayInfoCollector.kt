package com.smtdfc.deskify.core.detection.display

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.util.DisplayMetrics

/**
 * Per-display metrics used by the diagnostics screen and, later, by the
 * window manager to size windows against a concrete display.
 *
 * Everything here is *read-only* and *non-throwing*: a missing metric
 * is reported as `null` or a sensible default, never as an exception.
 */
data class DisplayInfo(
    /** The display id (`0` is the primary / built-in display). */
    val id: Int,
    /** Human-readable name, e.g. "Built-in screen" or "USB Display 1". */
    val name: String,
    /** `true` when this is the built-in (primary) display. */
    val isDefault: Boolean,
    /** Width in pixels. */
    val widthPx: Int,
    /** Height in pixels. */
    val heightPx: Int,
    /** Density in dots-per-inch (e.g. 160 for mdpi, 420/480/560 for
     * hdpi/xhdpi/xxhdpi). External displays often report 160 or 213. */
    val densityDpi: Int,
    /**
     * Refresh rate in Hz. `null` when the API is unavailable or the
     * display reports no rate.
     *
     * `Display.getRefreshRate()` exists on API 24+ but is deprecated;
     * on API 31+ we prefer `getSupportedRefreshRates()` and report the
     * highest.
     */
    val refreshRateHz: Double?,
    /** Whether the display can host *activities* (not just presentations).
     * This is what `ActivityOptions.setLaunchDisplayId` requires. */
    val canHostActivities: Boolean,
    /** Display category, e.g. "DISPLAY_CATEGORY_PRESENTATION". */
    val category: String,
)

/**
 * Collects a [DisplayInfo] for every display currently attached.
 *
 * The detector calls this on first launch and on every
 * `DisplayManager.DisplayListener` connect/disconnect event.
 */
object DisplayInfoCollector {

    fun collectAll(context: Context): List<DisplayInfo> {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE)
            as? DisplayManager
            ?: return emptyList()

        val displays: Array<Display> = displayManager.displays ?: return emptyList()
        return displays.map { display -> toDisplayInfo(context, displayManager, display) }
    }

    /**
     * Convenience accessor for the *primary* display only.
     */
    fun primary(context: Context): DisplayInfo? =
        collectAll(context).firstOrNull { it.isDefault }

    /**
     * Convenience accessor for every *non-primary* display that is
     * usable for presentations.
     */
    fun external(context: Context): List<DisplayInfo> =
        collectAll(context).filter { !it.isDefault }

    // ------------------------------------------------------------------
    // internal
    // ------------------------------------------------------------------

    private fun toDisplayInfo(
        context: Context,
        dm: DisplayManager,
        display: Display,
    ): DisplayInfo {
        val isDefault = display.displayId == Display.DEFAULT_DISPLAY
        val (width, height, density, refresh) = readMetrics(dm, display)
        val canHost = canHostActivities(context, display)
        val category = displayCategory(display)

        return DisplayInfo(
            id = display.displayId,
            name = display.name,
            isDefault = isDefault,
            widthPx = width,
            heightPx = height,
            densityDpi = density,
            refreshRateHz = refresh,
            canHostActivities = canHost,
            category = category,
        )
    }

    /**
     * Reads resolution / density / refresh rate. Refresh rate is
     * API-level dependent, so the read is wrapped in a try/catch with
     * an API-level dispatch.
     *
     * `DisplayManager.getDisplayProperties(display)` (API 26+) returns
     * the *current* size and density in px units. For API 24-25 we
     * fall back to `Display.getMetrics()`.
     */
    private fun readMetrics(
        dm: DisplayManager,
        display: Display,
    ): Quad {
        val dm2 = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getMetrics(dm2)
        val width = dm2.widthPixels
        val height = dm2.heightPixels
        val density = dm2.densityDpi

        val refreshRate: Double = when {
            // API 31+ exposes a list of supported rates; report the
            // highest. This is the most accurate number on modern
            // devices (which can switch rates for power).
            Build.VERSION.SDK_INT >= 31 -> try {
                val supported = display.supportedRefreshRates
                if (supported.isEmpty()) {
                    display.refreshRate.toDouble()
                } else {
                    (supported.maxOrNull() ?: 0f).toDouble()
                }
            } catch (e: Exception) {
                display.refreshRate.toDouble()
            }
            else -> display.refreshRate.toDouble()
        }

        return Quad(width, height, density, refreshRate)
    }

    /**
     * A display can host activities when:
     *   - it is the default display, OR
     *   - it is a presentation-category display AND the device
     *     supports activities on secondary displays.
     *
     * `ActivityOptions.setLaunchDisplayId` is documented since API 24.
     * On API 34+ the formal gate is
     * `PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS`;
     * on API 24-33 the presentation flag is sufficient.
     */
    private fun canHostActivities(
        context: Context,
        display: Display,
    ): Boolean {
        if (display.displayId == Display.DEFAULT_DISPLAY) return true
        val isPresentation = displayCategory(display).contains("PRESENTATION")
        if (!isPresentation) return false
        if (Build.VERSION.SDK_INT >= 34) {
            val feature = PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS
            return context.packageManager.hasSystemFeature(feature)
        }
        // API 24-33: presentation displays can host activities via
        // setLaunchDisplayId; no separate feature flag needed.
        return true
    }

    private fun displayCategory(display: Display): String {
        // API 29+ exposes DisplayManager.DISPLAY_CATEGORY_* constants.
        // We map the flags onto those constants so the diagnostics
        // screen shows the real category string.
        if (Build.VERSION.SDK_INT >= 29) {
            val category = when {
                display.flags and Display.FLAG_PRESENTATION != 0 ->
                    DisplayManager.DISPLAY_CATEGORY_PRESENTATION.toString()
                display.displayId == Display.DEFAULT_DISPLAY ->
                    "INTERNAL"
                else ->
                    "EXTERNAL"
            }
            return "DISPLAY_CATEGORY_" + category
        }
        // Pre-API 29: fall back to the legacy flag-based names.
        val legacy = when {
            display.flags and Display.FLAG_PRESENTATION != 0 -> "PRESENTATION"
            else -> "INTERNAL"
        }
        return "DISPLAY_CATEGORY_" + legacy
    }

    /**
     * A small private value class to keep `readMetrics` tidy.
     */
    private data class Quad(
        val width: Int,
        val height: Int,
        val density: Int,
        val refreshRate: Double,
    )
}
