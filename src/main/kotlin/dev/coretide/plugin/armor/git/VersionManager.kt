/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.git

import org.gradle.api.Project
import java.io.File
import javax.inject.Inject

abstract class VersionManager
    @Inject
    constructor() {
        companion object {
            fun configureVersionFromGit(project: Project) {
                val version = extractVersionFromGitTag(project)
                project.version = version
                println("📋 Project version set to: $version")
            }

            private fun extractVersionFromGitTag(project: Project): String {
                val tagRef = System.getenv("CI_COMMIT_TAG")
                if (tagRef?.startsWith("v") == true) {
                    return tagRef.replace("v", "")
                }
                val githubRef = System.getenv("GITHUB_REF")
                if (githubRef?.startsWith("refs/tags/v") == true) {
                    return githubRef.replace("refs/tags/v", "")
                }
                val gitDir = File(project.projectDir, ".git")
                if (!gitDir.exists()) {
                    println("⚠️ Warning: Not in a git repository (.git directory not found in ${project.projectDir})")
                    return "0.0.1-SNAPSHOT"
                }
                val fallbackProvider =
                    project.providers
                        .exec { spec ->
                            spec.commandLine("git", "describe", "--tags", "--abbrev=0")
                            spec.workingDir(project.projectDir)
                            spec.isIgnoreExitValue = true
                        }.standardOutput.asText
                        .map { output ->
                            val tag = output.trim()
                            val version = if (tag.startsWith("v")) tag.replace("v", "") else tag
                            "$version-SNAPSHOT"
                        }
                return try {
                    val versionProvider =
                        project.providers
                            .exec { spec ->
                                spec.commandLine("git", "describe", "--tags", "--exact-match", "HEAD")
                                spec.workingDir(project.projectDir)
                                spec.isIgnoreExitValue = true
                            }.standardOutput.asText
                            .map { output ->
                                val tag = output.trim()
                                if (tag.startsWith("v")) tag.replace("v", "") else tag
                            }
                    versionProvider.getOrElse(fallbackProvider.getOrElse("0.0.1-SNAPSHOT"))
                } catch (e: Exception) {
                    println("⚠️ Warning: Could not extract version from Git tag: ${e.message}")
                    "0.0.1-SNAPSHOT"
                }
            }
        }
    }
