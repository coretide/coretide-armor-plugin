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
import java.io.ByteArrayOutputStream
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
    val hooksDir: File,
    /** `core.hooksPath` as configured, or null when unset. Relative values are relative to [topLevel]. */
    val hooksPath: String?,
) {
    companion object {
        /** Returns null when [directory] is not inside a git work tree, or git is not installed. */
        fun locate(
            execOperations: ExecOperations,
            directory: File,
        ): GitRepository? {
            val topLevel = git(execOperations, directory, "rev-parse", "--show-toplevel") ?: return null
            val commonDir = git(execOperations, directory, "rev-parse", "--git-common-dir") ?: return null
            // Older git prints the common dir relative to the working directory.
            val commonDirFile = File(commonDir).let { if (it.isAbsolute) it else File(directory, commonDir) }
            return GitRepository(
                topLevel = File(topLevel).canonicalFile,
                hooksDir = File(commonDirFile, "hooks").canonicalFile,
                hooksPath = git(execOperations, directory, "config", "--get", "core.hooksPath"),
            )
        }

        private fun git(
            execOperations: ExecOperations,
            directory: File,
            vararg args: String,
        ): String? =
            try {
                val output = ByteArrayOutputStream()
                val result =
                    execOperations.exec {
                        it.workingDir = directory
                        it.commandLine(listOf("git") + args)
                        it.standardOutput = output
                        it.errorOutput = ByteArrayOutputStream()
                        it.isIgnoreExitValue = true
                    }
                output.toString().trim().takeIf { result.exitValue == 0 && it.isNotEmpty() }
            } catch (_: Exception) {
                null
            }
    }
}
