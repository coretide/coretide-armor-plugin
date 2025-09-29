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

import dev.coretide.plugin.armor.git.GitHooksManager
import dev.coretide.plugin.armor.git.VersionManager
import dev.coretide.plugin.armor.task.MultiModuleTaskCreator
import dev.coretide.plugin.armor.task.TaskCreator
import dev.coretide.plugin.armor.util.ConfigurationCacheUtil
import dev.coretide.plugin.armor.util.ConfiguratorUtil
import dev.coretide.plugin.armor.util.LogUtil
import dev.coretide.plugin.armor.util.ProjectDetector
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class CodeArmorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("codeArmor", CodeArmorExtension::class.java)
        project.afterEvaluate {
            LogUtil.initialize(project, extension)
            val projectType =
                if (extension.autoDetect) {
                    ProjectDetector.detectProjectType(project)
                } else {
                    extension.projectType ?: ProjectDetector.detectProjectType(project)
                }
            val isMultiModule = extension.isMultiModule || ProjectDetector.detectMultiModule(project)
            LogUtil.essential(
                "🛡️ CodeArmor: Detected ${projectType.displayName} project${if (isMultiModule) " (multi-module)" else ""}",
            )
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
            ConfigurationCacheUtil.optimizeThirdPartyPlugins(project)
        }
    }

    private fun configureSingleModuleProject(
        project: Project,
        extension: CodeArmorExtension,
        projectType: ProjectType,
    ) {
        ConfiguratorUtil.registerConfigurators(project, extension, projectType)
        TaskCreator.createCustomTasks(project, extension)
    }
}
