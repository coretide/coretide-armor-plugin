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
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** `armorSecretScan`: gitleaks over the history in the CI tier, with its findings for code scanning. */
class SecretScanTest {
    private val isWindows = System.getProperty("os.name").startsWith("Windows")

    /**
     * A stand-in gitleaks: it answers `git --help`, writes an empty SARIF report where it is told, and finds a
     * secret when FAKE_LEAK is 1.
     */
    private fun fakeGitleaks(tools: File): File {
        val sarif = """{"version":"2.1.0","runs":[{"tool":{"driver":{"name":"gitleaks"}},"results":[]}]}"""
        return if (isWindows) {
            // Arguments: git <repository> --report-format sarif --report-path <file> ...
            tools.resolve("gitleaks.cmd").apply {
                writeText(
                    "@echo off\r\nif \"%2\"==\"--help\" exit /b 0\r\n>\"%~6\" echo $sarif\r\n" +
                        "if \"%FAKE_LEAK%\"==\"1\" exit /b 1\r\nexit /b 0\r\n",
                )
            }
        } else {
            tools.resolve("gitleaks").apply {
                writeText(
                    """
                    #!/bin/sh
                    [ "${'$'}1 ${'$'}2" = "git --help" ] && exit 0
                    while [ ${'$'}# -gt 0 ]; do [ "${'$'}1" = "--report-path" ] && out="${'$'}2"; shift; done
                    echo '$sarif' > "${'$'}out"
                    [ "${'$'}FAKE_LEAK" = 1 ] && exit 1
                    exit 0
                    """.trimIndent() + "\n",
                )
                setExecutable(true, false)
            }
        }
    }

    private fun gitProject(dir: File) {
        ArmorTestFixture.writeProject(dir, armorConfig = "    secretScan = true")
        ArmorTestFixture.initGitRepository(dir)
    }

    private fun scan(
        dir: File,
        gitleaks: File,
        vararg tasks: String,
        leak: Boolean = false,
        fail: Boolean = false,
    ): BuildResult {
        val environment =
            ArmorTestFixture.isolatedGitEnvironment + ("CODEARMOR_GITLEAKS" to gitleaks.absolutePath) + ("FAKE_LEAK" to if (leak) "1" else "0")
        return if (fail) {
            ArmorTestFixture.runAndFailWithEnvironment(dir, *tasks, set = environment)
        } else {
            ArmorTestFixture.runWithEnvironment(dir, *tasks, set = environment)
        }
    }

    @Test
    fun `a clean history passes, and its SARIF goes to code scanning`(
        @TempDir dir: File,
        @TempDir tools: File,
    ) {
        gitProject(dir)

        val result = scan(dir, fakeGitleaks(tools), "armorSecretScan", "armorSarifReport")

        assertContains(result.output, "🔑 gitleaks: no secrets in the history")
        val gathered = dir.resolve("build/reports/sarif/gitleaks-gitleaks.sarif")
        assertTrue(gathered.isFile, "not gathered")
        assertContains(gathered.readText(), "codearmor/gitleaks/gitleaks/")
    }

    @Test
    fun `a secret in the history fails the scan`(
        @TempDir dir: File,
        @TempDir tools: File,
    ) {
        gitProject(dir)

        val result = scan(dir, fakeGitleaks(tools), "armorSecretScan", leak = true, fail = true)

        assertContains(result.output, "gitleaks found secrets in the history")
    }

    @Test
    fun `without gitleaks the scan fails and says how to install it`(
        @TempDir dir: File,
    ) {
        gitProject(dir)

        val result = scan(dir, dir.resolve("no-gitleaks"), "armorSecretScan", fail = true)

        assertContains(result.output, "gitleaks is not installed")
    }

    @Test
    fun `fullAnalysis runs the scan, once for a multi-module build`(
        @TempDir single: File,
        @TempDir multi: File,
    ) {
        ArmorTestFixture.writeProject(single, armorConfig = "    secretScan = true")
        ArmorTestFixture.writeMultiModuleProject(multi, armorConfig = "    secretScan = true")

        val planned = ArmorTestFixture.run(single, "fullAnalysis", "--dry-run").output.lines()
        val plannedMulti = ArmorTestFixture.run(multi, "fullAnalysis", "--dry-run").output.lines()

        assertTrue(":armorSecretScan SKIPPED" in planned, planned.joinToString("\n"))
        assertEquals(1, plannedMulti.count { it.endsWith("armorSecretScan SKIPPED") }, plannedMulti.joinToString("\n"))
        assertTrue(":armorSecretScan SKIPPED" in plannedMulti, plannedMulti.joinToString("\n"))
    }
}
