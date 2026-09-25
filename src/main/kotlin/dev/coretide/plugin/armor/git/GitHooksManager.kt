/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.git

import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.task.InstallGitHooksTask
import dev.coretide.plugin.armor.task.UninstallGitHooksTask
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File

/**
 * Git hooks CodeArmor manages.
 *
 * Hooks are only ever written by `armorInstallGitHooks` and removed by `armorUninstallGitHooks`.
 * Nothing is written while Gradle configures a build: that would change files under `.git` as a
 * side effect of any build, including on CI, and invalidates the configuration cache on the next run.
 */
object GitHooksManager {
    /** Marks a hook as written by CodeArmor, so it can be upgraded or removed safely. */
    const val MARKER = "CODEARMOR-MANAGED-HOOK"

    /**
     * Text in the hooks CodeArmor 0.1.x wrote during configuration, before [MARKER] existed. The
     * 0.1.3 pre-commit hook calls tasks that no longer exist, so installing removes it.
     */
    private val LEGACY_SIGNATURES = listOf("CodeArmor: Running pre-push", "CodeArmor: Running pre-commit")

    fun registerTasks(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        project.tasks.register("armorInstallGitHooks", InstallGitHooksTask::class.java) { task ->
            task.gradleRootDirectory.set(project.rootDir)
            task.prePushEnabled.set(project.provider { extension.prePushEnabled })
            task.prePushTasks.set(extension.checks.prePush)
        }
        project.tasks.register("armorUninstallGitHooks", UninstallGitHooksTask::class.java) { task ->
            task.gradleRootDirectory.set(project.rootDir)
        }
    }

    /** True for a hook CodeArmor wrote, in this version or in 0.1.x. */
    fun isManagedByCodeArmor(hook: File): Boolean {
        if (!hook.isFile) return false
        val text = hook.readText()
        return MARKER in text || LEGACY_SIGNATURES.any { it in text }
    }

    /**
     * The pre-push hook. It runs [tasks] from the Gradle root, which may be a subdirectory of the
     * repository, and blocks the push when they fail. `git push --no-verify` skips it once.
     */
    fun prePushScript(
        gradleRootFromTopLevel: String,
        tasks: List<String>,
    ): String {
        val gradleRoot =
            if (gradleRootFromTopLevel.isEmpty()) {
                "\"\$(git rev-parse --show-toplevel)\""
            } else {
                "\"\$(git rev-parse --show-toplevel)\"/${shellQuote(gradleRootFromTopLevel)}"
            }
        val announce = shellQuote("🛡️ CodeArmor: running pre-push checks: ${tasks.joinToString(" ")}")
        return """
            |#!/bin/sh
            |# $MARKER
            |# Installed by `./gradlew armorInstallGitHooks`; `./gradlew armorUninstallGitHooks` removes it.
            |# Runs CodeArmor's pre-push checks and blocks the push when they fail.
            |# Skip it once with `git push --no-verify`.
            |
            |cd $gradleRoot || exit 1
            |
            |run_gradle() {
            |  if [ -f ./gradlew ]; then
            |    sh ./gradlew "${'$'}@"
            |  else
            |    gradle "${'$'}@"
            |  fi
            |}
            |
            |echo $announce
            |# Git passes the refs being pushed on stdin; Gradle must not consume them.
            |if run_gradle --quiet ${tasks.joinToString(" ") { shellQuote(it) }} </dev/null; then
            |  echo "✅ CodeArmor: pre-push checks passed"
            |else
            |  echo "❌ CodeArmor: pre-push checks failed, so the push was blocked."
            |  echo "   Fix the failures, or skip the checks once with: git push --no-verify"
            |  exit 1
            |fi
            |
        """.trimMargin()
    }

    /**
     * Warns when `core.hooksPath` makes Git ignore the repository's own hooks directory, which is
     * where CodeArmor installs.
     */
    fun reportHooksPath(
        repository: GitRepository,
        logger: Logger,
    ) {
        val configured = repository.hooksPath ?: return
        val hooksPath = File(configured).let { if (it.isAbsolute) it else File(repository.topLevel, configured) }
        if (hooksPath.canonicalFile == repository.hooksDir.canonicalFile) return
        logger.warn(
            "⚠️ core.hooksPath is set to $configured, so Git runs hooks from there instead of " +
                "${repository.hooksDir}. Unless that directory hands pre-push over to " +
                "${repository.hooksDir}, run CodeArmor's hook from its pre-push: " +
                File(repository.hooksDir, "pre-push").absolutePath,
        )
    }

    private fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
}
