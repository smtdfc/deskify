plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.smtdfc.deskify.core.detection"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
    }

    lint {
        abortOnError = false
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    // Pure library module: no AndroidX UI dependencies. This keeps the
    // detection layer reusable (and unit-testable on the JVM via
    // Robolectric) independent of the Compose UI stack in the app
    // module.
    api(libs.androidx.core.ktx)

    testImplementation(libs.junit4)
}
