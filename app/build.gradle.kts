import java.util.Properties
import java.io.FileInputStream

plugins {
    id("alpha.android.application")
    id("alpha.android.compose")
    id("alpha.hilt")
    // Data Binding has no KSP processor, so kapt stays in this module only.
    alias(libs.plugins.android.legacy.kapt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.serialization)
}

// Secrets never live in tracked files. Each key is resolved from, in order: a Gradle
// property, an environment variable, then the git-ignored local.properties.
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        FileInputStream(localPropertiesFile).use { load(it) }
    }
}
fun secret(name: String): String = providers.gradleProperty(name)
    .orElse(providers.environmentVariable(name))
    .orNull
    ?: localProperties.getProperty(name, "")

val hfApiToken: String = secret("HF_API_TOKEN")
val openRouterApiKey: String = secret("OPENROUTER_API_KEY")

val missingKeys: List<String> =
    listOf("HF_API_TOKEN" to hfApiToken, "OPENROUTER_API_KEY" to openRouterApiKey)
        .filter { it.second.isBlank() }
        .map { it.first }

if (missingKeys.isNotEmpty()) {
    val missingKeysText = missingKeys.joinToString(" and ")
    tasks.named("preBuild") {
        doFirst {
            logger.warn(
                "$missingKeysText not set. Add to local.properties " +
                    "(see local.properties.example). AI features that need a missing key " +
                    "will fail at run time."
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
        buildConfigField("String", "OPENROUTER_API_KEY", "\"$openRouterApiKey\"")

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

    androidResources {
        // Lists English and Arabic in the system per-app language screen (Android 13+).
        generateLocaleConfig = true
    }

    lint {
        // Existing findings are recorded in the baseline; only new ones fail the build.
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    // Shared modules. The AI layer is reached through :core:domain; :core:data and
    // :core:network are here so Hilt can see their bindings.
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.network)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(projects.core.datastore)
    implementation(projects.feature.auth)

    // App shell (spec 003)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.splashscreen)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

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

    // Document parsing
    implementation(libs.poi.core)
    implementation(libs.poi.ooxml)
    implementation(libs.poi.scratchpad)

    // Logging
    implementation(libs.timber)

    // Testing
    testImplementation(libs.junit)
    testImplementation(projects.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

// kapt remains only for Data Binding, which has no KSP processor.
kapt {
    correctErrorTypes = true
}

// StringsParityTest reads resource files from every module.
tasks.withType<Test>().configureEach {
    systemProperty("rootDir", rootDir.path)
}
