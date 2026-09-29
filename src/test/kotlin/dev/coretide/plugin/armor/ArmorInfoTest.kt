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

import dev.coretide.plugin.armor.ArmorTestFixture.Language
import dev.coretide.plugin.armor.task.ArmorInfoTask
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** `armorInfo`: the tiers, the tools, and what in the setup needs attention. */
class ArmorInfoTest {
    /** No SonarQube server, NVD API key or Veracode account comes from the machine running the tests. */
    private val unconfigured = setOf("SONAR_HOST_URL", "SONAR_TOKEN", "NVD_API_KEY", "VERACODE_USERNAME", "VERACODE_PASSWORD")

    private fun info(
        dir: File,
        vararg args: String,
        environment: Map<String, String> = emptyMap(),
    ) = ArmorTestFixture.runWithEnvironment(dir, "armorInfo", *args, set = environment, unset = unconfigured).output

    @Test
    fun `a Java project's tiers, tools, and what needs attention`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir)

        val output = info(dir, "--configuration-cache")
        val reused = info(dir, "--configuration-cache")

        assertContains(output, "🛡️ CodeArmor ")
        assertFalse(output.contains("(unknown version)"), output)
        assertContains(output, "  build, through codeQuality: spotbugsMain, jacocoTestReport, jacocoTestCoverageVerification")
        assertContains(output, "  CI, through fullAnalysis: dependencyCheckAnalyze, dependencyUpdates, armorLicenseReport")
        assertContains(output, "  ✅ SpotBugs 4.10.3")
        assertContains(output, "  ✅ JaCoCo 0.8.15: at least 30% of instructions, 25% of lines per class")
        assertContains(output, "  ➖ detekt: no Kotlin here")
        assertContains(output, "  ➖ SonarQube: no server configured")
        assertContains(output, "⚠️  SonarQube is on, but no server or token is configured, so fullAnalysis leaves it out")
        assertContains(output, "⚠️  OWASP has no NVD API key")
        assertContains(reused, "Reusing configuration cache.")
        assertContains(reused, "⚠️  SonarQube is on, but no server or token is configured")
    }

    @Test
    fun `a configured project needs no attention`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(
            dir,
            armorConfig = "    sonarHostUrl = \"https://sonar.example.com\"\n    owaspNvdApiKey = \"key\"\n    coverageMinimum = 0.825",
        )

        val output = info(dir)

        assertContains(output, "  ✅ SonarQube: https://sonar.example.com")
        assertContains(output, "  ✅ OWASP Dependency-Check: fails at CVSS 9.0, with an NVD API key")
        assertContains(output, "at least 82.5% of instructions")
        assertContains(output, "  CI, through fullAnalysis: dependencyCheckAnalyze, dependencyUpdates, armorLicenseReport, sonar")
        assertContains(output, "✅ Nothing needs attention")
    }

    @Test
    fun `sonar listed in checks ci without a server needs attention`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, armorConfig = "    checks {\n        ci = listOf(\"sonar\")\n    }")

        val output = info(dir)

        assertContains(output, "⚠️  sonar is in checks.ci, but no SonarQube server or token is configured")
    }

    @Test
    fun `a Kotlin project with kover = true uses Kover and detekt, a Java one JaCoCo`(
        @TempDir kotlin: File,
        @TempDir java: File,
    ) {
        ArmorTestFixture.writeProject(kotlin, language = Language.KOTLIN, armorConfig = "    kover = true")
        ArmorTestFixture.writeProject(java, armorConfig = "    kover = true")

        val kotlinOutput = info(kotlin)
        val javaOutput = info(java)

        assertContains(kotlinOutput, "  ✅ Kover: at least 30% of lines, 25% per class")
        assertContains(kotlinOutput, "  ✅ detekt ")
        assertContains(javaOutput, "(kover = true, but no Kotlin here)")
    }

    @Test
    fun `missing git hooks and gitleaks need attention, installed ones do not`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, armorConfig = "    enableGitHooks = true\n    secretScan = true\n    sonarqube = false\n    owasp = false")
        ArmorTestFixture.initGitRepository(dir)
        val gitleaks = dir.resolve("tools/gitleaks").apply { parentFile.mkdirs() }
        gitleaks.writeText("#!/bin/sh\nexit 0\n")
        gitleaks.setExecutable(true)
        val git = ArmorTestFixture.isolatedGitEnvironment

        val before = info(dir, environment = git + ("CODEARMOR_GITLEAKS" to dir.resolve("tools/missing").absolutePath))
        ArmorTestFixture.runWithEnvironment(dir, "armorInstallGitHooks", set = git)
        val after = info(dir, environment = git + ("CODEARMOR_GITLEAKS" to gitleaks.absolutePath))

        assertContains(before, "  pre-push hook: quickBuild")
        assertContains(before, "  ✅ Git hooks: pre-push, pre-commit (secret scan)")
        assertContains(before, "⚠️  Git hooks not installed: pre-push, pre-commit. Run ./gradlew armorInstallGitHooks")
        assertContains(before, "⚠️  secretScan is on, but gitleaks is not installed")
        assertContains(after, "✅ Nothing needs attention")
    }

    @Test
    fun `a multi-module build shows the build, then each module`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeMultiModuleProject(dir)

        val output = info(dir, "--configuration-cache")

        assertContains(output, "· armor-multi-fixture (multi-module)")
        assertContains(output, "Modules, each with its own armorInfo: :module-a, :module-b")
        assertContains(output, "· :module-a (Java ")
        assertContains(output, "· :module-b (Java ")
    }

    @Test
    fun `gitleaks is looked up as a path, or on the PATH`(
        @TempDir dir: File,
    ) {
        val bin = dir.resolve("bin").apply { mkdirs() }
        val empty = dir.resolve("empty").apply { mkdirs() }
        bin.resolve("gitleaks").apply {
            writeText("")
            setExecutable(true)
        }
        bin.resolve("scanner.exe").apply {
            writeText("")
            setExecutable(true)
        }
        val path = listOf(empty, bin).joinToString(File.pathSeparator)

        assertEquals(bin.resolve("gitleaks"), ArmorInfoTask.findExecutable("gitleaks", path, windows = false))
        assertEquals(bin.resolve("gitleaks"), ArmorInfoTask.findExecutable(bin.resolve("gitleaks").path, "", windows = false))
        assertEquals(bin.resolve("scanner.exe"), ArmorInfoTask.findExecutable("scanner", path, windows = true))
        assertNull(ArmorInfoTask.findExecutable("scanner", path, windows = false))
        assertNull(ArmorInfoTask.findExecutable("gitleaks", empty.path, windows = false))
    }
}
