package com.smtdfc.deskify.core.detection

import android.content.Context

/**
 * Reads a single [android.provider.Settings.Global] row.
 *
 * Returns `null` when the row does not exist. Global settings are *readable
 * by any app* without special permissions; writing requires
 * `WRITE_SECURE_SETTINGS`, which is a separate concern handled by the
 * settings module.
 *
 * This is isolated from the detectors so that unit tests can substitute a
 * fake without spinning up Robolectric.
 */
object SettingsReader {

    /**
     * Reads `key` from `Settings.Global`. Never throws: a missing key is
     * reported as `null`, a SecurityException (rare on read) is swallowed.
     */
    fun globalString(context: Context, key: String): String? = try {
        android.provider.Settings.Global.getString(
            context.contentResolver,
            key,
        )
    } catch (_: Exception) {
        // Global.getString does not take a default; a missing key simply
        // returns null. Catch defensively in case an OEM ROM throws.
        null
    }

    /**
     * Same, but with an explicit default so callers can express
     * "absent = false" semantics without a `?: false` at every call site.
     */
    fun globalBoolean(
        context: Context,
        key: String,
        defaultValue: Boolean = false,
    ): Boolean {
        val raw = globalString(context, key) ?: return defaultValue
        return when (raw.trim().lowercase()) {
            "true", "1" -> true
            "false", "0" -> false
            // Unknown value: fall back to the caller-provided default.
            else -> defaultValue
        }
    }

    /**
     * Reads a global setting as an Int. `null` when the key does not exist.
     */
    fun globalInt(
        context: Context,
        key: String,
        defaultValue: Int? = null,
    ): Int? {
        val raw = globalString(context, key) ?: return defaultValue
        return raw.toIntOrNull()
    }
}
