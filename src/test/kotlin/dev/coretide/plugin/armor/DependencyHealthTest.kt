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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Newer versions, the SBOM and its licence report, and the opt-in dependency analysis. */
class DependencyHealthTest {
    private val dependencies =
        """
        dependencies {
            implementation("org.slf4j:slf4j-api:2.0.0")
            testImplementation("junit:junit:4.13.2")
        }
        """.trimIndent()

    @Test
    fun `the SBOM and licence report cover what ships, not tests or tools`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, extraScript = dependencies)

        val result = ArmorTestFixture.run(dir, "armorLicenseReport")

        val report = dir.resolve("build/reports/codearmor/licenses.txt").readText()
        assertContains(report, "MIT (1)\n  org.slf4j:slf4j-api:2.0.0")
        assertFalse(report.contains("junit"), report)
        assertFalse(report.contains("spotbugs") || report.contains("jacoco"), report)
        assertTrue(dir.resolve("build/reports/cyclonedx/bom.json").isFile)
        assertContains(result.output, "📜 CodeArmor: 1 dependencies under 1 licence(s)")
    }

    @Test
    fun `a forbidden licence fails the licence report`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, armorConfig = "    forbiddenLicenses = mutableListOf(\"MIT\")", extraScript = dependencies)

        val failure = ArmorTestFixture.runAndFail(dir, "armorLicenseReport")

        assertContains(failure.output, "1 dependencies can only be used under a forbidden licence:")
        assertContains(failure.output, "org.slf4j:slf4j-api:2.0.0: MIT")
    }

    @Test
    fun `dependencyUpdates lists newer releases, with the configuration cache on`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, extraScript = dependencies)

        ArmorTestFixture.run(dir, "dependencyUpdates", "--configuration-cache")

        val report = dir.resolve("build/dependencyUpdates/report.txt").readText()
        assertContains(report, "org.slf4j:slf4j-api [2.0.0 -> ")
        // Only releases are offered for a dependency on a release.
        val offered = Regex("""org\.slf4j:slf4j-api \[2\.0\.0 -> ([^]]+)]""").find(report)!!.groupValues[1]
        assertFalse(offered.contains("alpha") || offered.contains("beta"), offered)
        assertTrue(dir.resolve("build/dependencyUpdates/report.json").isFile)
        assertTrue(dir.resolve("build/dependencyUpdates/report.html").isFile)
    }

    @Test
    fun `dependencyAnalysis reports unused dependencies without failing`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(
            dir,
            armorConfig = "    dependencyAnalysis = true",
            extraPlugins = listOf("java-library"),
            extraScript = dependencies,
        )

        val result = ArmorTestFixture.run(dir, "projectHealth")

        assertContains(result.output, "Unused dependencies which should be removed")
        assertContains(result.output, "org.slf4j:slf4j-api:2.0.0")
    }

    @Test
    fun `a multi-module build gets the dependency analysis on its root and modules`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeMultiModuleProject(dir, armorConfig = "    dependencyAnalysis = true")
        dir.resolve("module-a/build.gradle.kts").writeText(
            """
            plugins {
                id("java-library")
            }

            repositories {
                mavenCentral()
            }

            $dependencies
            """.trimIndent(),
        )

        val result = ArmorTestFixture.run(dir, "buildHealth")

        assertContains(result.output, ":module-a:computeAdvice")
        assertContains(result.output, "There were non-fatal dependency violations.")
        assertContains(dir.resolve("build/reports/dependency-analysis/build-health-report.txt").readText(), "slf4j-api")
    }
}
