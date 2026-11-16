import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

internal val JAVA_VERSION = JavaVersion.VERSION_17

internal val Project.libs: VersionCatalog
    get() = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

internal fun VersionCatalog.intVersion(name: String): Int =
    findVersion(name).get().requiredVersion.toInt()

internal fun VersionCatalog.library(name: String) = findLibrary(name).get()

internal fun Project.configureKotlinJvmTarget() {
    tasks.withType(KotlinJvmCompile::class.java).configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
}
