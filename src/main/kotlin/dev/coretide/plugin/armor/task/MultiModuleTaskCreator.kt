/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.task

import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.ProjectType
import dev.coretide.plugin.armor.configurator.AggregatedReportsConfigurator
import dev.coretide.plugin.armor.configurator.ArmorReportConfigurator
import dev.coretide.plugin.armor.configurator.DependencyHealthConfigurator
import dev.coretide.plugin.armor.configurator.SarifConfigurator
import dev.coretide.plugin.armor.util.ConfiguratorUtil
import dev.coretide.plugin.armor.util.LogUtil
import dev.coretide.plugin.armor.util.ProjectDetector
import org.gradle.api.Project
import java.io.File

object MultiModuleTaskCreator {
    fun configureMultiModuleProject(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        val actualProjects =
            project.subprojects.filter { subproject ->
                subproject.name != "bom" &&
                    subproject.name != "examples" &&
                    subproject.buildFile.exists()
            }

        val aggregatedReports = AggregatedReportsConfigurator.configure(project, extension)
        DependencyHealthConfigurator.configureAnalysisRoot(project, extension)
        val aggregatedCoverage = if (extension.jacoco && !extension.kover) AggregatedReportsConfigurator.coverageXml(project) else null

        // The secret scan covers the repository: once, from the root, for every module's fullAnalysis.
        val secretScan = if (extension.secretScan) SecretScanTask.register(project) else null
        actualProjects.forEach { subproject ->
            subproject.afterEvaluate {
                val subProjectType = ProjectDetector.detectProjectType(subproject)
                configureSingleModuleProject(subproject, extension, subProjectType, aggregatedCoverage)
                AggregatedReportsConfigurator.addModule(project, subproject, extension)
                if (secretScan != null && SecretScanTask.TASK_NAME in extension.checks.ci.get()) {
                    subproject.tasks.named { it == "fullAnalysis" }.configureEach { it.dependsOn(secretScan) }
                }
            }
        }

        createMultiModuleTasks(project, actualProjects, aggregatedReports)
        ArmorInfoTask.register(
            project,
            extension,
            "multi-module",
            tiers = null,
            module = false,
            repository = true,
            modules = actualProjects,
        )
        SarifConfigurator.register(project, listOf(project) + actualProjects)
        ArmorReportConfigurator.register(project, listOf(project) + actualProjects)
    }

    private fun configureSingleModuleProject(
        project: Project,
        extension: CodeArmorExtension,
        projectType: ProjectType,
        aggregatedCoverage: File?,
    ) {
        ConfiguratorUtil.registerConfigurators(project, extension, projectType, aggregatedCoverage)
        TaskCreator.createCustomTasks(project, extension, projectType, repository = false)
    }

    fun createMultiModuleTasks(
        project: Project,
        actualProjects: List<Project>,
        aggregatedReports: List<String> = emptyList(),
    ) {
        project.tasks.register("allCodeQuality") { task ->
            task.group = "verification"
            task.description = "Runs code quality checks on all modules, then the combined test and coverage reports"
            actualProjects.forEach { subproject ->
                subproject.tasks.findByName("codeQuality")?.let { subTask ->
                    task.dependsOn(subTask)
                }
            }
            task.dependsOn(aggregatedReports)
            task.doLast {
                LogUtil.essential("🎯 All modules code quality checks completed!")
            }
        }
    }
}
