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

import dev.coretide.plugin.armor.util.FileUtil
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.owasp.dependencycheck.xml.suppression.SuppressionParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** How CodeArmor configures OWASP Dependency Check, read back from its extension. Nothing is scanned. */
class OwaspTest {
    /** Prints the settings once CodeArmor has configured the project, and the JVM-wide ones it must leave alone. */
    private val printOwasp =
        """
        gradle.projectsEvaluated {
            val check = project.extensions.getByType(org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension::class.java)
            val lines =
                listOf(
                    "SCAN " + check.scanConfigurations.get(),
                    "SKIP_PROJECTS " + check.skipProjects.get(),
                    "CENTRAL " + check.analyzers.centralEnabled.get(),
                    "RETIREJS " + check.analyzers.retirejs.enabled.get(),
                    "NVD_KEY " + check.nvd.apiKey.orNull,
                    "NVD_DELAY " + check.nvd.delay.get(),
                    "AUTO_UPDATE " + check.autoUpdate.get(),
                    "SUPPRESSION " + check.suppressionFile.orNull,
                    "SYSTEM_CENTRAL " + System.getProperty("analyzer.central.enabled"),
                    "SYSTEM_NVD_KEY " + System.getProperty("nvd.api.key"),
                )
            project.tasks.register("printOwasp") { doLast { lines.forEach { println("OWASP " + it) } } }
        }
        """.trimIndent()

    @Test
    fun `OWASP scans what ships, and is configured through its extension, not JVM-wide properties`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(
            dir,
            armorConfig = "    owaspNvdApiKey = \"test-key\"\n    owaspNvdApiDelay = 1234",
            extraScript = printOwasp,
        )

        val output = ArmorTestFixture.run(dir, "printOwasp").output

        // Not every resolvable configuration: CodeArmor's own tools would otherwise be scanned and fail the build.
        assertContains(output, "OWASP SCAN [runtimeClasspath]")
        assertContains(output, "OWASP SKIP_PROJECTS []")
        assertContains(output, "OWASP CENTRAL false")
        assertContains(output, "OWASP RETIREJS false")
        assertContains(output, "OWASP NVD_KEY test-key")
        assertContains(output, "OWASP NVD_DELAY 1234")
        // System properties outlive the build in the Gradle daemon and are shared by every project.
        assertContains(output, "OWASP SYSTEM_CENTRAL null")
        assertContains(output, "OWASP SYSTEM_NVD_KEY null")
    }

    @Test
    fun `the scan starts with the default suppressions`(
        @TempDir dir: File,
    ) {
        // Offline: the test downloads no NVD data, so the analysis itself then fails for want of it. What matters is
        // that it starts; until 0.4.0 it failed at once, reading a default suppression file from a task that had not run.
        ArmorTestFixture.writeProject(dir, armorConfig = "    owaspAutoUpdate = false")

        val output = ArmorTestFixture.runWhateverTheOutcome(dir, "dependencyCheckAnalyze").output

        assertContains(output, "analyzing dependencies for vulnerabilities")
        assertFalse(output.contains("property 'suppressionFile'"), output)
    }

    @Test
    fun `CodeArmor suppresses nothing itself, passes the build's own suppressions on, and downloads the NVD data`(
        @TempDir unset: File,
        @TempDir configured: File,
    ) {
        ArmorTestFixture.writeProject(unset, extraScript = printOwasp)
        ArmorTestFixture.writeProject(configured, armorConfig = "    owaspSuppressionFile = \"config/owasp/suppressions.xml\"", extraScript = printOwasp)
        configured.resolve("config/owasp").mkdirs()
        configured.resolve("config/owasp/suppressions.xml").writeText(FileUtil.defaultOwaspSuppressionContent())

        val none = ArmorTestFixture.run(unset, "printOwasp").output
        val own = ArmorTestFixture.run(configured, "printOwasp").output

        assertContains(none, "OWASP SUPPRESSION null")
        // Without it, a scan fails wherever there is no NVD data yet, as on a fresh CI runner.
        assertContains(none, "OWASP AUTO_UPDATE true")
        // As files: Gradle may name the project directory by another path to the same place, as macOS's /private/var.
        val passed = own.lines().first { it.startsWith("OWASP SUPPRESSION ") }.removePrefix("OWASP SUPPRESSION ")
        assertEquals(configured.resolve("config/owasp/suppressions.xml").canonicalFile, File(passed).canonicalFile)
    }

    @Test
    fun `the suppression file armorScaffoldConfigs writes suppresses nothing, and dependency-check accepts it`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir)

        ArmorTestFixture.run(dir, "armorScaffoldConfigs")

        // dependency-check's own parser, which validates against its schema.
        assertEquals(emptyList(), SuppressionParser().parseSuppressionRules(dir.resolve("config/owasp/suppressions.xml")))
    }
}
