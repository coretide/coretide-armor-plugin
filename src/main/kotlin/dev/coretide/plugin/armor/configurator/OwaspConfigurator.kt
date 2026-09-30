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
import dev.coretide.plugin.armor.util.LogUtil
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension
import org.owasp.dependencycheck.gradle.extension.NvdExtension
import java.io.File

object OwaspConfigurator {
    private const val REPORT_DIRECTORY = "reports/dependency-check"

    /** The JSON report, one of the formats CodeArmor asks for; SonarQube reads it. */
    fun jsonReport(project: Project): File =
        project.layout.buildDirectory
            .file("$REPORT_DIRECTORY/dependency-check-report.json")
            .get()
            .asFile

    fun configureOwasp(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        project.pluginManager.apply("org.owasp.dependencycheck")
        project.configure<DependencyCheckExtension> {
            // dependency-check 12.2.x exposes lazy Gradle properties; plugin source must call
            // set() explicitly (the `=` form is a Kotlin DSL script-only convenience).
            failBuildOnCVSS.set(extension.owasp.failBuildOnCvss.get().toFloat())
            formats.set(listOf("HTML", "XML", "JSON", "SARIF"))
            outputDirectory.set(project.layout.buildDirectory.dir(REPORT_DIRECTORY))
            autoUpdate.set(extension.owasp.autoUpdate.get())
            // What ships, as in the SBOM. Left empty, every configuration is scanned, CodeArmor's own tools
            // (SpotBugs, detekt, PIT, Error Prone, JaCoCo) included, and a CVE in one of them fails the build.
            scanConfigurations.set(DependencyHealthConfigurator.SBOM_CONFIGURATIONS)
            ownSuppressionFile(project, extension)?.let { suppressionFile.set(it) }
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
     * The build's own suppression file, when it names one that exists. CodeArmor adds none of its own: a security
     * check's default must not hide findings, so dependency-check applies only its own base suppressions.
     */
    private fun ownSuppressionFile(
        project: Project,
        extension: CodeArmorExtension,
    ): String? {
        val configured = extension.owasp.suppressionFile.orNull ?: return null
        val file = project.file(configured)
        if (file.isFile) return file.absolutePath
        LogUtil.essential("⚠️ OWASP suppression file not found, so no suppressions apply: $configured")
        return null
    }

    /** The NVD API key: the build's own, then the `nvd.api.key` property, `NVD_API_KEY`, and the system property. */
    fun nvdApiKey(
        project: Project,
        extension: CodeArmorExtension,
    ): String? =
        extension.owasp.nvdApiKey.orNull
            ?: project.findProperty("nvd.api.key") as? String
            ?: System.getenv("NVD_API_KEY")
            ?: System.getProperty("nvd.api.key")

    private fun configureNvd(
        project: Project,
        extension: CodeArmorExtension,
        nvd: NvdExtension,
    ) {
        val apiKey = nvdApiKey(project, extension)
        nvd.delay.set(extension.owasp.nvdApiDelay.get())
        nvd.maxRetryCount.set(extension.owasp.nvdMaxRetryCount.get())
        nvd.validForHours.set(extension.owasp.nvdValidForHours.get())
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
