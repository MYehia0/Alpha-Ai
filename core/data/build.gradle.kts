plugins {
    id("alpha.android.library")
    id("alpha.hilt")
}

android {
    namespace = "com.example.alpha_ai.core.data"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.network)
    implementation(projects.core.datastore)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.timber)

    testImplementation(projects.core.testing)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
