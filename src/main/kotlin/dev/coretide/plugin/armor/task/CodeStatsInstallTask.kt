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

import dev.coretide.plugin.armor.codestats.CodeStatsFiles
import dev.coretide.plugin.armor.codestats.CodeStatsInstaller
import dev.coretide.plugin.armor.enumeration.CodeStatsScope
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

/**
 * Applies the code stats settings.
 *
 * - Enabled, scope REPO: installs for this repository, unless code stats hooks already cover it.
 * - Enabled, scope GLOBAL: installs for every repository on this machine, unless another code stats
 *   install already does.
 * - Disabled: removes CodeArmor's install from this repository, and opts it out of any global install.
 */
@UntrackedTask(because = "Changes git configuration and files outside the build")
abstract class CodeStatsInstallTask : CodeStatsTask() {
    init {
        description = "📊 Installs code stats reporting to Code::Stats as configured (codeArmor.codeStats)"
    }

    @TaskAction
    fun install() {
        if (runningOnCi()) {
            logger.lifecycle("ℹ️ CI is set: code stats reports a developer's own work, so nothing is installed here")
            return
        }
        val installer = CodeStatsInstaller(paths, logger)
        val repository = locateRepository()
        if (!codeStatsEnabled.get()) {
            if (repository == null) {
                logger.lifecycle("ℹ️ Code stats is disabled; nothing to do outside a git repository")
                return
            }
            installer.uninstallRepository(repository, quiet = true)
            if (CodeStatsFiles.activeHooksDir(repository) != null) {
                installer.optOut(repository)
                logger.lifecycle("📴 Code stats is disabled for this project: opted this repository out of the global code stats install")
            } else {
                logger.lifecycle("ℹ️ Code stats is disabled for this project (codeArmor.codeStats.enabled)")
            }
            return
        }
        installer.ensureToken(System.getenv("CODESTATS_API_TOKEN"))
        when (scope.get()) {
            CodeStatsScope.REPO -> {
                if (repository == null) {
                    logger.warn("⚠️ ${gradleRootDirectory.get().asFile} is not inside a git work tree; code stats needs one for scope REPO")
                    return
                }
                installer.installRepository(repository)
            }
            CodeStatsScope.GLOBAL -> installer.installGlobal(git, repository)
        }
    }
}
