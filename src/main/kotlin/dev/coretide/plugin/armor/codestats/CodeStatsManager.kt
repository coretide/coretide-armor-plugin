/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.codestats

import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.git.GitRepository
import dev.coretide.plugin.armor.task.CodeStatsFlushTask
import dev.coretide.plugin.armor.task.CodeStatsInstallTask
import dev.coretide.plugin.armor.task.CodeStatsStatusTask
import dev.coretide.plugin.armor.task.CodeStatsTask
import dev.coretide.plugin.armor.task.CodeStatsUninstallTask
import dev.coretide.plugin.armor.util.LogUtil
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * Code stats: reporting commit activity to Code::Stats from git hooks.
 *
 * Like CodeArmor's other hooks, nothing is installed while Gradle configures a build; the
 * armorCodeStats* tasks do all the work.
 */
object CodeStatsManager {
    fun configure(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        val settings = CodeStatsSettings.resolve(project, extension.codeStats)
        settings.warnings.forEach { LogUtil.essential("⚠️ CodeArmor: $it") }

        fun CodeStatsTask.configure() {
            gradleRootDirectory.set(project.rootDir)
            codeStatsEnabled.set(settings.enabled)
            scope.set(settings.scope)
        }
        project.tasks.register("armorCodeStatsInstall", CodeStatsInstallTask::class.java) { it.configure() }
        project.tasks.register("armorCodeStatsUninstall", CodeStatsUninstallTask::class.java) { it.configure() }
        project.tasks.register("armorCodeStatsStatus", CodeStatsStatusTask::class.java) { it.configure() }
        project.tasks.register("armorCodeStatsFlush", CodeStatsFlushTask::class.java) { it.configure() }

        val onCi = project.providers.environmentVariable("CI").orNull == "true"
        if (settings.enabled && !onCi) {
            val reporting =
                project.providers
                    .of(ReportingValueSource::class.java) {
                        it.parameters.directory.set(project.rootDir.absolutePath)
                    }.get()
            if (!reporting) {
                LogUtil.essential("💡 CodeArmor: code stats is enabled but this repository is not reporting; run ./gradlew armorCodeStatsInstall")
            }
        }
    }

    /**
     * Whether a repository is reporting: code stats hooks are active and it has not opted out. A
     * directory outside any git repository counts as reporting, since there is nothing to install.
     *
     * A value source, so the configuration cache re-checks it on every build instead of remembering it.
     */
    abstract class ReportingValueSource : ValueSource<Boolean, ReportingValueSource.Parameters> {
        interface Parameters : ValueSourceParameters {
            val directory: Property<String>
        }

        @get:Inject
        abstract val execOperations: ExecOperations

        override fun obtain(): Boolean {
            val repository = GitRepository.locate(execOperations, File(parameters.directory.get())) ?: return true
            val hooksPath = repository.hooksPath ?: return false
            return CodeStatsFiles.isCodeStatsHooksDir(repository.resolveHooksPath(hooksPath)) &&
                repository.git.output("config", "--get", "codestats.enabled") != "false"
        }
    }
}
