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

object OwaspConfigurator {
    fun configureOwasp(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        project.pluginManager.apply("org.owasp.dependencycheck")
        project.configure<DependencyCheckExtension> {
            // dependency-check 12.2.x exposes lazy Gradle properties; plugin source must call
            // set() explicitly (the `=` form is a Kotlin DSL script-only convenience).
            skipProjects.set(listOf("*"))
            failBuildOnCVSS.set(extension.owaspFailBuildOnCVSS.toFloat())
            formats.set(listOf("HTML", "XML", "JSON"))
            outputDirectory.set(project.layout.buildDirectory.dir("reports/dependency-check"))
            autoUpdate.set(extension.owaspAutoUpdate)
            configureNvdApiSettings(project, extension)
            this.suppressionFile.set(resolveSuppressionFile(project, extension))
            // These System properties are set during *configuration*. That is only safe because
            // dependencyCheckAnalyze is marked notCompatibleWithConfigurationCache (see
            // ConfigurationCacheUtil), which forces a full configuration whenever it runs. If that
            // opt-out is ever removed, a configuration-cache hit would skip these and the scan
            // would silently run without the NVD API key and with the disabled analyzers re-enabled.
            System.setProperty("dependencycheck.autoUpdate", extension.owaspAutoUpdate.toString())
            System.setProperty("dependencycheck.failBuildOnCVSS", extension.owaspFailBuildOnCVSS.toString())
            System.setProperty("dependencycheck.formats", "HTML,XML,JSON")
            System.setProperty("dependencycheck.outputDirectory", project.file("build/reports/dependency-check").absolutePath)
            System.setProperty("dependencycheck.writeReports", "true")
            System.setProperty("dependencycheck.reportFormat", "ALL")
            System.setProperty("analyzer.assembly.enabled", "false")
            System.setProperty("analyzer.nuspec.enabled", "false")
            System.setProperty("analyzer.nugetconf.enabled", "false")
            System.setProperty("analyzer.central.enabled", "false")
            System.setProperty("analyzer.nexus.enabled", "false")
            System.setProperty("analyzer.node.enabled", "false")
            System.setProperty("analyzer.nodeAudit.enabled", "false")
            System.setProperty("analyzer.retirejs.enabled", "false")
            System.setProperty("analyzer.ossindex.enabled", "false")
            System.setProperty("analyzer.jar.enabled", "true")
            System.setProperty("analyzer.archive.enabled", "true")
            System.setProperty("analyzer.filename.enabled", "true")
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

    private fun configureNvdApiSettings(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        val apiKey =
            extension.owaspNvdApiKey
                ?: project.findProperty("nvd.api.key") as? String
                ?: System.getenv("NVD_API_KEY")
                ?: System.getProperty("nvd.api.key")
        if (apiKey != null) {
            LogUtil.essential("🔑 CodeArmor: Using NVD API key for faster vulnerability lookups")
            System.setProperty("nvd.api.key", apiKey)
            System.setProperty("nvd.api.delay", extension.owaspNvdApiDelay.toString())
            System.setProperty("nvd.api.max.retry.count", extension.owaspNvdMaxRetryCount.toString())
            System.setProperty("nvd.api.valid.for.hours", extension.owaspNvdValidForHours.toString())
            System.setProperty("nvd.api.datafeed.validation.enabled", "true")
            System.setProperty("nvd.api.endpoint", "https://services.nvd.nist.gov/rest/json/cves/2.0/")
        } else {
            LogUtil.essential("⚠️  CodeArmor: No NVD API key configured. Using slower public access.")
            LogUtil.essential("💡 To speed up scans, set NVD_API_KEY environment variable or configure in build.gradle")
            LogUtil.essential("🌐 Get your free API key at: https://nvd.nist.gov/developers/request-an-api-key")
        }
    }
}
