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
import dev.coretide.plugin.armor.codestats.CodeStatsSettings
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

/** Shows the code stats settings in effect and what is installed. Changes nothing. */
@UntrackedTask(because = "Reports the current state of git configuration and files outside the build")
abstract class CodeStatsStatusTask : CodeStatsTask() {
    init {
        description = "📊 Shows the code stats settings and what is installed"
    }

    @TaskAction
    fun status() {
        val paths = paths
        val enabled = codeStatsEnabled.get()
        logger.lifecycle("📊 Code stats: ${if (enabled) "enabled, scope ${scope.get()}" else "disabled"}")
        logger.lifecycle(
            "   Settings: codeArmor.codeStats in the build script; ${CodeStatsSettings.ENABLED_PROPERTY} and " +
                "${CodeStatsSettings.SCOPE_PROPERTY} in ~/.gradle/gradle.properties or -P override it",
        )
        logger.lifecycle("   Token: ${if (paths.tokenFile.isFile) paths.tokenFile.toString() else "missing (${paths.tokenFile})"}")
        logger.lifecycle("   Queued pulses: ${paths.queuedPulses()} in ${paths.queueDir}")

        val repository = locateRepository()
        if (repository == null) {
            logger.lifecycle("   Repository: not inside a git work tree")
            return
        }
        val active = CodeStatsFiles.activeHooksDir(repository)
        val optedOut = repository.git.output("config", "--get", "codestats.enabled") == "false"
        val state =
            when {
                active == null -> "not reporting"
                optedOut -> "opted out of the code stats hooks in $active"
                CodeStatsFiles.isCodeArmorInstall(active, paths) -> "reporting through CodeArmor's install in $active"
                else -> "reporting through a code stats install CodeArmor did not make, in $active"
            }
        logger.lifecycle("   This repository: $state")
        if (enabled && (active == null || optedOut) && !runningOnCi()) {
            logger.warn("⚠️ Code stats is enabled but this repository is not reporting; run ./gradlew armorCodeStatsInstall")
        }
    }
}
