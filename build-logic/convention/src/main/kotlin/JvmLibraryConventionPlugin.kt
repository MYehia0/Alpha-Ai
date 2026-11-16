import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension

/**
 * `alpha.jvm.library`: a pure Kotlin module. It has no Android, Compose, Retrofit, or Firebase
 * on its classpath, which is what keeps `:core:common` and `:core:domain` platform-free.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")

        extensions.configure(JavaPluginExtension::class.java) {
            sourceCompatibility = JAVA_VERSION
            targetCompatibility = JAVA_VERSION
        }
        configureKotlinJvmTarget()
        enforceDependencyRules()
    }
}
