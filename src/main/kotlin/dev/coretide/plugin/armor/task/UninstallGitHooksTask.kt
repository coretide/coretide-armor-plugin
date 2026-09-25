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

import dev.coretide.plugin.armor.git.GitHooksManager
import dev.coretide.plugin.armor.git.GitRepository
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * Removes the git hooks CodeArmor installed, in this version or in 0.1.x. Hooks CodeArmor did not
 * write are left alone.
 */
@UntrackedTask(because = "Removes files from the repository's .git/hooks, which Gradle does not track")
abstract class UninstallGitHooksTask : DefaultTask() {
    @get:Internal
    abstract val gradleRootDirectory: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        group = "build setup"
        description = "🧹 Removes the git hooks CodeArmor installed"
    }

    @TaskAction
    fun uninstall() {
        val repository = GitRepository.locate(execOperations, gradleRootDirectory.get().asFile)
        if (repository == null) {
            logger.lifecycle("ℹ️ Not inside a git work tree; no hooks to remove.")
            return
        }
        val removed =
            listOf("pre-push", "pre-commit")
                .map { File(repository.hooksDir, it) }
                .filter { GitHooksManager.isManagedByCodeArmor(it) && it.delete() }
        if (removed.isEmpty()) {
            logger.lifecycle("ℹ️ No CodeArmor hooks installed in ${repository.hooksDir}")
        } else {
            removed.forEach { logger.lifecycle("🧹 Removed $it") }
        }
    }
}
