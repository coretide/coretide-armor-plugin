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

import dev.coretide.plugin.armor.configurator.ArchitectureTestsConfigurator
import dev.coretide.plugin.armor.configurator.JacocoConfigurator
import java.io.File
import kotlin.test.assertContains
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** `toolVersions { }`: the versions of the tools CodeArmor runs, overridden per build. */
class ToolVersionsTest {
    private val print =
        """
        tasks.register("printVersions") {
            val jacocoVersion = project.extensions.getByType<JacocoPluginExtension>().toolVersion
            val dependencies =
                listOf("errorprone", "testImplementation").flatMap { name ->
                    configurations.getByName(name).allDependencies.map { name + " " + it.group + ":" + it.name + ":" + it.version }
                }
            doLast {
                println("VERSION jacoco " + jacocoVersion)
                dependencies.forEach { println("VERSION " + it) }
            }
        }
        """.trimIndent()

    private val tools = "    errorProne = true\n    nullAway = true\n    architectureTests = true\n    mutationTesting = true"

    @Test
    fun `each tool runs the version toolVersions names`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(
            dir,
            armorConfig =
                tools +
                    """

                    toolVersions {
                        jacoco = "0.8.13"
                        pitest = "1.19.0"
                        errorProne = "2.40.0"
                        nullAway = "0.12.9"
                        archUnit = "1.4.1"
                    }
                    """.trimIndent().prependIndent("    "),
            extraScript = print,
        )

        val output = ArmorTestFixture.run(dir, "printVersions", "armorInfo").output

        assertContains(output, "VERSION jacoco 0.8.13")
        assertContains(output, "VERSION errorprone com.google.errorprone:error_prone_core:2.40.0")
        assertContains(output, "VERSION errorprone com.uber.nullaway:nullaway:0.12.9")
        assertContains(output, "VERSION testImplementation com.tngtech.archunit:archunit-junit5:1.4.1")
        assertContains(output, "✅ JaCoCo 0.8.13:")
        assertContains(output, "✅ Error Prone 2.40.0, with NullAway 0.12.9")
        assertContains(output, "✅ Mutation testing with PIT 1.19.0")
        assertContains(output, "✅ Architecture tests with ArchUnit 1.4.1")
    }

    @Test
    fun `without overrides each tool runs the version CodeArmor was tested with`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, armorConfig = tools, extraScript = print)

        val output = ArmorTestFixture.run(dir, "printVersions").output

        assertContains(output, "VERSION jacoco ${JacocoConfigurator.TOOL_VERSION}")
        assertContains(output, "VERSION testImplementation com.tngtech.archunit:archunit-junit5:${ArchitectureTestsConfigurator.ARCHUNIT_VERSION}")
    }
}
