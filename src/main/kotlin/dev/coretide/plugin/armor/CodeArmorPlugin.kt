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

import dev.coretide.plugin.armor.codestats.CodeStatsManager
import dev.coretide.plugin.armor.configurator.ArmorReportConfigurator
import dev.coretide.plugin.armor.configurator.SarifConfigurator
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
        // Lazy, so the defaults follow the tool switches the build script sets after applying. Set, not a
        // convention: add() on a property that only has a convention replaces it, so ci.add("x") would have dropped
        // every default check.
        extension.checks.build.set(project.provider { TaskCreator.defaultBuildTier(extension) })
        extension.checks.ci.set(project.provider { TaskCreator.defaultCiTier(project, extension) })
        // The one place CodeArmor waits for the build script: its settings decide which tools to apply.
        project.afterEvaluate {
            extension.disallowChanges()
            LogUtil.initialize(project, extension)
            val projectType = extension.projectType.orNull ?: ProjectDetector.detectProjectType(project)
            val isMultiModule = extension.forcedMultiModule || ProjectDetector.detectMultiModule(project)
            LogUtil.essential(
                "🛡️ CodeArmor: Detected ${projectType.displayName} project${if (isMultiModule) " (multi-module)" else ""}",
            )
            if (isMultiModule) {
                MultiModuleTaskCreator.configureMultiModuleProject(project, extension)
            } else {
                configureSingleModuleProject(project, extension, projectType)
            }
            extension.deprecations.messages().forEach { LogUtil.essential("⚠️ CodeArmor: $it") }
            if (extension.gitHooks.enabled.get()) {
                GitHooksManager.registerTasks(project, extension)
            }
            CodeStatsManager.configure(project, extension)
            if (extension.versionFromGit.get()) {
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
        TaskCreator.createCustomTasks(project, extension, projectType, repository = true)
        SarifConfigurator.register(project, listOf(project))
        ArmorReportConfigurator.register(project, listOf(project))
    }
}
