/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.configurator

import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.ProjectType
import dev.coretide.plugin.armor.git.GitOperation
import dev.coretide.plugin.armor.git.GitValueSource
import dev.coretide.plugin.armor.util.LogUtil
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Project
import org.gradle.api.provider.Provider
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
        val appVersionProvider: Provider<String> =
            project.providers.of(GitValueSource::class.java) {
                it.parameters.operation.set(GitOperation.VERSION)
                it.parameters.projectDir.set(project.projectDir.absolutePath)
            }
        val gitCommitIdProvider: Provider<String> =
            project.providers.of(GitValueSource::class.java) {
                it.parameters.operation.set(GitOperation.COMMIT_HASH)
                it.parameters.projectDir.set(project.projectDir.absolutePath)
            }

        project.tasks.named("processResources", Copy::class.java) { task ->
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
                task.filesMatching(pattern) { copySpec ->
                    try {
                        copySpec.filter(
                            mapOf(
                                "tokens" to
                                    mapOf(
                                        "appVersion" to appVersionProvider.get(),
                                        "gitVersion" to gitCommitIdProvider.get(),
                                    ),
                            ),
                            ReplaceTokens::class.java,
                        )
                        LogUtil.verbose("📝 Resource processing configured for pattern: $pattern")
                    } catch (e: Exception) {
                        LogUtil.verbose("⚠️ Warning: Could not configure resource token replacement for $pattern: ${e.message}")
                    }
                }
            }

            task.doFirst {
                LogUtil.verbose("📝 Resource processing configured with tokens:")
                LogUtil.verbose("   • appVersion: ${appVersionProvider.get()}")
                LogUtil.verbose("   • gitVersion: ${gitCommitIdProvider.get()}")
            }
        }
    }
}
