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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** detekt in Kotlin projects: part of the build tier, with a baseline for findings already in the code. */
class DetektTest {
    @Test
    fun `a Kotlin project runs detekt in the build tier and writes its reports`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, language = Language.KOTLIN, armorConfig = FOCUS_ON_DETEKT)

        val result = ArmorTestFixture.run(dir, "codeQuality")

        assertEquals(TaskOutcome.SUCCESS, result.task(":detekt")?.outcome)
        listOf("detekt.xml", "detekt.html", "detekt.sarif").forEach { report ->
            assertTrue(dir.resolve("build/reports/detekt/$report").isFile, "$report is missing")
        }
    }

    @Test
    fun `a finding fails the build, and a baseline accepts the findings already there`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, language = Language.KOTLIN, armorConfig = FOCUS_ON_DETEKT)
        dir.resolve("src/main/kotlin/com/example/Legacy.kt").writeText("package com.example\n\npublic fun legacy() {\n}\n")

        val failure = ArmorTestFixture.runAndFail(dir, "codeQuality")
        assertContains(failure.output, "[EmptyFunctionBlock]")

        ArmorTestFixture.run(dir, "detektBaseline")
        assertContains(dir.resolve("detekt-baseline.xml").readText(), "EmptyFunctionBlock:Legacy.kt")
        assertEquals(TaskOutcome.SUCCESS, ArmorTestFixture.run(dir, "codeQuality").task(":detekt")?.outcome)

        // The baseline covers what was there, not what comes next. detekt identifies a finding by rule, file
        // and code, so the new one goes in a new file.
        dir.resolve("src/main/kotlin/com/example/Newer.kt").writeText("package com.example\n\npublic fun newer() {\n}\n")
        val newFinding = ArmorTestFixture.runAndFail(dir, "codeQuality").output.lineSequence()
        assertTrue(newFinding.any { "Newer.kt" in it && "[EmptyFunctionBlock]" in it }, "the new finding was not reported")
        assertFalse(newFinding.any { "Legacy.kt" in it && "[EmptyFunctionBlock]" in it }, "the baselined finding was reported")
    }

    @Test
    fun `config detekt yml overrides rules and keeps the other defaults`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, language = Language.KOTLIN, armorConfig = FOCUS_ON_DETEKT)
        dir.resolve("config/detekt").mkdirs()
        dir.resolve("config/detekt/detekt.yml").writeText("style:\n  MaxLineLength:\n    maxLineLength: 200\n")
        val longLine = "public val long: String = \"" + "x".repeat(130) + "\""
        dir.resolve("src/main/kotlin/com/example/Rules.kt").writeText("package com.example\n\n$longLine\n\npublic fun empty() {\n}\n")

        val output = ArmorTestFixture.runAndFail(dir, "detekt").output

        assertFalse(output.contains("[MaxLineLength]"), "the override in detekt.yml was ignored")
        assertContains(output, "[EmptyFunctionBlock]")
    }

    @Test
    fun `a Java project gets no detekt`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, armorConfig = FOCUS_ON_DETEKT)

        val result = ArmorTestFixture.run(dir, "codeQuality", "--dry-run")

        assertFalse(result.output.contains(":detekt"), "detekt ran in a Java project")
    }

    @Test
    fun `detekt = false leaves detekt out`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, language = Language.KOTLIN, armorConfig = "$FOCUS_ON_DETEKT\n    detekt = false")

        val result = ArmorTestFixture.run(dir, "codeQuality", "--dry-run", "tasks", "--all")

        assertFalse(result.output.contains(":detekt"), "detekt ran although it is off")
        assertFalse(result.output.lineSequence().any { it.startsWith("detekt ") }, "a detekt task exists although it is off")
    }

    @Test
    fun `a project that applies detekt 1 itself keeps it`(
        @TempDir dir: File,
    ) {
        // A stand-in for detekt 1.x, under its plugin ID: applying detekt 2 as well would clash on the task name.
        writeLegacyDetektPlugin(dir.resolve("buildSrc"))
        ArmorTestFixture.writeProject(
            dir,
            language = Language.KOTLIN,
            extraPlugins = listOf("io.gitlab.arturbosch.detekt"),
            armorConfig = FOCUS_ON_DETEKT,
        )

        val result = ArmorTestFixture.run(dir, "codeQuality")

        assertContains(result.output, "LEGACY DETEKT RAN")
    }

    @Test
    fun `SonarQube imports detekt's findings`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(
            dir,
            language = Language.KOTLIN,
            extraScript =
                """
                tasks.register("printDetektReportPath") {
                    doLast {
                        val sonarTask = project.tasks.getByName("sonar") as org.sonarqube.gradle.SonarTask
                        println("DETEKT_REPORT " + sonarTask.properties.get()["sonar.kotlin.detekt.reportPaths"])
                    }
                }
                """.trimIndent(),
        )

        val result = ArmorTestFixture.run(dir, "printDetektReportPath")

        val reportPath = result.output.lineSequence().first { it.startsWith("DETEKT_REPORT ") }.removePrefix("DETEKT_REPORT ")
        assertEquals(dir.resolve("build/reports/detekt/detekt.xml").canonicalPath, File(reportPath).canonicalPath)
    }

    @Test
    fun `a Kotlin project without findings needs no baseline file`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, language = Language.KOTLIN, armorConfig = FOCUS_ON_DETEKT)

        ArmorTestFixture.run(dir, "detekt")

        assertNull(dir.walkTopDown().firstOrNull { it.name.contains("baseline") && it.isFile }, "a baseline file appeared")
    }

    private fun writeLegacyDetektPlugin(buildSrc: File) {
        buildSrc.resolve("src/main/java").mkdirs()
        buildSrc.resolve("build.gradle.kts").writeText(
            """
            plugins {
                `java-gradle-plugin`
            }

            gradlePlugin {
                plugins {
                    create("legacyDetekt") {
                        id = "io.gitlab.arturbosch.detekt"
                        implementationClass = "LegacyDetektPlugin"
                    }
                }
            }
            """.trimIndent(),
        )
        buildSrc.resolve("src/main/java/LegacyDetektPlugin.java").writeText(
            """
            import org.gradle.api.Plugin;
            import org.gradle.api.Project;

            public class LegacyDetektPlugin implements Plugin<Project> {
                @Override
                public void apply(Project project) {
                    project.getTasks().register("detekt", task -> task.doLast(t -> System.out.println("LEGACY DETEKT RAN")));
                }
            }
            """.trimIndent(),
        )
    }

    private companion object {
        /** No SpotBugs, and no coverage thresholds for fixtures without tests. */
        const val FOCUS_ON_DETEKT = "    spotbugs = false\n    coverageMinimum = 0.0\n    coverageClassMinimum = 0.0"
    }
}
