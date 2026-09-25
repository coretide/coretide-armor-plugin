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

import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * The three check tiers: pre-push runs the basic checks (see [GitHooksTest]), `build` runs the local
 * checks through `codeQuality`, and `fullAnalysis` adds the checks that need a network or a server.
 */
class CheckTiersTest {
    /** The tasks a `--dry-run` build would have executed. */
    private fun BuildResult.plannedTasks(): Set<String> =
        output
            .lines()
            .filter { it.startsWith(":") && it.endsWith(" SKIPPED") }
            .map { it.removeSuffix(" SKIPPED").removePrefix(":") }
            .toSet()

    private fun plan(
        dir: File,
        task: String,
        armorConfig: String = "",
    ): Set<String> {
        ArmorTestFixture.writeProject(dir, armorConfig = armorConfig)
        return ArmorTestFixture.run(dir, task, "--dry-run").plannedTasks()
    }

    @Test
    fun `build runs the local checks but not the network ones`(
        @TempDir dir: File,
    ) {
        val planned = plan(dir, "build")

        assertTrue(planned.containsAll(listOf("codeQuality", "spotbugsMain", "jacocoTestCoverageVerification")), "$planned")
        assertFalse("sonar" in planned, "SonarQube needs a server; it belongs to fullAnalysis")
        assertFalse("dependencyCheckAnalyze" in planned, "OWASP needs the network; it belongs to fullAnalysis")
    }

    @Test
    fun `codeQuality no longer runs SonarQube`(
        @TempDir dir: File,
    ) {
        val planned = plan(dir, "codeQuality")

        assertTrue("jacocoTestCoverageVerification" in planned, "$planned")
        assertFalse("sonar" in planned)
    }

    @Test
    fun `fullAnalysis adds OWASP and SonarQube to the local checks`(
        @TempDir dir: File,
    ) {
        val planned = plan(dir, "fullAnalysis")

        assertTrue(
            planned.containsAll(listOf("codeQuality", "jacocoTestCoverageVerification", "dependencyCheckAnalyze", "sonar")),
            "$planned",
        )
    }

    @Test
    fun `the default tiers follow the tool switches`(
        @TempDir dir: File,
    ) {
        val build = plan(dir, "build", armorConfig = "    jacoco = false")
        val fullAnalysis = plan(dir, "fullAnalysis", armorConfig = "    sonarqube = false")

        assertFalse("jacocoTestCoverageVerification" in build, "$build")
        assertFalse("sonar" in fullAnalysis, "$fullAnalysis")
        assertTrue("dependencyCheckAnalyze" in fullAnalysis, "$fullAnalysis")
    }

    @Test
    fun `checks build can be emptied to keep build lean`(
        @TempDir dir: File,
    ) {
        val planned =
            plan(
                dir,
                "build",
                armorConfig =
                    """
                        checks {
                            build = listOf<String>()
                        }
                    """.trimIndent(),
            )

        assertFalse("codeQuality" in planned, "$planned")
        assertFalse("jacocoTestCoverageVerification" in planned, "$planned")
    }

    @Test
    fun `a project without the Java plugin still builds`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, withSource = false)
        // Say, a docs or aggregator project: no compile, test, SpotBugs or JaCoCo tasks exist.
        dir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                base
                id("dev.coretide.plugin.armor")
            }

            codeArmor {
                enableGitHooks = false
                enableVersionFromGit = false
            }
            """.trimIndent(),
        )

        val planned = ArmorTestFixture.run(dir, "build", "--dry-run").plannedTasks()

        assertFalse("codeQuality" in planned, "$planned")
    }

    @Test
    fun `a multi-module build with a non-Java module runs the checks in the Java modules`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeMultiModuleProject(dir, modules = listOf("app", "docs"))
        dir.resolve("docs/build.gradle.kts").writeText("plugins {\n    base\n}\n")
        dir.resolve("docs/src").deleteRecursively()

        val result = ArmorTestFixture.run(dir, "build", "--dry-run")

        assertTrue(":app:codeQuality SKIPPED" in result.output.lines(), result.output)
        assertFalse(result.output.contains(":docs:codeQuality"), result.output)
    }

    @Test
    fun `checks ci replaces the network checks`(
        @TempDir dir: File,
    ) {
        val planned =
            plan(
                dir,
                "fullAnalysis",
                armorConfig =
                    """
                        checks {
                            ci = listOf("dependencyCheckAnalyze")
                        }
                    """.trimIndent(),
            )

        assertTrue("dependencyCheckAnalyze" in planned, "$planned")
        assertFalse("sonar" in planned, "$planned")
    }
}
