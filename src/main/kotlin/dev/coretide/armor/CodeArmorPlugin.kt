/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.armor

import dev.coretide.armor.git.GitHooksManager
import dev.coretide.armor.git.VersionManager
import dev.coretide.armor.tasks.MultiModuleTaskCreator
import dev.coretide.armor.tasks.TaskCreator
import dev.coretide.armor.utils.ConfigurationCacheUtils
import dev.coretide.armor.utils.ConfiguratorUtils
import dev.coretide.armor.utils.ProjectDetector
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class CodeArmorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("codeArmor", CodeArmorExtension::class.java)
        project.afterEvaluate {
            val projectType =
                if (extension.autoDetect) {
                    ProjectDetector.detectProjectType(project)
                } else {
                    extension.projectType ?: ProjectDetector.detectProjectType(project)
                }
            val isMultiModule = extension.isMultiModule || ProjectDetector.detectMultiModule(project)
            println("🛡️ CodeArmor: Detected $projectType project${if (isMultiModule) " (multi-module)" else ""}")
            if (isMultiModule) {
                MultiModuleTaskCreator.configureMultiModuleProject(project, extension)
            } else {
                configureSingleModuleProject(project, extension, projectType)
            }
            if (extension.enableGitHooks) {
                GitHooksManager.configureGitHooks(project, extension)
            }
            if (extension.enableVersionFromGit) {
                VersionManager.configureVersionFromGit(project)
            }
            ConfigurationCacheUtils.optimizeThirdPartyPlugins(project)
        }
    }

    private fun configureSingleModuleProject(
        project: Project,
        extension: CodeArmorExtension,
        projectType: ProjectType,
    ) {
        ConfiguratorUtils.registerConfigurators(project, extension, projectType)
        TaskCreator.createCustomTasks(project, extension, projectType)
    }
}
