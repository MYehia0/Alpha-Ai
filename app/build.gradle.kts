import java.util.Properties
import java.io.FileInputStream

plugins {
    id("alpha.android.application")
    id("alpha.android.compose")
    id("alpha.hilt")
    // Data Binding has no KSP processor, so kapt stays in this module only.
    alias(libs.plugins.android.legacy.kapt)
    alias(libs.plugins.google.services)
}

// Secrets never live in tracked files. HF_API_TOKEN is resolved from, in order: a Gradle
// property, an environment variable, then the git-ignored local.properties.
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        FileInputStream(localPropertiesFile).use { load(it) }
    }
}
val hfApiToken: String = providers.gradleProperty("HF_API_TOKEN")
    .orElse(providers.environmentVariable("HF_API_TOKEN"))
    .orNull
    ?: localProperties.getProperty("HF_API_TOKEN", "")

if (hfApiToken.isBlank()) {
    tasks.named("preBuild") {
        doFirst {
            logger.warn(
                "HF_API_TOKEN is not set. Add it to local.properties " +
                    "(see local.properties.example). AI features will fail at run time."
            )
        }
    }
}

android {
    namespace = "com.example.alpha_ai"

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.alpha_ai"
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "HF_API_TOKEN", "\"$hfApiToken\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        // Existing findings are recorded in the baseline; only new ones fail the build.
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.security.crypto)

    // UI
    implementation(libs.material)
    implementation(libs.material.progressbar)
    implementation(libs.swipe.layout)
    implementation(libs.glide)

    // Networking
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.ui.auth)

    // Document parsing
    implementation(libs.poi.core)
    implementation(libs.poi.ooxml)
    implementation(libs.poi.scratchpad)

    // Logging
    implementation(libs.timber)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

// kapt remains only for Data Binding, which has no KSP processor.
kapt {
    correctErrorTypes = true
}
