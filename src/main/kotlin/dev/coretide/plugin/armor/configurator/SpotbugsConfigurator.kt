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
import dev.coretide.plugin.armor.task.GenerateConfigFileTask
import dev.coretide.plugin.armor.util.FileUtil
import dev.coretide.plugin.armor.util.LogUtil
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
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
            excludeFilter.set(resolveExcludeFile(project, config))
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

    /**
     * A user-supplied exclude file wins; otherwise armor generates its default under `build/`.
     *
     * Generation is a task rather than a configuration-time write: writing into the project tree
     * while configuring invalidates the configuration cache on the next run.
     */
    private fun resolveExcludeFile(
        project: Project,
        config: SpotBugsConfig,
    ): Provider<RegularFile> {
        config.excludeFile?.let { excludeFilePath ->
            val excludeFileObj = project.file(excludeFilePath)
            if (excludeFileObj.exists()) {
                return project.layout.file(project.provider { excludeFileObj })
            }
            LogUtil.essential("⚠️ SpotBugs exclude file not found, using armor default: $excludeFilePath")
        }

        val generator =
            project.tasks.register(
                "generateSpotbugsExcludeFile",
                GenerateConfigFileTask::class.java,
            ) { task ->
                task.description = "Generates the default SpotBugs exclude filter"
                task.content.set(FileUtil.defaultSpotbugsExcludeContent())
                task.outputFile.set(
                    project.layout.buildDirectory.file("codearmor/${FileUtil.SPOTBUGS_EXCLUDE_FILENAME}"),
                )
            }

        return generator.flatMap { it.outputFile }
    }

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
