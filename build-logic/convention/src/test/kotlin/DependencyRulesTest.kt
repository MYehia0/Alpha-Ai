import java.io.File
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Builds a throwaway multi-project fixture and checks that each module dependency rule
 * rejects a violation and that an allowed graph is accepted.
 */
class DependencyRulesTest {

    @get:Rule
    val tempDir = TemporaryFolder()

    private val modules = listOf(
        ":app", ":core:common", ":core:domain", ":core:data", ":feature:one", ":feature:two",
    )

    /** Creates the fixture; [dependencies] maps a module path to the module paths it uses. */
    private fun fixture(dependencies: Map<String, List<String>>): File {
        val root = tempDir.root
        File(root, "settings.gradle.kts").writeText(
            "rootProject.name = \"fixture\"\n" +
                modules.joinToString("\n") { "include(\"$it\")" },
        )
        modules.forEach { module ->
            val dir = File(root, module.removePrefix(":").replace(':', '/')).apply { mkdirs() }
            val deps = dependencies[module].orEmpty()
                .joinToString("\n") { "    implementation(project(\"$it\"))" }
            File(dir, "build.gradle.kts").writeText(
                """
                plugins {
                    `java-library`
                    id("alpha.dependency.rules")
                }
                dependencies {
                $deps
                }
                """.trimIndent(),
            )
        }
        return root
    }

    private fun runner(root: File) = GradleRunner.create()
        .withProjectDir(root)
        .withPluginClasspath()
        .withArguments("projects", "--stacktrace")

    private fun assertRejected(result: BuildResult, rule: String, from: String, to: String) {
        val expected = "Dependency rule $rule violated: '$from' must not depend on '$to'."
        assertTrue("Expected \"$expected\" in:\n${result.output}", expected in result.output)
    }

    @Test
    fun `DR-1 rejects a core module depending on the app`() {
        val root = fixture(mapOf(":core:data" to listOf(":app")))
        assertRejected(runner(root).buildAndFail(), "DR-1", ":core:data", ":app")
    }

    @Test
    fun `DR-1 rejects a core module depending on a feature module`() {
        val root = fixture(mapOf(":core:data" to listOf(":feature:one")))
        assertRejected(runner(root).buildAndFail(), "DR-1", ":core:data", ":feature:one")
    }

    @Test
    fun `DR-2 rejects a feature module depending on another feature module`() {
        val root = fixture(mapOf(":feature:one" to listOf(":feature:two")))
        assertRejected(runner(root).buildAndFail(), "DR-2", ":feature:one", ":feature:two")
    }

    @Test
    fun `DR-3 rejects the domain module depending on anything but common`() {
        val root = fixture(mapOf(":core:domain" to listOf(":core:data")))
        assertRejected(runner(root).buildAndFail(), "DR-3", ":core:domain", ":core:data")
    }

    @Test
    fun `an allowed graph configures successfully`() {
        val root = fixture(
            mapOf(
                ":core:domain" to listOf(":core:common"),
                ":core:data" to listOf(":core:common", ":core:domain"),
                ":feature:one" to listOf(":core:common", ":core:domain"),
                ":app" to listOf(":core:data", ":feature:one", ":feature:two"),
            ),
        )
        val result = runner(root).build()
        assertTrue(result.output, "BUILD SUCCESSFUL" in result.output)
    }
}
