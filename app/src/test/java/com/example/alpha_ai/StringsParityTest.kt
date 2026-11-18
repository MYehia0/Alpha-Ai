package com.example.alpha_ai

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Every translatable string has both an English and an Arabic version, in every module that has
 * strings (FR-009, SC-005). Strings marked `translatable="false"` are skipped.
 */
class StringsParityTest {

    private val root = File(System.getProperty("rootDir") ?: error("rootDir system property not set"))

    private val resourceDirs = listOf(
        "app/src/main/res",
        "app/src/debug/res",
        "core/designsystem/src/main/res",
        "core/ui/src/main/res",
        "feature/auth/src/main/res",
    )

    @Test
    fun `english and arabic hold the same strings in every module`() {
        val problems = resourceDirs.flatMap { dir ->
            val english = names(File(root, "$dir/values"), translatableOnly = true)
            val arabic = names(File(root, "$dir/values-ar"), translatableOnly = false)
            val untranslatable = names(File(root, "$dir/values"), translatableOnly = false) - english
            (english - arabic).map { "$dir: '$it' has no Arabic" } +
                (arabic - english - untranslatable).map { "$dir: '$it' is Arabic only" } +
                (arabic intersect untranslatable).map { "$dir: '$it' is marked untranslatable but has Arabic" }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun `every module checked has strings`() {
        resourceDirs.forEach { dir ->
            assertTrue("$dir has no English strings", names(File(root, "$dir/values"), false).isNotEmpty())
        }
    }

    private fun names(dir: File, translatableOnly: Boolean): Set<String> {
        val files = dir.listFiles { f -> f.name.startsWith("strings") && f.extension == "xml" }.orEmpty()
        return files.flatMap { file ->
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            listOf("string", "plurals", "string-array").flatMap { tag ->
                val nodes = doc.getElementsByTagName(tag)
                (0 until nodes.length).map { nodes.item(it) as Element }
            }
                .filter { !translatableOnly || it.getAttribute("translatable") != "false" }
                .map { it.getAttribute("name") }
        }.toSet()
    }
}
