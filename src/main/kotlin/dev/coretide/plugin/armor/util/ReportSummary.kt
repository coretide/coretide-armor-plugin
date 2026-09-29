/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.util

import groovy.json.JsonSlurper
import org.w3c.dom.Element
import java.io.File
import java.util.Locale

/**
 * One line per tool, read from the reports the tools left in a project's build directory. A tool that has not
 * run has no line. Nothing here runs a tool.
 */
object ReportSummary {
    enum class Status(
        val icon: String,
    ) {
        OK("✅"),
        WARNING("⚠️"),
        FAILED("❌"),
    }

    data class Row(
        val tool: String,
        val status: Status,
        val result: String,
        val report: File?,
    )

    fun rows(buildDir: File): List<Row> =
        listOfNotNull(
            tests(buildDir),
            tests(buildDir, "integrationTest", "Integration tests"),
            coverage(buildDir),
            diffCoverage(buildDir),
            spotbugs(buildDir),
            detekt(buildDir),
            mutations(buildDir),
            vulnerabilities(buildDir),
            updates(buildDir),
            licences(buildDir),
            api(buildDir),
        )

    /** A test task's results; `test`, the unit tests, by default. */
    fun tests(
        buildDir: File,
        task: String = "test",
        tool: String = "Tests",
    ): Row? {
        val executions = TestResults.read(File(buildDir, "test-results/$task"))
        if (executions.isEmpty()) return null
        val byTest = executions.groupBy { it.id }
        val failed = byTest.count { (_, runs) -> runs.none { !it.failed } }
        val flaky = TestResults.flaky(executions).size
        val result =
            buildList {
                add("${byTest.size} tests")
                if (failed > 0) add("$failed failed")
                if (flaky > 0) add("$flaky flaky")
            }.joinToString(", ")
        val status = if (failed > 0) Status.FAILED else if (flaky > 0) Status.WARNING else Status.OK
        return Row(tool, status, result, File(buildDir, "reports/tests/$task/index.html").takeIf { it.isFile })
    }

    /** JaCoCo's report, the combined one of a multi-module root, or Kover's, which uses the same XML format. */
    fun coverage(buildDir: File): Row? {
        val (tool, xml, html) =
            listOf(
                Triple("Coverage (JaCoCo)", "reports/jacoco/test/jacocoTestReport.xml", "reports/jacoco/test/html/index.html"),
                Triple(
                    "Coverage (all modules)",
                    "reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml",
                    "reports/jacoco/testCodeCoverageReport/html/index.html",
                ),
                Triple("Coverage (Kover)", "reports/kover/report.xml", "reports/kover/html/index.html"),
            ).firstOrNull { File(buildDir, it.second).isFile } ?: return null
        val report = XmlReports.parse(File(buildDir, xml)).documentElement
        val counters = XmlReports.children(report, "counter").associateBy { it.getAttribute("type") }
        fun percent(type: String): String? {
            val counter = counters[type] ?: return null
            val covered = counter.getAttribute("covered").toLong()
            val total = covered + counter.getAttribute("missed").toLong()
            return if (total == 0L) null else String.format(Locale.ROOT, "%.1f%%", covered * 100.0 / total)
        }
        val result = listOfNotNull(percent("LINE")?.let { "$it of lines" }, percent("BRANCH")?.let { "$it of branches" })
        return Row(tool, Status.OK, result.joinToString(", ").ifEmpty { "nothing to measure" }, File(buildDir, html).takeIf { it.isFile })
    }

    /** What `armorDiffCoverage` measured, when it found a base branch. */
    fun diffCoverage(buildDir: File): Row? {
        val json = File(buildDir, "reports/codearmor/diff-coverage.json").takeIf { it.isFile } ?: return null
        val result = JsonSlurper().parse(json) as? Map<*, *> ?: return null
        val base = result["base"]
        val lines = (result["lines"] as? Number)?.toInt() ?: 0
        if (lines == 0) return Row("Diff coverage", Status.OK, "no changed lines with code since $base", null)
        val share = ((result["covered"] as? Number)?.toInt() ?: 0).toDouble() / lines
        val minimum = (result["minimum"] as? Number)?.toDouble()
        val status = if (minimum != null && share < minimum) Status.FAILED else Status.OK
        val percent = String.format(Locale.ROOT, "%.1f%%", share * 100)
        return Row("Diff coverage", status, "$percent of $lines changed lines since $base", null)
    }

