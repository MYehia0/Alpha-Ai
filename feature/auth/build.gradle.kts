plugins {
    id("alpha.android.feature")
}

android {
    namespace = "com.example.alpha_ai.feature.auth"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    lint {
        // Compose text must come from string resources (constitution IV).
        error += "HardcodedText"
        // SignInReason (a nav argument from the JVM :core:domain, which has no @Keep) is kept by
        // consumer-rules.pro instead; lint does not read keep rules.
        disable += "MissingKeepAnnotation"
    }
}

dependencies {
    // The introduction illustrations (Icons.Filled.Edit, Build, DateRange).
    implementation(libs.androidx.compose.material.icons.core)
}
