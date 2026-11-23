/*
 * Repository and plugin resolution for the whole build.
 *
 * AGP 9.5.0 (stable, Oct 2026) + Kotlin 2.4.0 (stable) pair with
 * Gradle >= 9.6 and compileSdk 36 / API level 37 support.
 */
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Deskify"
include(":app")
include(":core-detection")
