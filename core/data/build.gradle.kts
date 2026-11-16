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
}
