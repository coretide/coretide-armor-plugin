/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor

import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

/** docs/settings.md against the extension: every setting listed in its block, with its default. */
class SettingsReferenceTest {
    private val armor: CodeArmorExtension =
        ProjectBuilder.builder().build().extensions.create("codeArmor", CodeArmorExtension::class.java)

    /** Each block by its name in the DSL; the top level is "". */
    private val blocks: Map<String, Any> =
        with(armor) {
            linkedMapOf(
                "" to this,
                "coverage" to coverage,
                "diffCoverage" to diffCoverage,
                "tests" to tests,
                "mutationTesting" to mutationTesting,
                "compilation" to compilation,
                "spotbugs" to spotbugs,
                "detekt" to detekt,
                "owasp" to owasp,
                "dependencyHealth" to dependencyHealth,
                "libraryApi" to libraryApi,
                "sonarqube" to sonarqube,
                "gitHooks" to gitHooks,
                "checks" to checks,
                "codeStats" to codeStats,
                "toolVersions" to toolVersions,
            )
        }

    /** Defaults the plugin computes for each project, which the page describes in words. */
    private val computed = setOf("checks.build", "checks.ci")

    /** Each section's rows: setting to the default column, without backticks. */
    private fun documented(): Map<String, Map<String, String>> {
        val sections = linkedMapOf<String, MutableMap<String, String>>()
        var section: MutableMap<String, String>? = null
        File("docs/settings.md").readLines().forEach { line ->
            if (line.startsWith("## ")) {
                val name = line.removePrefix("## ").trim('`', ' ')
                section = linkedMapOf<String, String>().also { sections[if (name == "Top level") "" else name] = it }
            } else if (line.startsWith("| `")) {
                val cells = line.split('|').map { it.trim() }
                section!![cells[1].trim('`')] = cells[3].replace("`", "")
            }
        }
        return sections
    }

    /** A block's settings, by name: its Gradle properties. */
    private fun settings(block: Any): Map<String, Provider<*>> =
        block.javaClass.methods
            .filter { it.parameterCount == 0 && it.name.startsWith("get") && Provider::class.java.isAssignableFrom(it.returnType) }
            .associate { it.name.removePrefix("get").replaceFirstChar(Char::lowercase) to it.invoke(block) as Provider<*> }

    private fun render(setting: Provider<*>): String =
        when (val value = setting.orNull) {
            null -> "unset"
            is List<*> -> if (value.isEmpty()) "empty" else value.joinToString(", ")
            is Enum<*> -> value.name
            else -> value.toString()
        }

    @Test
    fun `every block and setting is on the page`() {
        val documented = documented()

        assertEquals(blocks.keys, documented.keys)
        blocks.forEach { (name, block) ->
            assertEquals(settings(block).keys.sorted(), documented.getValue(name).keys.sorted(), "the settings of '$name'")
        }
    }

    @Test
    fun `each default on the page is the plugin's`() {
        val documented = documented()

        blocks.forEach { (name, block) ->
            settings(block)
                .filterKeys { "$name.$it" !in computed }
                .forEach { (setting, value) ->
                    assertEquals(render(value), documented.getValue(name)[setting], "the default of $name.$setting".removePrefix("."))
                }
        }
    }
}
