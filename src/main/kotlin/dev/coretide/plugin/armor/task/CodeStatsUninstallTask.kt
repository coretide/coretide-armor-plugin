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

import dev.coretide.plugin.armor.codestats.CodeStatsInstaller
import dev.coretide.plugin.armor.enumeration.CodeStatsScope
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

/**
 * Removes CodeArmor's code stats install at the configured scope and puts back the `core.hooksPath`
 * it replaced. An install CodeArmor did not make is left alone, and the token and settings are kept.
 */
@UntrackedTask(because = "Changes git configuration outside the build")
abstract class CodeStatsUninstallTask : CodeStatsTask() {
    init {
        description = "🧹 Removes CodeArmor's code stats install and restores the previous core.hooksPath"
    }

    @TaskAction
    fun uninstall() {
        if (runningOnCi()) {
            logger.lifecycle("ℹ️ CI is set: code stats is never installed here, so there is nothing to remove")
            return
        }
        val installer = CodeStatsInstaller(paths, logger)
        when (scope.get()) {
            CodeStatsScope.REPO -> {
                val repository = locateRepository()
                if (repository == null) {
                    logger.lifecycle("ℹ️ Not inside a git work tree; no repository install to remove")
                    return
                }
                installer.uninstallRepository(repository)
            }
            CodeStatsScope.GLOBAL -> installer.uninstallGlobal(git)
        }
    }
}
