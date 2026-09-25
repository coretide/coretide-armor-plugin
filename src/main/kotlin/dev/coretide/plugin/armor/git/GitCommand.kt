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

/** Runs git in [workingDir] through Gradle's [ExecOperations], so it is safe at execution time. */
class GitCommand(
    private val execOperations: ExecOperations,
    private val workingDir: File,
) {
    /** The trimmed output of a successful command; null when git fails, prints nothing, or is missing. */
    fun output(vararg args: String): String? = exec(*args)?.takeIf { it.first == 0 }?.second?.takeIf { it.isNotEmpty() }

    /** Runs git and returns whether it succeeded. */
    fun run(vararg args: String): Boolean = exec(*args)?.first == 0

    private fun exec(vararg args: String): Pair<Int, String>? =
        try {
            val output = ByteArrayOutputStream()
            val result =
                execOperations.exec {
                    it.workingDir = workingDir
                    it.commandLine(listOf("git") + args)
                    it.standardOutput = output
                    it.errorOutput = ByteArrayOutputStream()
                    it.isIgnoreExitValue = true
                }
            result.exitValue to output.toString().trim()
        } catch (_: Exception) {
            null
        }
}
