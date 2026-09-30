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
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Flaky-test retries, the slow-test list, Kover and PIT. */
class TestsAndCoverageTest {
    @Test
    fun `on CI a flaky test is retried, passes, and is named`(
        @TempDir dir: File,
    ) {
        writeJavaProjectWithTests(dir)
        // Fails on its first run and passes on the retry, which may be in a new test JVM: the marker is a file
        // in the project's build directory, the test's working directory.
        writeTest(
            dir,
            "FlakyTest",
            """
            @Test
            void flaky() throws Exception {
                java.nio.file.Path marker = java.nio.file.Path.of("build", "flaky-marker");
                if (java.nio.file.Files.notExists(marker)) {
                    java.nio.file.Files.createFile(marker);
                    throw new AssertionError("first run fails");
                }
            }
            """,
        )

        val result = ArmorTestFixture.runWithEnvironment(dir, "test", set = mapOf("CI" to "true"))

        assertContains(result.output, "1 test(s) in :test failed, then passed on a retry (flaky):")
        assertContains(result.output, "com.example.FlakyTest > flaky()")
    }

    @Test
    fun `off CI a failing test is not retried`(
        @TempDir dir: File,
    ) {
        writeJavaProjectWithTests(dir)
        writeTest(dir, "AlwaysFailsTest", "@Test\nvoid fails() {\n    throw new AssertionError(\"always\");\n}")

        val failure = ArmorTestFixture.runAndFailWithEnvironment(dir, "test", unset = setOf("CI")).output

        assertEquals(1, Regex("AlwaysFailsTest > fails\\(\\) FAILED").findAll(failure).count(), "the test ran more than once")
    }

    @Test
    fun `tests over the slow threshold are listed`(
        @TempDir dir: File,
    ) {
        writeJavaProjectWithTests(dir, armorConfig = "    tests { slowThresholdMillis = 300 }")
        writeTest(dir, "SlowTest", "@Test\nvoid sleeps() throws Exception {\n    Thread.sleep(600);\n}\n\n@Test\nvoid quick() {\n}")

        val result = ArmorTestFixture.run(dir, "test")

        assertContains(result.output, "slowest tests in :test (over 0.3s):")
        assertContains(result.output, "com.example.SlowTest > sleeps()")
        assertFalse(result.output.contains("SlowTest > quick()"), "a quick test was listed")
    }

