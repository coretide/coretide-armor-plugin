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

import dev.coretide.plugin.armor.util.LogUtil
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

abstract class GitValueSource : ValueSource<String, GitValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val operation: Property<GitOperation>
        val projectDir: Property<String>
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String =
        when (parameters.operation.get()) {
            GitOperation.VERSION -> obtainVersion()
            GitOperation.COMMIT_HASH -> obtainCommitHash()
            null -> "unknown"
        }

    private fun obtainVersion(): String {
        val projectDirPath = parameters.projectDir.get()
        val projectDir = File(projectDirPath)

        val tagRef = System.getenv("CI_COMMIT_TAG")
        if (tagRef?.startsWith("v") == true) {
            return tagRef.replace("v", "")
        }
        val githubRef = System.getenv("GITHUB_REF")
        if (githubRef?.startsWith("refs/tags/v") == true) {
            return githubRef.replace("refs/tags/v", "")
        }

        val gitDir = File(projectDir, ".git")
        LogUtil.verbosePrint("🔍 Git version detection:")
        LogUtil.verbosePrint("   Project directory: ${projectDir.absolutePath}")
        LogUtil.verbosePrint("   Git directory: ${gitDir.absolutePath}")
        LogUtil.verbosePrint("   Git directory exists: ${gitDir.exists()}")
        if (!gitDir.exists()) {
            LogUtil.essentialPrint("   ⚠️ No .git directory found, using default version")
            return "0.0.1-SNAPSHOT"
        }
        return try {
            val exactTagOutput = ByteArrayOutputStream()
            val exactTagResult: ExecResult =
                execOperations.exec {
                    it.workingDir = projectDir
                    it.commandLine("git", "describe", "--tags", "--exact-match", "HEAD")
                    it.standardOutput = exactTagOutput
                    it.errorOutput = ByteArrayOutputStream()
                    it.isIgnoreExitValue = true
                }
            LogUtil.verbosePrint("   Exact tag result: exit code ${exactTagResult.exitValue}")
            if (exactTagResult.exitValue == 0) {
                val tag = exactTagOutput.toString().trim()
                LogUtil.verbosePrint("   ✅ Found exact tag: $tag")
                return if (tag.startsWith("v")) tag.replace("v", "") else tag
            } else {
                LogUtil.verbosePrint("   ℹ️ No exact tag found (normal if not on tagged commit)")
            }
            val latestOutput = ByteArrayOutputStream()
            val latestTagResult: ExecResult =
                execOperations.exec {
                    it.workingDir = projectDir
                    it.commandLine("git", "describe", "--tags", "--abbrev=0")
                    it.standardOutput = latestOutput
                    it.errorOutput = ByteArrayOutputStream()
                    it.isIgnoreExitValue = true
                }
            LogUtil.verbosePrint("   Latest tag result: exit code ${latestTagResult.exitValue}")
            if (latestTagResult.exitValue == 0) {
                val tag = latestOutput.toString().trim()
                val version = if (tag.startsWith("v")) tag.replace("v", "") else tag
                LogUtil.verbosePrint("   ✅ Found latest tag: $tag, using: $version-SNAPSHOT")
                "$version-SNAPSHOT"
            } else {
                LogUtil.verbosePrint("   ⚠️ No tags found in repository")
                val logOutput = ByteArrayOutputStream()
                val logResult =
                    execOperations.exec {
                        it.workingDir = projectDir
                        it.commandLine("git", "log", "--oneline", "-1")
                        it.standardOutput = logOutput
                        it.errorOutput = ByteArrayOutputStream()
                        it.isIgnoreExitValue = true
                    }

                if (logResult.exitValue == 0) {
                    LogUtil.verbosePrint("   ℹ️ Repository has commits but no tags")
                } else {
                    LogUtil.verbosePrint("   ℹ️ Repository appears to be empty (no commits)")
                }
                "0.0.1-SNAPSHOT"
            }
        } catch (e: Exception) {
            LogUtil.essentialPrint("   ❌ Git command failed: ${e.message}")
            "0.0.1-SNAPSHOT"
        }
    }

    private fun obtainCommitHash(): String {
        val projectDirPath = parameters.projectDir.get()
        val projectDir = File(projectDirPath)

        return try {
            val output = ByteArrayOutputStream()
            val result: ExecResult =
                execOperations.exec {
                    it.workingDir = projectDir
                    it.commandLine("git", "rev-parse", "--short", "HEAD")
                    it.standardOutput = output
                    it.isIgnoreExitValue = true
                }

            if (result.exitValue == 0) {
                output.toString().trim().takeIf { it.isNotBlank() } ?: "unknown"
            } else {
                "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }
}

enum class GitOperation {
    VERSION,
    COMMIT_HASH,
}
