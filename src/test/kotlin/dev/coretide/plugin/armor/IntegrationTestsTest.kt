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

/** `tests { integrationTests = true }`: an `integrationTest` suite in the build tier, counted in coverage and the summary. */
class IntegrationTestsTest {
    private val junit =
        """
        dependencies {
            testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
            testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        }
        """.trimIndent()

    private fun writeIntegrationTest(dir: File) {
        dir.resolve("src/integrationTest/java/com/example").mkdirs()
        dir.resolve("src/integrationTest/java/com/example/SampleIT.java").writeText(
            """
            package com.example;

            import static org.junit.jupiter.api.Assertions.assertEquals;

            import org.junit.jupiter.api.Test;

            class SampleIT {
                @Test
                void adds() {
                    assertEquals(3, new Sample().add(1, 2));
                }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `build runs the integration tests, and their coverage counts`(
        @TempDir dir: File,
    ) {
        // No unit tests: without the integration tests' coverage, coverage verification would fail.
        ArmorTestFixture.writeProject(dir, armorConfig = "    tests { integrationTests = true }", extraScript = junit)
        writeIntegrationTest(dir)

        val result = ArmorTestFixture.run(dir, "build")

        assertTrue(result.output.lines().any { it == "> Task :integrationTest" }, result.output)
        assertContains(result.output, "✅ Integration tests: 1 test")
        assertContains(result.output, "✅ Coverage (JaCoCo): 100.0% of lines")
        assertTrue(dir.resolve("build/test-results/integrationTest").isDirectory)
    }

    @Test
    fun `a failing integration test fails the build`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, armorConfig = "    tests { integrationTests = true }", extraScript = junit)
        writeIntegrationTest(dir)
        val test = dir.resolve("src/integrationTest/java/com/example/SampleIT.java")
        test.writeText(test.readText().replace("assertEquals(3,", "assertEquals(4,"))

        val result = ArmorTestFixture.runAndFail(dir, "build")

        assertContains(result.output, "SampleIT")
        assertContains(result.output, ":integrationTest")
    }

    @Test
    fun `without integrationTests there is no suite`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, extraScript = junit)

        val result = ArmorTestFixture.run(dir, "tasks", "--all")

        assertFalse(result.output.lines().any { it.startsWith("integrationTest ") }, result.output)
    }
}
