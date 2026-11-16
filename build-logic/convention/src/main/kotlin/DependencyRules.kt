import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

private const val RULES_DOC = "See specs/001-build-foundation/data-model.md."

/**
 * Returns the message for the module dependency rule that [from] -> [to] breaks, or null when
 * the dependency is allowed. Paths are Gradle project paths such as `:core:data`.
 */
internal fun dependencyRuleViolation(from: String, to: String): String? {
    fun violation(rule: String, explanation: String) =
        "Dependency rule $rule violated: '$from' must not depend on '$to'.\n$explanation $RULES_DOC"

    val fromCore = from.startsWith(":core:")
    val fromFeature = from.startsWith(":feature:")
    val toFeature = to.startsWith(":feature:")

    return when {
        from == ":core:domain" && to != ":core:common" ->
            violation("DR-3", "':core:domain' may depend only on ':core:common'.")

        fromCore && (to == ":app" || toFeature) ->
            violation("DR-1", "Core modules must not depend on the app or on feature modules.")

        fromFeature && toFeature && from != to ->
            violation("DR-2", "A feature module must not depend on another feature module.")

        else -> null
    }
}

/** Fails the build as soon as this project declares a project dependency that breaks a rule. */
internal fun Project.enforceDependencyRules() {
    val from = path
    configurations.configureEach {
        dependencies.withType(ProjectDependency::class.java).configureEach {
            dependencyRuleViolation(from, path)?.let { throw GradleException(it) }
        }
    }
}

/** Applies only the dependency rules. The library convention plugins apply the same check. */
class DependencyRulesPlugin : Plugin<Project> {
    override fun apply(target: Project) = target.enforceDependencyRules()
}
