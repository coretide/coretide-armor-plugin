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
import dev.coretide.plugin.armor.util.ConfiguratorUtil
import dev.coretide.plugin.armor.util.LogUtil
import dev.coretide.plugin.armor.util.ProjectDetector
import org.gradle.api.Project

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

        actualProjects.forEach { subproject ->
            subproject.afterEvaluate {
                val subProjectType = ProjectDetector.detectProjectType(subproject)
                configureSingleModuleProject(subproject, extension, subProjectType)
            }
        }

        createMultiModuleTasks(project, actualProjects)
    }

    private fun configureSingleModuleProject(
        project: Project,
        extension: CodeArmorExtension,
        projectType: ProjectType,
    ) {
        ConfiguratorUtil.registerConfigurators(project, extension, projectType)
        TaskCreator.createCustomTasks(project, extension)
    }

    fun createMultiModuleTasks(
        project: Project,
        actualProjects: List<Project>,
    ) {
        project.tasks.register("allCodeQuality") { task ->
            task.group = "verification"
            task.description = "Runs code quality checks on all modules"
            actualProjects.forEach { subproject ->
                subproject.tasks.findByName("codeQuality")?.let { subTask ->
                    task.dependsOn(subTask)
                }
            }
            task.doLast {
                LogUtil.essential("🎯 All modules code quality checks completed!")
            }
        }
    }
}
