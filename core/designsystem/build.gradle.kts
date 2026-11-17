plugins {
    id("alpha.android.library")
    id("alpha.android.compose")
}

android {
    namespace = "com.example.alpha_ai.core.designsystem"

    lint {
        // Compose text must come from string resources (constitution IV).
        error += "HardcodedText"
    }
}

dependencies {
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.junit)
}
