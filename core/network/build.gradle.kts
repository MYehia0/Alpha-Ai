plugins {
    id("alpha.android.library")
    id("alpha.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.alpha_ai.core.network"
}

dependencies {
    implementation(projects.core.common)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.timber)
    // HTTP logging exists in debug builds only, so a release build cannot log requests.
    debugImplementation(libs.okhttp.logging)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
