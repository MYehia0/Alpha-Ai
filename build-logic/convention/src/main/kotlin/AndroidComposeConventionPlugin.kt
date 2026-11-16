import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `alpha.android.compose`: enables Jetpack Compose in a module that already applies
 * `alpha.android.application` or `alpha.android.library`.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.findByType(ApplicationExtension::class.java)?.buildFeatures?.compose = true
        extensions.findByType(LibraryExtension::class.java)?.buildFeatures?.compose = true

        dependencies.run {
            add("implementation", platform(libs.library("androidx-compose-bom")))
            add("implementation", libs.library("androidx-compose-material3"))
            add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
            add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
        }
    }
}
