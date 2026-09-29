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

/** The root of a multi-module build gets one coverage report and one test report for all modules. */
class MultiModuleReportsTest {
    @Test
    fun `allCodeQuality writes one coverage report that counts tests in other modules`(
        @TempDir dir: File,
    ) {
        writeBuild(dir)

        ArmorTestFixture.run(dir, "allCodeQuality")

        val xml = dir.resolve("build/reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml").readText()
        // Both modules' classes, and Calculator is covered although only module-b's test calls it.
        assertContains(xml, """<class name="com/example/a/Calculator"""")
        assertContains(xml, """<class name="com/example/b/Greeter"""")
        assertTrue(coveredLines(xml, "com/example/a/Calculator") > 0, "Calculator has no covered lines")
        assertTrue(dir.resolve("build/reports/jacoco/testCodeCoverageReport/html/index.html").isFile)
    }

    @Test
    fun `the combined coverage report leaves out what the module reports leave out`(
        @TempDir dir: File,
    ) {
        writeBuild(dir)
        // "Config" is one of the default coverage exclusions.
        dir.resolve("module-a/src/main/java/com/example/a/AppConfig.java").writeText(
            "package com.example.a;\n\npublic class AppConfig {\n    public int port() {\n        return 8080;\n    }\n}\n",
        )

        ArmorTestFixture.run(dir, "testCodeCoverageReport")

        val xml = dir.resolve("build/reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml").readText()
        assertFalse(xml.contains("AppConfig"), "an excluded class is in the combined report")
        assertContains(xml, "com/example/a/Calculator")
    }

    @Test
    fun `allCodeQuality writes one test report for all modules`(
        @TempDir dir: File,
    ) {
        writeBuild(dir)

        ArmorTestFixture.run(dir, "allCodeQuality")

        val report = dir.resolve("build/reports/tests/test/aggregated-results")
        assertTrue(report.resolve("index.html").isFile, "no combined test report in $report")
        val html = report.walkTopDown().filter { it.extension == "html" }.joinToString("\n") { it.readText() }
        assertContains(html, "CalculatorTest")
        assertContains(html, "GreeterTest")
    }

    @Test
    fun `jacoco = false leaves out the combined coverage report, not the test report`(
        @TempDir dir: File,
    ) {
        writeBuild(dir, armorConfig = "    jacoco = false")

        val result = ArmorTestFixture.run(dir, "allCodeQuality", "--dry-run")

        assertFalse(result.output.contains(":testCodeCoverageReport"), "coverage was aggregated with jacoco off")
        assertContains(result.output, ":testAggregateTestReport")
    }

    @Test
    fun `each module's SonarQube analysis reads the combined coverage report`(
        @TempDir dir: File,
    ) {
        writeBuild(dir)
        dir.resolve("module-a/build.gradle.kts").appendText(
            """

            tasks.register("printCoveragePaths") {
                doLast {
                    val sonarTask = project.tasks.getByName("sonar") as org.sonarqube.gradle.SonarTask
                    println("COVERAGE_PATHS " + sonarTask.properties.get()["sonar.coverage.jacoco.xmlReportPaths"])
                }
            }
            """.trimIndent(),
        )

        val result = ArmorTestFixture.run(dir, ":module-a:printCoveragePaths")

        val paths = result.output.lineSequence().first { it.startsWith("COVERAGE_PATHS ") }.removePrefix("COVERAGE_PATHS ").split(",")
        val combined = dir.resolve("build/reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml").canonicalPath
        assertTrue(paths.any { File(it).isAbsolute && File(it).canonicalPath == combined }, "$paths does not include $combined")
    }

    @Test
    fun `a module without the Java plugin does not break the combined reports`(
        @TempDir dir: File,
    ) {
        writeBuild(dir, extraModules = listOf("docs"))
        dir.resolve("docs").mkdirs()
        dir.resolve("docs/build.gradle.kts").writeText("plugins {\n    base\n}\n")

        ArmorTestFixture.run(dir, "allCodeQuality")

        assertTrue(dir.resolve("build/reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml").isFile)
    }

    @Test
    fun `the combined reports reuse the configuration cache on a second run`(
        @TempDir dir: File,
    ) {
        writeBuild(dir)

        ArmorTestFixture.run(dir, "testCodeCoverageReport", "testAggregateTestReport", "--configuration-cache")
        val second = ArmorTestFixture.run(dir, "testCodeCoverageReport", "testAggregateTestReport", "--configuration-cache")

        assertContains(second.output, "Configuration cache entry reused")
    }

    /** The LINE counter's covered count for [className] in a JaCoCo XML report. */
    private fun coveredLines(
        xml: String,
        className: String,
    ): Int {
        val classXml = xml.substringAfter("""<class name="$className"""").substringBefore("</class>")
        val lineCounters = Regex("""<counter type="LINE" missed="\d+" covered="(\d+)"/>""").findAll(classXml).toList()
        return lineCounters.lastOrNull()?.groupValues?.get(1)?.toInt() ?: 0
    }

    /**
     * module-a has a Calculator and its own test; module-b depends on module-a and its test also calls
     * Calculator. Coverage thresholds are off: the fixtures test the reports, not the thresholds.
     */
    private fun writeBuild(
        dir: File,
        armorConfig: String = "",
        extraModules: List<String> = emptyList(),
    ) {
        ArmorTestFixture.writeMultiModuleProject(
            dir,
            modules = listOf("module-a", "module-b") + extraModules,
            armorConfig = "    spotbugs = false\n    coverageMinimum = 0.0\n    coverageClassMinimum = 0.0\n$armorConfig",
        )
        extraModules.forEach { dir.resolve(it).deleteRecursively() }
        val junit =
            """

            dependencies {
                testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
                testRuntimeOnly("org.junit.platform:junit-platform-launcher")
            }
            """.trimIndent()
        dir.resolve("module-a/build.gradle.kts").appendText(junit)
        dir.resolve("module-b/build.gradle.kts").appendText(junit + "\n\ndependencies {\n    implementation(project(\":module-a\"))\n}\n")

        write(dir, "module-a/src/main/java/com/example/a/Calculator.java", "package com.example.a;\n\npublic class Calculator {\n    public int add(int a, int b) {\n        return a + b;\n    }\n}\n")
        write(
            dir,
            "module-a/src/test/java/com/example/a/CalculatorTest.java",
            "package com.example.a;\n\nimport org.junit.jupiter.api.Test;\n\nclass CalculatorTest {\n    @Test\n    void constructs() {\n        new Calculator();\n    }\n}\n",
        )
        write(
            dir,
            "module-b/src/main/java/com/example/b/Greeter.java",
            "package com.example.b;\n\nimport com.example.a.Calculator;\n\npublic class Greeter {\n    public String greet() {\n        return \"sum \" + new Calculator().add(1, 2);\n    }\n}\n",
        )
        write(
            dir,
            "module-b/src/test/java/com/example/b/GreeterTest.java",
            "package com.example.b;\n\nimport org.junit.jupiter.api.Test;\nimport static org.junit.jupiter.api.Assertions.assertEquals;\n\n" +
                "class GreeterTest {\n    @Test\n    void greets() {\n        assertEquals(\"sum 3\", new Greeter().greet());\n    }\n}\n",
        )
    }

    private fun write(
        dir: File,
        path: String,
        content: String,
    ) {
        dir.resolve(path).apply { parentFile.mkdirs() }.writeText(content)
    }
}
