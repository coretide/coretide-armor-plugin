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

import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsExtension
import com.github.spotbugs.snom.SpotBugsTask
import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.config.SpotBugsConfig
import dev.coretide.plugin.armor.util.FileUtil
import dev.coretide.plugin.armor.util.LogUtil
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

object SpotbugsConfigurator {
    fun configureSpotbugs(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        project.pluginManager.apply("com.github.spotbugs")

        val config = extension.spotbugsConfig

        project.configure<SpotBugsExtension> {
            toolVersion.set(config.toolVersion)
            ignoreFailures.set(config.ignoreFailures)
            showStackTraces.set(config.showStackTraces)
            showProgress.set(config.showProgress)
            effort.set(Effort.valueOf(config.effort))
            reportLevel.set(Confidence.valueOf(config.reportLevel))
            resolveExcludeFile(project, config).let { excludeFilter.set(it) }
            config.includeFile?.let { includeFilePath ->
                val includeFileObj = project.file(includeFilePath)
                if (includeFileObj.exists()) {
                    includeFilter.set(includeFileObj)
                } else {
                    LogUtil.essential("⚠️ SpotBugs include file not found: $includeFilePath")
                }
            }
            config.maxHeap?.let { heap ->
                maxHeapSize.set(heap)
            }
            if (config.bugCategories.isNotEmpty()) {
                visitors.set(config.bugCategories)
            }
            if (config.extraArgs.isNotEmpty()) {
                extraArgs.addAll(config.extraArgs)
            }
            LogUtil.verbose("🔧 SpotBugs configuration:")
            LogUtil.verbose("   • Tool version: ${config.toolVersion}")
            LogUtil.verbose("   • Effort: ${config.effort}")
            LogUtil.verbose("   • Report level: ${config.reportLevel}")
            config.maxHeap?.let { LogUtil.verbose("   • Max heap: $it") }
            config.timeout?.let { LogUtil.verbose("   • Timeout: ${it}ms") }
            if (config.bugCategories.isNotEmpty()) {
                LogUtil.verbose("   • Bug categories: ${config.bugCategories.joinToString(", ")}")
            }
        }
        project.tasks.withType(SpotBugsTask::class.java).configureEach { task ->
            configureSpotBugsTask(task, project, config)
        }
        LogUtil.verbose("✅ SpotBugs configured with version ${config.toolVersion}")
    }

    private fun resolveExcludeFile(
        project: Project,
        config: SpotBugsConfig,
    ) = config.excludeFile?.let { excludeFilePath ->
        val excludeFileObj = project.file(excludeFilePath)
        if (excludeFileObj.exists()) {
            excludeFileObj
        } else {
            FileUtil.createDefaultSpotbugsExclude(project, excludeFileObj)
        }
    } ?: FileUtil.createDefaultSpotbugsExclude(project)

    private fun configureSpotBugsTask(
        task: SpotBugsTask,
        project: Project,
        config: SpotBugsConfig,
    ) {
        task.reports { reports ->
            if (config.xmlReports) {
                reports.create("xml") { report ->
                    report.required.set(true)
                    report.outputLocation.set(project.file("build/reports/spotbugs/${task.name}.xml"))
                }
            }

            if (config.htmlReports) {
                reports.create("html") { report ->
                    report.required.set(true)
                    report.outputLocation.set(project.file("build/reports/spotbugs/${task.name}.html"))
                }
            }

            if (config.textReports) {
                reports.create("text") { report ->
                    report.required.set(true)
                    report.outputLocation.set(project.file("build/reports/spotbugs/${task.name}.txt"))
                }
            }

            if (config.sarifReports) {
                try {
                    reports.create("sarif") { report ->
                        report.required.set(true)
                        report.outputLocation.set(project.file("build/reports/spotbugs/${task.name}.sarif"))
                    }
                } catch (_: Exception) {
                    LogUtil.essential("⚠️ SARIF reports not supported in this SpotBugs version")
                }
            }
        }

        task.doLast {
            LogUtil.verbose("✅ SpotBugs analysis completed for ${task.name}")
            if (config.htmlReports) {
                LogUtil.verbose("📊 HTML report: build/reports/spotbugs/${task.name}.html")
            }
            if (config.xmlReports) {
                LogUtil.verbose("📄 XML report: build/reports/spotbugs/${task.name}.xml")
            }
            if (config.textReports) {
                LogUtil.verbose("📝 Text report: build/reports/spotbugs/${task.name}.txt")
            }
        }
    }
}
