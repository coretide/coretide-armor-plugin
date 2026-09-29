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
import dev.coretide.plugin.armor.task.ArmorReportTask
import dev.coretide.plugin.armor.util.ReportSummary
import dev.coretide.plugin.armor.util.ReportSummary.Row
import dev.coretide.plugin.armor.util.ReportSummary.Status
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** The `armorReport` summary page, and the ArchUnit scaffold. */
class ArmorReportTest {
    private val junit =
        """
        dependencies {
            testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
            testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        }
        """.trimIndent()

    private fun writeJavaTest(
        dir: File,
        body: String = "@Test\nvoid adds() {\n    org.junit.jupiter.api.Assertions.assertEquals(3, new Sample().add(1, 2));\n}",
    ) {
        val tests = dir.resolve("src/test/java/com/example").apply { mkdirs() }
        tests.resolve("SampleTest.java").writeText(
            "package com.example;\n\nimport org.junit.jupiter.api.Test;\n\nclass SampleTest {\n${body.prependIndent("    ")}\n}\n",
        )
    }

    @Test
    fun `build ends with a summary of the checks, on the console and on a page`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, extraScript = junit)
        writeJavaTest(dir)

        val result = ArmorTestFixture.run(dir, "build")

        assertContains(result.output, "📋 CodeArmor summary: ")
        assertContains(result.output, "✅ Tests: 1 tests")
        assertContains(result.output, "✅ Coverage (JaCoCo): 100.0% of lines")
        assertContains(result.output, "✅ SpotBugs: no findings")
        val page = dir.resolve("build/reports/codearmor/index.html").readText()
        assertContains(page, "<a href=\"../tests/test/index.html\">index.html</a>")
        assertContains(page, "<a href=\"../spotbugs/spotbugsMain.html\">spotbugsMain.html</a>")
    }

    @Test
    fun `on GitHub Actions the summary also goes to the job summary`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, extraScript = junit)
        writeJavaTest(dir)
        val summary = dir.resolve("step-summary.md")

        ArmorTestFixture.runWithEnvironment(dir, "build", set = mapOf("GITHUB_STEP_SUMMARY" to summary.absolutePath))

        val text = summary.readText()
        assertContains(text, "### 🛡️ CodeArmor summary")
        assertContains(text, "| ✅ | Tests | 1 tests |")
        assertContains(text, "| ✅ | SpotBugs | no findings |")
    }

    @Test
    fun `the job summary keeps a table cell in its cell`() {
        val markdown = ArmorReportTask.markdown(listOf(":a" to listOf(Row("x|y", Status.OK, "<b>\nnext", null))), headings = true)

        assertContains(markdown, "**:a**")
        assertContains(markdown, "| ✅ | x\\|y | &lt;b> next |")
    }

    @Test
    fun `a multi-module summary has a section per module`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeMultiModuleProject(dir)

        val result = ArmorTestFixture.run(dir, "allCodeQuality", "--configuration-cache")

        val page = dir.resolve("build/reports/codearmor/index.html").readText()
        assertContains(page, "<h2>:module-a</h2>")
        assertContains(page, "<h2>:module-b</h2>")
        assertContains(result.output, "   :module-a")
    }

    @Test
    fun `before any check runs the summary says so`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir)

        val result = ArmorTestFixture.run(dir, "armorReport")

        assertContains(result.output, "no reports yet")
        assertContains(dir.resolve("build/reports/codearmor/index.html").readText(), "No reports yet")
    }

    @Test
    fun `each tool's report becomes one line`(
        @TempDir build: File,
    ) {
        build.resolve("reports/jacoco/test").mkdirs()
        build.resolve("reports/jacoco/test/jacocoTestReport.xml").writeText(
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <!DOCTYPE report PUBLIC "-//JACOCO//DTD Report 1.1//EN" "report.dtd">
            <report name="x"><package name="p"><counter type="LINE" missed="9" covered="1"/></package>
            <counter type="LINE" missed="1" covered="3"/><counter type="BRANCH" missed="1" covered="1"/></report>
            """.trimIndent(),
        )
        build.resolve("reports/spotbugs").mkdirs()
        build.resolve("reports/spotbugs/spotbugsMain.xml").writeText("<BugCollection><BugInstance/><BugInstance/></BugCollection>")
        build.resolve("reports/detekt").mkdirs()
        build.resolve("reports/detekt/detekt.xml").writeText("<checkstyle><file><error/></file></checkstyle>")
        build.resolve("reports/pitest").mkdirs()
        build.resolve("reports/pitest/mutations.xml").writeText(
            "<mutations><mutation detected=\"true\"/><mutation detected=\"false\"/><mutation detected=\"true\"/><mutation detected=\"true\"/></mutations>",
        )
        build.resolve("reports/dependency-check").mkdirs()
        build.resolve("reports/dependency-check/dependency-check-report.json").writeText(
            """{"dependencies":[{"vulnerabilities":[{},{}]},{"vulnerabilities":[]},{}]}""",
        )
        build.resolve("dependencyUpdates").mkdirs()
        build.resolve("dependencyUpdates/report.json").writeText("""{"outdated":{"count":3}}""")
        build.resolve("reports/japicmp").mkdirs()
        build.resolve("reports/japicmp/api.txt").writeText("Comparing binary compatibility of a.jar against b.jar\n***! MODIFIED CLASS")

        val rows = ReportSummary.rows(build).associateBy { it.tool }

        assertEquals("75.0% of lines, 50.0% of branches", rows.getValue("Coverage (JaCoCo)").result)
        assertEquals(Row("SpotBugs", Status.WARNING, "2 findings", null), rows.getValue("SpotBugs"))
        assertEquals("1 findings", rows.getValue("detekt").result)
        assertEquals("75.0% of 4 mutations killed", rows.getValue("Mutation testing (PIT)").result)
        assertEquals("2 vulnerabilities in 1 dependencies", rows.getValue("Vulnerabilities (OWASP)").result)
        assertEquals(Row("Dependency updates", Status.WARNING, "3 dependencies have a newer release", null), rows.getValue("Dependency updates"))
        assertEquals(Status.FAILED, rows.getValue("API compatibility").status)
        assertNull(rows["Tests"], "no test results, so no line")
    }

    @Test
    fun `the page escapes what it shows`(
        @TempDir dir: File,
    ) {
        val page = ArmorReportTask.render(listOf(":a<b>" to listOf(Row("x&y", Status.OK, "\"quoted\"", null))), dir)

        assertContains(page, "x&amp;y")
        assertContains(page, "&quot;quoted&quot;")
        assertFalse(page.contains("<b>"))
    }

    @Test
    fun `architectureTests scaffolds an ArchUnit test that fails on a violation`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, armorConfig = "    architectureTests = true", extraScript = junit)
        ArmorTestFixture.run(dir, "armorScaffoldArchitectureTests")
        val test = dir.resolve("src/test/java/com/example/ArchitectureTest.java")
        assertTrue(test.isFile)
        ArmorTestFixture.run(dir, "test")

        dir.resolve("src/main/java/com/example/Noisy.java").writeText(
            "package com.example;\n\npublic class Noisy {\n    public void speak() {\n        System.out.println(\"hi\");\n    }\n}\n",
        )
        val failure = ArmorTestFixture.runAndFail(dir, "test")

        assertContains(failure.output, "ArchitectureTest")
        assertContains(failure.output, "noStandardStreams")
    }

    @Test
    fun `a Kotlin project gets a Kotlin architecture test`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, language = Language.KOTLIN, armorConfig = "    architectureTests = true", extraScript = junit)

        ArmorTestFixture.run(dir, "armorScaffoldArchitectureTests")
        // detekt checks the scaffolded test too.
        val result = ArmorTestFixture.run(dir, "detekt", "test")

        val test = dir.resolve("src/test/kotlin/com/example/ArchitectureTest.kt")
        assertNotNull(test.takeIf { it.isFile })
        assertContains(test.readText(), "@AnalyzeClasses(packages = [\"com.example\"]")
        assertFalse(result.output.contains("FAILED"))
    }
}
