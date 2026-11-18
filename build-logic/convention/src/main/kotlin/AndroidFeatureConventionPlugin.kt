import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `alpha.android.feature`: everything a `:feature:*` module needs. An Android library with
 * Compose, Hilt, and Kotlin serialization (for type-safe routes), plus the shared core modules,
 * navigation, lifecycle-aware state collection, and the test libraries.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("alpha.android.library")
        pluginManager.apply("alpha.android.compose")
        pluginManager.apply("alpha.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        dependencies.run {
            add("implementation", project(mapOf("path" to ":core:common")))
            add("implementation", project(mapOf("path" to ":core:domain")))
            add("implementation", project(mapOf("path" to ":core:designsystem")))
            add("implementation", project(mapOf("path" to ":core:ui")))

            add("implementation", libs.library("androidx-navigation-compose"))
            add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("kotlinx-serialization-json"))
            add("implementation", libs.library("timber"))

            add("testImplementation", project(mapOf("path" to ":core:testing")))

            add("androidTestImplementation", platform(libs.library("androidx-compose-bom")))
            add("androidTestImplementation", libs.library("androidx-compose-ui-test-junit4"))
            add("androidTestImplementation", libs.library("androidx-test-runner"))
            add("androidTestImplementation", libs.library("androidx-test-junit"))
            add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
        }
    }
}
