/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.armor.configurators

import dev.coretide.armor.CodeArmorExtension
import dev.coretide.armor.utils.FileUtils
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension

object OwaspConfigurator {
    fun configureOwasp(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        project.pluginManager.apply("org.owasp.dependencycheck")
        project.configure<DependencyCheckExtension> {
            skipProjects = listOf("*")
            failBuildOnCVSS = extension.owaspFailBuildOnCVSS.toFloat()
            formats = listOf("HTML", "XML", "JSON")
            outputDirectory = project.file("build/reports/dependency-check").absolutePath
            autoUpdate = extension.owaspAutoUpdate
            configureNvdApiSettings(project, extension)
            extension.owaspSuppressionFile?.let { suppressionFile ->
                val suppressionFileObj = project.file(suppressionFile)
                if (suppressionFileObj.exists()) {
                    this.suppressionFile = suppressionFileObj.absolutePath
                } else {
                    FileUtils.createDefaultOwaspSuppression(project, suppressionFileObj)
                    this.suppressionFile = suppressionFileObj.absolutePath
                }
            } ?: run {
                val defaultSuppressionFile = FileUtils.createDefaultOwaspSuppression(project)
                this.suppressionFile = defaultSuppressionFile.absolutePath
            }
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
                println("✅ OWASP Dependency Check analysis completed")
                println("📊 Reports available at: build/reports/dependency-check/")
            }
        }
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
            println("🔑 CodeArmor: Using NVD API key for faster vulnerability lookups")
            System.setProperty("nvd.api.key", apiKey)
            System.setProperty("nvd.api.delay", extension.owaspNvdApiDelay.toString())
            System.setProperty("nvd.api.max.retry.count", extension.owaspNvdMaxRetryCount.toString())
            System.setProperty("nvd.api.valid.for.hours", extension.owaspNvdValidForHours.toString())
            System.setProperty("nvd.api.datafeed.validation.enabled", "true")
            System.setProperty("nvd.api.endpoint", "https://services.nvd.nist.gov/rest/json/cves/2.0/")
        } else {
            println("⚠️  CodeArmor: No NVD API key configured. Using slower public access.")
            println("💡 To speed up scans, set NVD_API_KEY environment variable or configure in build.gradle")
            println("🌐 Get your free API key at: https://nvd.nist.gov/developers/request-an-api-key")
        }
    }
}
