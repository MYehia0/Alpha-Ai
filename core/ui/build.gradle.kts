plugins {
    id("alpha.android.library")
    id("alpha.android.compose")
}

android {
    namespace = "com.example.alpha_ai.core.ui"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.designsystem)
}
