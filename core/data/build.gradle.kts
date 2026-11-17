import java.util.Properties

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

// The opt-in live check (LiveCapabilityCheck) calls the real providers and spends requests
// from the daily free allowance. It runs only with -PliveAi; CI never sets it.
//   ./gradlew :core:data:testDebugUnitTest -PliveAi -Pcapability=grammar [-Pcompare]
tasks.withType<Test>().configureEach {
    if (providers.gradleProperty("liveAi").isPresent) {
        val localProperties = Properties()
        val localFile = rootProject.file("local.properties")
        if (localFile.isFile) localFile.inputStream().use { localProperties.load(it) }
        val key = providers.gradleProperty("OPENROUTER_API_KEY")
            .orElse(providers.environmentVariable("OPENROUTER_API_KEY"))
            .orNull ?: localProperties.getProperty("OPENROUTER_API_KEY").orEmpty()

        // The key goes in the environment, which is not written to test reports.
        environment("OPENROUTER_API_KEY", key)
        systemProperty("liveAi", "true")
        systemProperty("liveAi.capability", providers.gradleProperty("capability").orNull.orEmpty())
        systemProperty("liveAi.compare", providers.gradleProperty("compare").isPresent)
        systemProperty("liveAi.reportDir", layout.buildDirectory.dir("reports/ai-live").get().asFile.path)
        testLogging.showStandardStreams = true
        outputs.upToDateWhen { false }
    }
}
