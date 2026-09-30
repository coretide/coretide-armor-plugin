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
import kotlin.test.assertContains
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

    /** Planned with no SonarQube server in the environment, unless [environment] names one. */
    private fun plan(
        dir: File,
        task: String,
        armorConfig: String = "",
        environment: Map<String, String> = emptyMap(),
    ): Set<String> {
        ArmorTestFixture.writeProject(dir, armorConfig = armorConfig)
        return ArmorTestFixture
            .runWithEnvironment(dir, task, "--dry-run", set = environment, unset = SONAR_ENVIRONMENT)
            .plannedTasks()
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
    fun `fullAnalysis adds OWASP and dependency health to the local checks`(
        @TempDir dir: File,
    ) {
        val planned = plan(dir, "fullAnalysis")

        assertTrue(
            planned.containsAll(
                listOf(
                    "codeQuality",
                    "jacocoTestCoverageVerification",
                    "dependencyCheckAnalyze",
                    "dependencyUpdates",
                    "cyclonedxBom",
                    "armorLicenseReport",
                ),
            ),
            "$planned",
        )
        assertFalse("projectHealth" in planned, "the dependency analysis is opt-in")
        assertFalse("sonar" in planned, "no SonarQube server is configured")
    }

    @Test
    fun `fullAnalysis runs SonarQube once a server or token is configured, or checks ci lists it`(
        @TempDir inTheBuild: File,
        @TempDir fromTheEnvironment: File,
        @TempDir listed: File,
    ) {
        val host = plan(inTheBuild, "fullAnalysis", armorConfig = "    sonarHostUrl = \"https://sonar.example.com\"")
        // A token alone means SonarQube Cloud, the SonarScanner's default server.
        val token = plan(fromTheEnvironment, "fullAnalysis", environment = mapOf("SONAR_TOKEN" to "from-ci"))
        val explicit = plan(listed, "fullAnalysis", armorConfig = "    checks {\n        ci = listOf(\"sonar\")\n    }")

        assertTrue("sonar" in host, "$host")
        assertTrue("sonar" in token, "$token")
        assertTrue("sonar" in explicit, "$explicit")
    }

    @Test
    fun `fullAnalysis says why it left SonarQube out`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, armorConfig = "    owasp = false\n    dependencyUpdates = false\n    sbom = false")

        val result = ArmorTestFixture.runWithEnvironment(dir, "fullAnalysis", unset = SONAR_ENVIRONMENT)

        assertContains(result.output, "SonarQube skipped: no server configured")
    }

    @Test
    fun `the default tiers follow the tool switches`(
        @TempDir dir: File,
    ) {
        val build = plan(dir, "build", armorConfig = "    jacoco = false")
        val fullAnalysis =
            plan(dir, "fullAnalysis", armorConfig = "    sonarqube = false\n    sbom = false\n    dependencyUpdates = false")

        assertFalse("jacocoTestCoverageVerification" in build, "$build")
        assertFalse("sonar" in fullAnalysis, "$fullAnalysis")
        assertFalse("armorLicenseReport" in fullAnalysis, "$fullAnalysis")
        assertFalse("dependencyUpdates" in fullAnalysis, "$fullAnalysis")
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

    @Test
    fun `checks ci add keeps the default network checks`(
        @TempDir dir: File,
    ) {
        // Until 0.5.0, add() replaced the defaults, which were a convention: only armorCustomCheck would have run.
        ArmorTestFixture.writeProject(
            dir,
            armorConfig = "    checks { ci.add(\"armorCustomCheck\") }",
            extraScript = "tasks.register(\"armorCustomCheck\")",
        )

        val planned =
            ArmorTestFixture.runWithEnvironment(dir, "fullAnalysis", "--dry-run", unset = SONAR_ENVIRONMENT).plannedTasks()

        assertTrue("armorCustomCheck" in planned, "$planned")
        assertTrue("dependencyCheckAnalyze" in planned, "$planned")
        assertTrue("dependencyUpdates" in planned, "$planned")
    }

    @Test
    fun `checks prePush add keeps quickBuild`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, armorConfig = "    enableGitHooks = true\n    checks { prePush.add(\"spotbugsMain\") }")

        val output = ArmorTestFixture.run(dir, "armorInfo").output

        assertContains(output, "  pre-push hook: quickBuild, spotbugsMain")
    }

    private companion object {
        val SONAR_ENVIRONMENT = setOf("SONAR_HOST_URL", "SONAR_TOKEN")
    }
}
