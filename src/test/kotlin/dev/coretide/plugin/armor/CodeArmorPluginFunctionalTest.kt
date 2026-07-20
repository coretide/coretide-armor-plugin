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

/**
 * Behavioural contract of the plugin as consumers experience it. These assertions are the
 * regression net for the configuration-cache rework — they must keep passing across it.
 */
class CodeArmorPluginFunctionalTest {
    @Test
    fun `applies cleanly to a java project and registers its tasks`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, ArmorTestFixture.Language.JAVA)

        val result = ArmorTestFixture.run(dir, "tasks", "--all")

        assertContains(result.output, "quickBuild")
        assertContains(result.output, "codeQuality")
        assertContains(result.output, "fullAnalysis")
        assertContains(result.output, "logExclusionInfo")
    }

    @Test
    fun `detects a java project`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, ArmorTestFixture.Language.JAVA)

        val result = ArmorTestFixture.run(dir, "help", "-PcodeArmorLogLevel=ESSENTIAL")

        assertContains(result.output, "CodeArmor: Detected")
    }

    @Test
    fun `detects a spring boot project`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeSpringBootLikeProject(dir)

        val result = ArmorTestFixture.run(dir, "help")

        assertContains(result.output, "CodeArmor: Detected")
    }

    @Test
    fun `detects a multi-module project`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeMultiModuleProject(dir)

        val result = ArmorTestFixture.run(dir, "help")

        assertContains(result.output, "multi-module")
    }

    @Test
    fun `disabling every tool still applies without registering quality tasks`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(
            dir,
            armorConfig =
                """
                    jacoco = false
                    spotbugs = false
                    sonarqube = false
                    owasp = false
                    veracode = false
                """.trimIndent(),
        )

        val result = ArmorTestFixture.run(dir, "tasks", "--all")

        // quickBuild and logExclusionInfo are unconditional; the aggregate tasks are not.
        assertContains(result.output, "quickBuild")
        assertFalse(result.output.contains("codeQuality -"), "codeQuality should not be registered")
        assertFalse(result.output.contains("fullAnalysis -"), "fullAnalysis should not be registered")
    }

    @Test
    fun `logExclusionInfo reports default exclusions`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir)

        val result = ArmorTestFixture.run(dir, "logExclusionInfo")

        assertContains(result.output, "CodeArmor Exclusion Information")
        assertContains(result.output, "Default exclusions:")
        assertContains(result.output, "Minimum coverage:")
    }

    @Test
    fun `logExclusionInfo reflects user exclusions and coverage thresholds`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(
            dir,
            armorConfig =
                """
                    coverageMinimum = 0.75
                    coverageExclusions = mutableListOf("**/generated/**", "**/dto/**")
                """.trimIndent(),
        )

        val result = ArmorTestFixture.run(dir, "logExclusionInfo")

        assertContains(result.output, "User exclusions: 2")
        assertContains(result.output, "Minimum coverage: 75%")
    }

    @Test
    fun `logExclusionInfo honours disabled default exclusions`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(
            dir,
            armorConfig = "    coverageIncludeDefaultExclusions = false",
        )

        val result = ArmorTestFixture.run(dir, "logExclusionInfo")

        assertContains(result.output, "Default exclusions: DISABLED")
    }

    @Test
    fun `jacoco and spotbugs tasks are registered when enabled`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, armorConfig = "    sonarqube = false\n    owasp = false")

        val result = ArmorTestFixture.run(dir, "tasks", "--all")

        assertContains(result.output, "jacocoTestReport")
        assertContains(result.output, "spotbugsMain")
    }

    @Test
    fun `owasp dependencyCheckAnalyze is registered when enabled`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(
            dir,
            armorConfig = "    jacoco = false\n    spotbugs = false\n    sonarqube = false",
        )

        val result = ArmorTestFixture.run(dir, "tasks", "--all")

        assertContains(result.output, "dependencyCheckAnalyze")
    }

    @Test
    fun `quickBuild compiles and tests without running quality checks`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir)

        val result = ArmorTestFixture.run(dir, "quickBuild")

        assertTrue(result.output.contains("BUILD SUCCESSFUL") || result.tasks.isNotEmpty())
        assertFalse(result.output.contains("spotbugsMain"), "quickBuild must not trigger SpotBugs")
    }
}
