import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/** `alpha.android.library`: shared settings for Android `:core:*` and `:feature:*` modules. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("com.android.library")

        extensions.configure(LibraryExtension::class.java) {
            compileSdk = libs.intVersion("compileSdk")
            defaultConfig {
                minSdk = libs.intVersion("minSdk")
            }
            compileOptions {
                sourceCompatibility = JAVA_VERSION
                targetCompatibility = JAVA_VERSION
            }
            lint {
                abortOnError = true
                checkReleaseBuilds = false
            }
        }
        configureKotlinJvmTarget()
        enforceDependencyRules()
    }
}