    fun spotbugs(buildDir: File): Row? {
        val xml = File(buildDir, "reports/spotbugs/spotbugsMain.xml").takeIf { it.isFile } ?: return null
        val findings = XmlReports.parse(xml).getElementsByTagName("BugInstance").length
        return findings("SpotBugs", findings, File(buildDir, "reports/spotbugs/spotbugsMain.html"))
    }

    /** `detekt`'s report, or `detektMain`'s with type resolution: the newer, when both are there. */
    fun detekt(buildDir: File): Row? {
        val name =
            listOf("detekt", "main")
                .filter { File(buildDir, "reports/detekt/$it.xml").isFile }
                .maxByOrNull { File(buildDir, "reports/detekt/$it.xml").lastModified() } ?: return null
        val findings = XmlReports.parse(File(buildDir, "reports/detekt/$name.xml")).getElementsByTagName("error").length
        return findings("detekt", findings, File(buildDir, "reports/detekt/$name.html"))
    }

    fun mutations(buildDir: File): Row? {
        val xml = File(buildDir, "reports/pitest/mutations.xml").takeIf { it.isFile } ?: return null
        val mutations = XmlReports.parse(xml).getElementsByTagName("mutation")
        val total = mutations.length
        val detected = (0 until total).count { (mutations.item(it) as Element).getAttribute("detected") == "true" }
        val result = if (total == 0) "no mutations" else String.format(Locale.ROOT, "%.1f%% of %d mutations killed", detected * 100.0 / total, total)
        return Row("Mutation testing (PIT)", Status.OK, result, File(buildDir, "reports/pitest/index.html").takeIf { it.isFile })
    }

    fun vulnerabilities(buildDir: File): Row? {
        val json = File(buildDir, "reports/dependency-check/dependency-check-report.json").takeIf { it.isFile } ?: return null
        val dependencies = (JsonSlurper().parse(json) as? Map<*, *>)?.get("dependencies") as? List<*> ?: emptyList<Any?>()
        val vulnerabilities = dependencies.sumOf { ((it as? Map<*, *>)?.get("vulnerabilities") as? List<*>)?.size ?: 0 }
        val vulnerable = dependencies.count { (((it as? Map<*, *>)?.get("vulnerabilities") as? List<*>)?.size ?: 0) > 0 }
        val status = if (vulnerabilities > 0) Status.WARNING else Status.OK
        val result = if (vulnerabilities == 0) "no known vulnerabilities" else "$vulnerabilities vulnerabilities in $vulnerable dependencies"
        val html = File(buildDir, "reports/dependency-check/dependency-check-report.html")
        return Row("Vulnerabilities (OWASP)", status, result, html.takeIf { it.isFile })
    }

    fun updates(buildDir: File): Row? {
        val json = File(buildDir, "dependencyUpdates/report.json").takeIf { it.isFile } ?: return null
        val outdated = ((JsonSlurper().parse(json) as? Map<*, *>)?.get("outdated") as? Map<*, *>)?.get("count") as? Number
        val count = outdated?.toInt() ?: 0
        val result = if (count == 0) "all dependencies up to date" else "$count dependencies have a newer release"
        return Row("Dependency updates", if (count == 0) Status.OK else Status.WARNING, result, File(buildDir, "dependencyUpdates/report.html").takeIf { it.isFile })
    }

    fun licences(buildDir: File): Row? {
        val report = File(buildDir, "reports/codearmor/licenses.txt").takeIf { it.isFile } ?: return null
        val bom = File(buildDir, "reports/cyclonedx/bom.xml").takeIf { it.isFile } ?: return null
        val components = SbomLicenses.read(bom)
        val byLicense = SbomLicenses.byLicense(components)
        val unknown = byLicense[SbomLicenses.UNKNOWN]?.size ?: 0
        val result =
            "${components.size} dependencies, ${byLicense.keys.count { it != SbomLicenses.UNKNOWN }} licences" +
                if (unknown > 0) ", $unknown without licence information" else ""
        return Row("Licences", if (unknown > 0) Status.WARNING else Status.OK, result, report)
    }

    fun api(buildDir: File): Row? {
        val txt = File(buildDir, "reports/japicmp/api.txt").takeIf { it.isFile } ?: return null
        val compatible = txt.readText().contains("No changes.")
        val result = if (compatible) "no binary incompatible changes" else "binary incompatible changes"
        return Row("API compatibility", if (compatible) Status.OK else Status.FAILED, result, File(buildDir, "reports/japicmp/api.html").takeIf { it.isFile })
    }

    private fun findings(
        tool: String,
        count: Int,
        html: File,
    ) = Row(tool, if (count == 0) Status.OK else Status.WARNING, if (count == 0) "no findings" else "$count findings", html.takeIf { it.isFile })
}
