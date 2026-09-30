/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.configurator

import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.task.ListJacocoAntTask
import dev.coretide.plugin.armor.util.ExclusionUtil
import dev.coretide.plugin.armor.util.LogUtil
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.reporting.ReportSpec
import org.gradle.api.reporting.ReportingExtension
import org.gradle.api.tasks.testing.AggregateTestReport
import org.gradle.testing.jacoco.plugins.JacocoCoverageReport
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.File

/**
 * One coverage report and one test report for a whole multi-module build, on its root project, through
 * Gradle's own aggregation plugins. Coverage counts tests in one module that exercise another's code.
 */
object AggregatedReportsConfigurator {
    const val COVERAGE_REPORT = "testCodeCoverageReport"
    const val TEST_REPORT = "testAggregateTestReport"
    private const val TEST_SUITE = "test"

    /**
     * JaCoCo's reporting library, handed from a module to the root. The root need not be able to download
     * anything: many builds declare their repositories only in the modules.
     */
    private const val JACOCO_ANT = "codeArmorJacocoAnt"
    private const val JACOCO_ANT_ELEMENTS = "codeArmorJacocoAntElements"
    private const val JACOCO_ANT_USAGE = "codearmor-jacoco-ant"

    /** Registers the reports on [root]; modules join through [addModule]. Returns the report tasks. */
    fun configure(
        root: Project,
        extension: CodeArmorExtension,
    ): List<String> {
        val reportTasks = mutableListOf<String>()
        root.pluginManager.apply("test-report-aggregation")
        registerIfAbsent(root, TEST_REPORT, AggregateTestReport::class.java) { it.testSuiteName.set(TEST_SUITE) }
        reportTasks += TEST_REPORT

        // Kover projects have their own coverage data, not JaCoCo's.
        if (extension.coverage.enabled.get() && !extension.coverage.kover.get()) {
            root.pluginManager.apply("jacoco-report-aggregation")
            registerIfAbsent(root, COVERAGE_REPORT, JacocoCoverageReport::class.java) { it.testSuiteName.set(TEST_SUITE) }
            val jacocoAnt =
                root.configurations.create(JACOCO_ANT) { configuration ->
                    configuration.isCanBeConsumed = false
                    configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, root.objects.named(Usage::class.java, JACOCO_ANT_USAGE))
                }
            val exclusions = ExclusionUtil.generateJacocoReportExclusions(extension)
            root.tasks.named(COVERAGE_REPORT, JacocoReport::class.java).configure { report ->
                report.reports { reports ->
                    reports.xml.required.set(true)
                    reports.html.required.set(true)
                }
                // The module hands over a list of the jars' paths in Gradle's cache; see ListJacocoAntTask.
                val listings = jacocoAnt.incoming.files
                report.jacocoClasspath =
                    root
                        .files(listings.elements.map { lists -> lists.flatMap { jars(it.asFile) } })
                        .builtBy(listings)
                // The same exclusions as each module's own report, applied without resolving anything yet.
                val classes = root.files(*report.classDirectories.from.toTypedArray())
                report.classDirectories.setFrom(classes.asFileTree.matching { it.exclude(exclusions) })
            }
            reportTasks += COVERAGE_REPORT
        }
        return reportTasks
    }

    /**
     * Adds a configured module to the reports. Only modules with the Java plugin publish test results and
     * coverage data; a docs or aggregator module is left out.
     */
    fun addModule(
        root: Project,
        module: Project,
        extension: CodeArmorExtension,
    ) {
        if (!module.plugins.hasPlugin(JavaPlugin::class.java)) return
        root.dependencies.add("testReportAggregation", module)
        if (!extension.coverage.enabled.get() || extension.coverage.kover.get()) return
        root.dependencies.add("jacocoAggregation", module)
        val jacocoAnt = root.configurations.getByName(JACOCO_ANT)
        if (jacocoAnt.dependencies.isEmpty()) {
            publishJacocoAnt(module)
            root.dependencies.add(JACOCO_ANT, module)
            LogUtil.verbose("📊 The combined coverage report takes JaCoCo from ${module.path}")
        }
    }

    /** The aggregated coverage XML, which SonarQube reads in every module of a multi-module build. */
    fun coverageXml(root: Project): File =
        root.layout.buildDirectory
            .file("reports/jacoco/$COVERAGE_REPORT/$COVERAGE_REPORT.xml")
            .get()
            .asFile

    /** The module's own, resolved JaCoCo reporting library, listed in a variant the root can depend on. */
    private fun publishJacocoAnt(module: Project) {
        val listing =
            module.tasks.register("codeArmorJacocoAnt", ListJacocoAntTask::class.java) { task ->
                task.description = "Lists JaCoCo's reporting library for the combined coverage report"
                task.library.from(module.configurations.named("jacocoAnt"))
                task.listing.set(module.layout.buildDirectory.file("codearmor/jacoco-ant.txt"))
            }
        module.configurations.create(JACOCO_ANT_ELEMENTS) { configuration ->
            configuration.isCanBeResolved = false
            configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, module.objects.named(Usage::class.java, JACOCO_ANT_USAGE))
            configuration.outgoing.artifact(listing)
        }
    }

    private fun jars(listing: File): List<File> = listing.readLines().filter { it.isNotBlank() }.map(::File)

    private fun <T : ReportSpec> registerIfAbsent(
        root: Project,
        name: String,
        type: Class<T>,
        configure: (T) -> Unit,
    ) {
        // A root with its own tests already has these reports from Gradle.
        val reports = root.extensions.getByType(ReportingExtension::class.java).reports
        if (name !in reports.names) {
            reports.register(name, type) { configure(it) }
        }
    }
}
