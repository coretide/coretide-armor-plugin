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

import org.gradle.process.ExecOperations
import java.io.File

/**
 * The git repository a directory belongs to, as git itself resolves it.
 *
 * Hooks live in the *common* git directory: in a linked worktree, `.git` is a file and the worktree's
 * own git directory has no hooks. [hooksDir] is always the repository's own hooks directory, never
 * `core.hooksPath`, which may be a global directory shared by every repository on the machine.
 */
class GitRepository(
    val topLevel: File,
    val commonDir: File,
    /** `core.hooksPath` as configured, or null when unset. Relative values are relative to [topLevel]. */
    val hooksPath: String?,
    /** Runs git inside this repository. */
    val git: GitCommand,
) {
    val hooksDir: File get() = File(commonDir, "hooks")

    /** A `core.hooksPath` value as a directory: relative values are relative to [topLevel]. */
    fun resolveHooksPath(value: String): File = File(value).let { if (it.isAbsolute) it else File(topLevel, value) }

    companion object {
        /** Returns null when [directory] is not inside a git work tree, or git is not installed. */
        fun locate(
            execOperations: ExecOperations,
            directory: File,
        ): GitRepository? {
            val git = GitCommand(execOperations, directory)
            val topLevel = git.output("rev-parse", "--show-toplevel") ?: return null
            val commonDir = git.output("rev-parse", "--git-common-dir") ?: return null
            // Older git prints the common dir relative to the working directory.
            val commonDirFile = File(commonDir).let { if (it.isAbsolute) it else File(directory, commonDir) }
            return GitRepository(
                topLevel = File(topLevel).canonicalFile,
                commonDir = commonDirFile.canonicalFile,
                hooksPath = git.output("config", "--get", "core.hooksPath"),
                git = git,
            )
        }
    }
}
