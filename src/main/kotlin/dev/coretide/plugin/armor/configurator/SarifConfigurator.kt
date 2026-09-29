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
import dev.coretide.plugin.armor.task.SarifReportTask
import dev.coretide.plugin.armor.task.ScaffoldProjectTask
import dev.coretide.plugin.armor.task.SecretScanTask
import org.gradle.api.Project

/** `armorSarifReport`, which gathers the tools' SARIF reports, and `armorScaffoldProject`. Once per build. */
object SarifConfigurator {
    const val SARIF_TASK = "armorSarifReport"
    const val SCAFFOLD_TASK = "armorScaffoldProject"

    /** Where the tools write SARIF, under each project's `build/reports`. */
    val REPORT_PATTERNS = listOf("spotbugs/*.sarif", "detekt/*.sarif", "dependency-check/*.sarif", "gitleaks/*.sarif")

    /** Registers the tasks on [root], gathering the reports of [projects]. */
    fun register(
        root: Project,
        projects: List<Project>,
    ) {
        root.tasks.register(SARIF_TASK, SarifReportTask::class.java) { task ->
            task.group = "verification"
            task.description = "🧾 Gathers the SpotBugs, detekt, OWASP and gitleaks SARIF reports for code scanning"
            projects.forEach { project ->
                task.reports.from(
                    project.layout.buildDirectory.dir("reports").map { reports ->
                        reports.asFileTree.matching { it.include(REPORT_PATTERNS) }
                    },
                )
                // It reads what these write, when they run in the same build.
                task.mustRunAfter(project.tasks.withType(SpotBugsTask::class.java))
                task.mustRunAfter(
                    project.tasks.named {
                        it == DetektConfigurator.TASK_NAME || it == "dependencyCheckAnalyze" || it == SecretScanTask.TASK_NAME
                    },
                )
            }
            task.rootDirectory.set(root.layout.projectDirectory)
            task.outputDirectory.set(root.layout.buildDirectory.dir("reports/sarif"))
        }
        root.tasks.register(SCAFFOLD_TASK, ScaffoldProjectTask::class.java) { task ->
            task.gradleRootDirectory.set(root.rootDir)
        }
    }
}
