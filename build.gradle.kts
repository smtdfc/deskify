/*
 * Root project: declares the shared plugin classpath.
 *
 * Per-module build files (app, core-detection, ...) apply the
 * plugins with their own options.
 *
 * The plugin versions come from the version catalog
 * (gradle/libs.versions.toml), which is available in every
 * build script via the `libs.plugins.*` accessors.
 */
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
