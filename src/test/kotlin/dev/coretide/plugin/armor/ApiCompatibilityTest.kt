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
import dev.coretide.plugin.armor.configurator.ApiCompatibilityConfigurator
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** `apiBaseline` (japicmp against a release) and `kotlinAbiValidation` (a committed Kotlin ABI dump). */
class ApiCompatibilityTest {
    /** A library published as 1.0.0 to a repository in [repo], then at 1.1.0 compared with it. */
    private fun writeLibrary(
        dir: File,
        repo: File,
        version: String,
        armorConfig: String = "",
    ) {
        ArmorTestFixture.writeProject(
            dir,
            armorConfig = armorConfig,
            extraPlugins = listOf("java-library", "maven-publish"),
            extraScript =
                """
                group = "com.example"
                version = "$version"

                repositories {
                    maven { url = uri("${repo.invariantSeparatorsPath}") }
                }

                publishing {
                    publications {
                        create<MavenPublication>("library") {
                            from(components["java"])
                        }
                    }
                    repositories {
                        maven {
                            name = "local"
                            url = uri("${repo.invariantSeparatorsPath}")
                        }
                    }
                }
                """.trimIndent(),
        )
        dir.resolve("settings.gradle.kts").writeText("rootProject.name = \"calculator\"\n")
    }

    private fun writeCalculator(
        dir: File,
        methods: String,
    ) {
        dir.resolve("src/main/java/com/example/Sample.java").delete()
        dir.resolve("src/main/java/com/example/Calculator.java").writeText(
            """
            package com.example;

            public class Calculator {
            $methods
            }
            """.trimIndent(),
        )
    }

    private val add = "    public int add(int a, int b) { return a + b; }"
    private val subtract = "    public int subtract(int a, int b) { return a - b; }"

    @Test
    fun `the API check fails on a removed method and passes an added one`(
        @TempDir dir: File,
        @TempDir repo: File,
    ) {
        writeLibrary(dir, repo, "1.0.0")
        writeCalculator(dir, "$add\n$subtract")
        ArmorTestFixture.run(dir, "publishAllPublicationsToLocalRepository")

        writeLibrary(dir, repo, "1.1.0", armorConfig = "    apiBaseline = \"1.0.0\"")
        writeCalculator(dir, "$add\n$subtract\n    public int negate(int a) { return -a; }")
        // An added method is compatible. The configuration cache must not object to the baseline's resolution.
        ArmorTestFixture.run(dir, "armorApiCheck", "--configuration-cache")

        writeCalculator(dir, add)
        val failure = ArmorTestFixture.runAndFail(dir, "armorApiCheck")

        assertContains(failure.output, "Detected binary changes.")
        assertContains(dir.resolve("build/reports/japicmp/api.txt").readText(), "subtract")
        assertTrue(dir.resolve("build/reports/japicmp/api.html").isFile)
    }

    @Test
    fun `the Kotlin ABI check fails on a public API change until the dump is updated`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, language = Language.KOTLIN, armorConfig = "    kotlinAbiValidation = true")

        ArmorTestFixture.run(dir, "updateLegacyAbi")
        assertTrue(dir.resolve("api").listFiles().orEmpty().isNotEmpty(), "no ABI dump written")
        ArmorTestFixture.run(dir, "codeQuality")

        dir.resolve("src/main/kotlin/com/example/Extra.kt").writeText("package com.example\n\npublic fun extra(value: Int): Int = value + 1\n")
        val failure = ArmorTestFixture.runAndFail(dir, "codeQuality")

        assertContains(failure.output, "Execution failed for task ':checkLegacyAbi'")
        assertContains(failure.output, "ABI has changed")
    }

    @Test
    fun `a baseline is a version or full coordinates`() {
        assertEquals("com.example:lib:1.0.0", ApiCompatibilityConfigurator.baselineCoordinates("1.0.0", "com.example", "lib"))
        assertEquals("org.acme:core:2.1", ApiCompatibilityConfigurator.baselineCoordinates("org.acme:core:2.1", "com.example", "lib"))
    }
}
