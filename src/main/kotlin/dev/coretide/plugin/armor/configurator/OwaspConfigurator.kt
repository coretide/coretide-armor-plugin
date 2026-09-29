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
import dev.coretide.plugin.armor.task.GenerateConfigFileTask
import dev.coretide.plugin.armor.util.FileUtil
import dev.coretide.plugin.armor.util.LogUtil
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension
import org.owasp.dependencycheck.gradle.extension.NvdExtension

object OwaspConfigurator {
    fun configureOwasp(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        project.pluginManager.apply("org.owasp.dependencycheck")
        project.configure<DependencyCheckExtension> {
            // dependency-check 12.2.x exposes lazy Gradle properties; plugin source must call
            // set() explicitly (the `=` form is a Kotlin DSL script-only convenience).
            failBuildOnCVSS.set(extension.owaspFailBuildOnCVSS.toFloat())
            formats.set(listOf("HTML", "XML", "JSON", "SARIF"))
            outputDirectory.set(project.layout.buildDirectory.dir("reports/dependency-check"))
            autoUpdate.set(extension.owaspAutoUpdate)
            // What ships, as in the SBOM. Left empty, every configuration is scanned, CodeArmor's own tools
            // (SpotBugs, detekt, PIT, Error Prone, JaCoCo) included, and a CVE in one of them fails the build.
            scanConfigurations.set(DependencyHealthConfigurator.SBOM_CONFIGURATIONS)
            suppressionFile.set(resolveSuppressionFile(project, extension))
            // Through the extension, not System properties: those are JVM-wide, so the last project configured
            // won, and they outlived the build in the Gradle daemon.
            analyzers.apply {
                jarEnabled.set(true)
                archiveEnabled.set(true)
                // Ecosystems a JVM build has no use for, and lookups that need their own credentials.
                assemblyEnabled.set(false)
                nuspecEnabled.set(false)
                nugetconfEnabled.set(false)
                nodeEnabled.set(false)
                nodeAudit.enabled.set(false)
                retirejs.enabled.set(false)
                centralEnabled.set(false)
                nexusEnabled.set(false)
                ossIndexEnabled.set(false)
            }
            configureNvd(project, extension, nvd)
        }
        project.tasks.named("dependencyCheckAnalyze") { task ->
            task.doLast {
                LogUtil.verbose("✅ OWASP Dependency Check analysis completed")
                LogUtil.verbose("📊 Reports available at: build/reports/dependency-check/")
            }
        }
    }

    /**
     * A user-supplied suppression file wins; otherwise armor generates its default under `build/`.
     * See [SpotbugsConfigurator] for why this is a task rather than a configuration-time write.
     *
     * dependency-check takes the path as a String, so the provider is mapped to an absolute path;
     * the task dependency is still carried by the provider.
     */
    private fun resolveSuppressionFile(
        project: Project,
        extension: CodeArmorExtension,
    ): Provider<String> {
        extension.owaspSuppressionFile?.let { configuredPath ->
            val suppressionFileObj = project.file(configuredPath)
            if (suppressionFileObj.exists()) {
                return project.provider { suppressionFileObj.absolutePath }
            }
            LogUtil.essential("⚠️ OWASP suppression file not found, using armor default: $configuredPath")
        }

        val generator =
            project.tasks.register(
                "generateOwaspSuppressionFile",
                GenerateConfigFileTask::class.java,
            ) { task ->
                task.description = "Generates the default OWASP dependency-check suppression file"
                task.content.set(FileUtil.defaultOwaspSuppressionContent())
                task.outputFile.set(
                    project.layout.buildDirectory.file("codearmor/${FileUtil.OWASP_SUPPRESSION_FILENAME}"),
                )
            }

        return generator.flatMap { it.outputFile }.map { it.asFile.absolutePath }
    }

    private fun configureNvd(
        project: Project,
        extension: CodeArmorExtension,
        nvd: NvdExtension,
    ) {
        val apiKey =
            extension.owaspNvdApiKey
                ?: project.findProperty("nvd.api.key") as? String
                ?: System.getenv("NVD_API_KEY")
                ?: System.getProperty("nvd.api.key")
        nvd.delay.set(extension.owaspNvdApiDelay)
        nvd.maxRetryCount.set(extension.owaspNvdMaxRetryCount)
        nvd.validForHours.set(extension.owaspNvdValidForHours)
        if (apiKey != null) {
            LogUtil.essential("🔑 CodeArmor: Using NVD API key for faster vulnerability lookups")
            nvd.apiKey.set(apiKey)
        } else {
            LogUtil.essential("⚠️  CodeArmor: No NVD API key configured. Using slower public access.")
            LogUtil.essential("💡 To speed up scans, set NVD_API_KEY environment variable or configure in build.gradle")
            LogUtil.essential("🌐 Get your free API key at: https://nvd.nist.gov/developers/request-an-api-key")
        }
    }
}
