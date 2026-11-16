plugins {
    id("alpha.android.library")
    id("alpha.hilt")
}

android {
    namespace = "com.example.alpha_ai.core.network"
}

dependencies {
    implementation(projects.core.common)
}
