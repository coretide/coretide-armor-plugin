/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.configurators

import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.ProjectType
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

object ResourceConfigurator {
    fun configure(
        project: Project,
        extension: CodeArmorExtension,
        projectType: ProjectType,
    ) {
        if (extension.enableResourceProcessing &&
            (projectType == ProjectType.JAVA_APPLICATION || projectType == ProjectType.KOTLIN_APPLICATION)
        ) {
            configureResourceProcessing(project)
        }
    }

    private fun configureResourceProcessing(project: Project) {
        project.tasks.named("processResources", Copy::class.java) { task ->
            val appVersion = getAppVersion(project)
            val gitCommitId = getGitCommitIdAbbrev(project)
            val applicationFilePatterns =
                listOf(
                    "**/application.yaml",
                    "**/application.yml",
                    "**/application.properties",
                    "**/application-*.yaml",
                    "**/application-*.yml",
                    "**/application-*.properties",
                )
            applicationFilePatterns.forEach { pattern ->
                task.filesMatching(pattern) {
                    try {
                        it.filter(
                            mapOf("tokens" to mapOf("appVersion" to appVersion, "gitVersion" to gitCommitId)),
                            ReplaceTokens::class.java,
                        )
                        project.logger.info("📝 Resource processing configured for pattern: $pattern")
                    } catch (e: Exception) {
                        project.logger.warn("⚠️ Warning: Could not configure resource token replacement for $pattern: ${e.message}")
                    }
                }
            }
            project.logger.info("📝 Resource processing configured with tokens:")
            project.logger.info("   • appVersion: $appVersion")
            project.logger.info("   • gitVersion: $gitCommitId")
        }
    }

    private fun getAppVersion(project: Project): String = project.version.toString()

    private fun getGitCommitIdAbbrev(project: Project): String =
        try {
            val result =
                project.providers
                    .exec { spec ->
                        spec.commandLine("git", "rev-parse", "--short", "HEAD")
                        spec.workingDir(project.projectDir)
                        spec.isIgnoreExitValue = true
                    }.standardOutput.asText
                    .get()
                    .trim()

            result.ifEmpty {
                project.logger.warn("⚠️ Warning: Could not get Git commit ID, using 'unknown'")
                "unknown"
            }
        } catch (e: Exception) {
            project.logger.warn("⚠️ Warning: Could not get Git commit ID: ${e.message}")
            "unknown"
        }
}
