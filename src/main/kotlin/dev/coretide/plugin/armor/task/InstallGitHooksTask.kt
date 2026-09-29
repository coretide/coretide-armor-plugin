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
 * - With `conventionalCommits`, a commit-msg hook rejects messages that are not Conventional Commits.
 * - With `secretScan`, a pre-commit hook scans the staged changes with gitleaks.
 * - A hook CodeArmor did not write is never overwritten. One it wrote and that is now switched off is removed.
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

    @get:Input
    abstract val conventionalCommits: Property<Boolean>

    @get:Input
    abstract val conventionalCommitTypes: ListProperty<String>

    @get:Input
    abstract val secretScan: Property<Boolean>

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
        if (GitHooksManager.isLegacy(legacyPreCommit) && legacyPreCommit.delete()) {
            logger.lifecycle("🧹 Removed the obsolete pre-commit hook an earlier CodeArmor version installed")
        }

        val prePush = File(repository.hooksDir, "pre-push")
        when {
            !prePushEnabled.get() -> remove(prePush, "prePushEnabled = false")
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
                write(prePush, GitHooksManager.prePushScript(gradleRootFromTopLevel, prePushTasks.get()))
                logger.lifecycle("🪝 Installed the pre-push hook ($prePush): runs ${prePushTasks.get().joinToString(" ")}")
            }
        }

        val types = conventionalCommitTypes.get()
        val invalidTypes = types.filterNot { GitHooksManager.COMMIT_TYPE.matches(it) }
        if (conventionalCommits.get() && invalidTypes.isNotEmpty()) {
            logger.warn("⚠️ Left out conventionalCommitTypes that are not plain words: ${invalidTypes.joinToString(", ")}")
        }
        installIfEnabled(
            File(repository.hooksDir, "commit-msg"),
            conventionalCommits.get(),
            "conventionalCommits",
            "rejects messages that are not Conventional Commits",
        ) { GitHooksManager.commitMsgScript(types - invalidTypes.toSet()) }
        installIfEnabled(
            File(repository.hooksDir, "pre-commit"),
            secretScan.get(),
            "secretScan",
            "scans the staged changes with gitleaks",
        ) { GitHooksManager.secretScanScript() }

        GitHooksManager.reportHooksPath(repository, logger)
    }

    /** Writes [hook] when [enabled]; otherwise removes the one CodeArmor wrote earlier, if any. */
    private fun installIfEnabled(
        hook: File,
        enabled: Boolean,
        setting: String,
        purpose: String,
        script: () -> String,
    ) {
        when {
            !enabled -> remove(hook, "$setting = false", quiet = true)
            hook.exists() && !GitHooksManager.isManagedByCodeArmor(hook) -> {
                logger.warn("⚠️ Left the existing ${hook.name} hook alone because CodeArmor did not write it: $hook")
            }
            else -> {
                write(hook, script())
                logger.lifecycle("🪝 Installed the ${hook.name} hook ($hook): $purpose")
            }
        }
    }

    private fun remove(
        hook: File,
        reason: String,
        quiet: Boolean = false,
    ) {
        if (GitHooksManager.isManagedByCodeArmor(hook) && hook.delete()) {
            logger.lifecycle("🧹 $reason: removed CodeArmor's ${hook.name} hook")
        } else if (!quiet) {
            logger.lifecycle("ℹ️ $reason: no ${hook.name} hook installed")
        }
    }

    private fun write(
        hook: File,
        script: String,
    ) {
        hook.writeText(script)
        hook.setExecutable(true, false)
    }
}
