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

import com.github.spotbugs.snom.SpotBugsTask
import dev.coretide.plugin.armor.task.ArmorReportTask
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

/** `armorReport`, the summary page. Once per build; `codeQuality` and `fullAnalysis` end with it. */
object ArmorReportConfigurator {
    const val TASK_NAME = "armorReport"

    /** The tasks whose reports the summary reads, besides tests and SpotBugs. */
    private val REPORTING_TASKS =
        setOf(
            "jacocoTestReport",
            "koverXmlReport",
            "koverHtmlReport",
            "testCodeCoverageReport",
            DetektConfigurator.TASK_NAME,
            MutationTestingConfigurator.TASK_NAME,
            "dependencyCheckAnalyze",
            DependencyHealthConfigurator.UPDATES_TASK,
            DependencyHealthConfigurator.LICENSE_TASK,
            ApiCompatibilityConfigurator.API_CHECK_TASK,
        )

    /** The tasks that end with the summary. */
    private val SUMMARIZED_TASKS = setOf("codeQuality", "fullAnalysis", "allCodeQuality")

    /** Registers the task on [root], summing up [projects]. */
    fun register(
        root: Project,
        projects: List<Project>,
    ) {
        val report =
            root.tasks.register(TASK_NAME, ArmorReportTask::class.java) { task ->
                task.group = "verification"
                task.description = "📋 Sums up the checks' reports on one page"
                projects.forEach { project ->
                    task.projectPaths.add(project.path)
                    task.buildDirectories.add(project.layout.buildDirectory)
                    // It reads what these write, when they run in the same build.
                    task.mustRunAfter(project.tasks.withType(Test::class.java))
                    task.mustRunAfter(project.tasks.withType(SpotBugsTask::class.java))
                    task.mustRunAfter(project.tasks.named { it in REPORTING_TASKS })
                }
                task.report.set(root.layout.buildDirectory.file("reports/codearmor/index.html"))
            }
        projects.forEach { project ->
            project.tasks.named { it in SUMMARIZED_TASKS }.configureEach { it.finalizedBy(report) }
        }
    }
}