    @Test
    fun `coverage { kover = true } measures a Kotlin project with Kover instead of JaCoCo`(
        @TempDir dir: File,
    ) {
        writeKotlinProjectWithTests(dir, armorConfig = "    coverage { kover = true }")
        writeKotlinTest(dir, "SampleTest", "@Test\nfun adds() {\n    assertEquals(3, Sample().add(1, 2))\n}")

        val result = ArmorTestFixture.run(dir, "codeQuality")

        assertEquals(TaskOutcome.SUCCESS, result.task(":koverVerify")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":test")?.outcome, "Kover did not run the tests")
        assertContains(dir.resolve("build/reports/kover/report.xml").readText(), "com/example/Sample")
        assertEquals(null, result.task(":jacocoTestReport"), "JaCoCo ran as well")
    }

    @Test
    fun `Kover enforces the coverage minimum`(
        @TempDir dir: File,
    ) {
        // A test that calls nothing: Sample is not covered at all.
        writeKotlinProjectWithTests(dir, armorConfig = "    coverage { kover = true; minimum = 0.5; classMinimum = 0.0 }")
        writeKotlinTest(dir, "EmptyTest", "@Test\nfun nothing() {\n}")

        val failure = ArmorTestFixture.runAndFail(dir, "koverVerify")

        assertContains(failure.output, "Rule violated")
    }

    @Test
    fun `a Java-only project keeps JaCoCo when coverage { kover = true }`(
        @TempDir dir: File,
    ) {
        // Kover only measures projects that apply the Kotlin plugin.
        writeJavaProjectWithTests(dir, armorConfig = "    coverage { kover = true }")
        writeTest(dir, "SampleTest", "@Test\nvoid adds() {\n    org.junit.jupiter.api.Assertions.assertEquals(3, new Sample().add(1, 2));\n}")

        val result = ArmorTestFixture.run(dir, "codeQuality")

        assertEquals(TaskOutcome.SUCCESS, result.task(":jacocoTestReport")?.outcome)
        assertEquals(null, result.task(":koverVerify"), "Kover ran in a Java-only project")
    }

    @Test
    fun `SonarQube reads Kover's report when Kover is on`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(
            dir,
            language = Language.KOTLIN,
            armorConfig = "    coverage { kover = true }",
            extraScript =
                """
                tasks.register("printCoveragePaths") {
                    doLast {
                        val sonarTask = project.tasks.getByName("sonar") as org.sonarqube.gradle.SonarTask
                        println("COVERAGE_PATHS " + sonarTask.properties.get()["sonar.coverage.jacoco.xmlReportPaths"])
                    }
                }
                """.trimIndent(),
        )

        val result = ArmorTestFixture.run(dir, "printCoveragePaths")

        assertContains(result.output, "COVERAGE_PATHS build/reports/kover/report.xml")
    }

    @Test
    fun `mutationTesting { enabled = true } runs PIT on demand and writes its reports`(
        @TempDir dir: File,
    ) {
        writeJavaProjectWithTests(dir, armorConfig = "    mutationTesting { enabled = true }")
        writeTest(dir, "SampleTest", "@Test\nvoid adds() {\n    org.junit.jupiter.api.Assertions.assertEquals(3, new Sample().add(1, 2));\n}")

        ArmorTestFixture.run(dir, "pitest")

        assertTrue(dir.resolve("build/reports/pitest/index.html").isFile, "no PIT HTML report")
        assertContains(dir.resolve("build/reports/pitest/mutations.xml").readText(), "com.example.Sample")
    }

    @Test
    fun `PIT is off by default and not part of any tier`(
        @TempDir dir: File,
    ) {
        writeJavaProjectWithTests(dir)

        val result = ArmorTestFixture.run(dir, "fullAnalysis", "--dry-run", "tasks", "--all")

        assertFalse(result.output.contains(":pitest"), "pitest ran in a tier")
        assertFalse(result.output.lineSequence().any { it.startsWith("pitest ") }, "a pitest task exists although it is off")
    }

    private fun writeJavaProjectWithTests(
        dir: File,
        armorConfig: String = "",
    ) {
        ArmorTestFixture.writeProject(
            dir,
            armorConfig = "$QUIET\n$armorConfig",
            extraScript =
                """
                dependencies {
                    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
                    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
                }
                """.trimIndent(),
        )
    }

    private fun writeKotlinProjectWithTests(
        dir: File,
        armorConfig: String = "",
    ) {
        ArmorTestFixture.writeProject(
            dir,
            language = Language.KOTLIN,
            armorConfig = "$QUIET\n$armorConfig",
            extraScript =
                """
                dependencies {
                    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
                    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
                }
                """.trimIndent(),
        )
    }

    private fun writeKotlinTest(
        dir: File,
        className: String,
        body: String,
    ) {
        dir.resolve("src/test/kotlin/com/example").apply { mkdirs() }.resolve("$className.kt").writeText(
            "package com.example\n\nimport org.junit.jupiter.api.Assertions.assertEquals\nimport org.junit.jupiter.api.Test\n\n" +
                "class $className {\n${body.trimIndent().prependIndent("    ")}\n}\n",
        )
    }

    private fun writeTest(
        dir: File,
        className: String,
        body: String,
    ) {
        dir.resolve("src/test/java/com/example").apply { mkdirs() }.resolve("$className.java").writeText(
            "package com.example;\n\nimport org.junit.jupiter.api.Test;\n\nclass $className {\n${body.trimIndent().prependIndent("    ")}\n}\n",
        )
    }

    private companion object {
        /** No SpotBugs or detekt, and no coverage thresholds unless a test sets them. */
        const val QUIET = "    spotbugs { enabled = false }\n    detekt { enabled = false }\n    coverage { minimum = 0.0; classMinimum = 0.0 }"
    }
}
