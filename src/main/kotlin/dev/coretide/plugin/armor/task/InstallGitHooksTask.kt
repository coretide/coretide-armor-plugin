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
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * Installs CodeArmor's git hooks into the repository's own hooks directory.
 *
 * - The pre-push hook runs the `checks.prePush` tasks and blocks the push when they fail. With
 *   `prePushEnabled = false`, a pre-push hook CodeArmor installed earlier is removed instead.
 * - A hook CodeArmor did not write is never overwritten.
 * - Hooks CodeArmor 0.1.x wrote during configuration are replaced, or removed when obsolete.
 */
@UntrackedTask(because = "Writes into the repository's .git/hooks, which Gradle does not track")
abstract class InstallGitHooksTask : DefaultTask() {
    @get:Internal
    abstract val gradleRootDirectory: DirectoryProperty

    @get:Input
    abstract val prePushEnabled: Property<Boolean>

    @get:Input
    abstract val prePushTasks: ListProperty<String>

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        group = "build setup"
        description = "🪝 Installs CodeArmor's git hooks (pre-push runs the basic checks)"
    }

    @TaskAction
    fun install() {
        val gradleRoot = gradleRootDirectory.get().asFile
        val repository = GitRepository.locate(execOperations, gradleRoot)
        if (repository == null) {
            logger.lifecycle("ℹ️ $gradleRoot is not inside a git work tree; no hooks installed.")
            return
        }
        repository.hooksDir.mkdirs()

        // 0.1.3's pre-commit hook calls tasks that no longer exist.
        val legacyPreCommit = File(repository.hooksDir, "pre-commit")
        if (GitHooksManager.isManagedByCodeArmor(legacyPreCommit) && legacyPreCommit.delete()) {
            logger.lifecycle("🧹 Removed the obsolete pre-commit hook an earlier CodeArmor version installed")
        }

        val prePush = File(repository.hooksDir, "pre-push")
        when {
            !prePushEnabled.get() -> {
                if (GitHooksManager.isManagedByCodeArmor(prePush) && prePush.delete()) {
                    logger.lifecycle("🧹 prePushEnabled = false: removed CodeArmor's pre-push hook")
                } else {
                    logger.lifecycle("ℹ️ prePushEnabled = false: no pre-push hook installed")
                }
            }
            prePush.exists() && !GitHooksManager.isManagedByCodeArmor(prePush) -> {
                logger.warn(
                    "⚠️ Left the existing pre-push hook alone because CodeArmor did not write it: $prePush. " +
                        "To run CodeArmor's checks from it, add: ./gradlew ${prePushTasks.get().joinToString(" ")}",
                )
            }
            prePushTasks.get().isEmpty() -> {
                logger.warn("⚠️ checks.prePush is empty, so no pre-push hook was installed")
            }
            else -> {
                val gradleRootFromTopLevel =
                    gradleRoot.canonicalFile.relativeTo(repository.topLevel).invariantSeparatorsPath
                prePush.writeText(GitHooksManager.prePushScript(gradleRootFromTopLevel, prePushTasks.get()))
                prePush.setExecutable(true, false)
                logger.lifecycle("🪝 Installed the pre-push hook ($prePush): runs ${prePushTasks.get().joinToString(" ")}")
            }
        }

        GitHooksManager.reportHooksPath(repository, logger)
    }
}
