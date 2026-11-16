plugins {
    id("alpha.android.library")
}

android {
    namespace = "com.example.alpha_ai.core.testing"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    api(libs.junit)
    api(libs.mockk)
    api(libs.turbine)
    api(libs.kotlinx.coroutines.test)
}
