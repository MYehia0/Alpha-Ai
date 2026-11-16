plugins {
    id("alpha.jvm.library")
}

dependencies {
    implementation(projects.core.common)
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
