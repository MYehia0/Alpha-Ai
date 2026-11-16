// Top-level build file. Plugins are declared here and applied in each module.
buildscript {
    dependencies {
        // AGP's built-in Kotlin uses the Kotlin Gradle plugin version found on this classpath.
        // Pinning it here keeps the Kotlin version in the version catalog.
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.legacy.kapt) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
