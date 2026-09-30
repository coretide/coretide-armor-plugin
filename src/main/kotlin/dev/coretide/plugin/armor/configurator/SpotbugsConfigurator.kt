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
import dev.coretide.plugin.armor.task.SpotbugsBaselineTask
import dev.coretide.plugin.armor.util.FileUtil
import dev.coretide.plugin.armor.util.LogUtil
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import java.io.File
import java.time.Duration

object SpotbugsConfigurator {
    fun configureSpotbugs(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        project.pluginManager.apply("com.github.spotbugs")

        val config = extension.spotbugs

        project.configure<SpotBugsExtension> {
            toolVersion.set(config.toolVersion.get())
            ignoreFailures.set(config.ignoreFailures.get())
            showStackTraces.set(config.showStackTraces.get())
            showProgress.set(config.showProgress.get())
            effort.set(Effort.valueOf(config.effort.get()))
            reportLevel.set(Confidence.valueOf(config.reportLevel.get()))
            excludeFilter.set(resolveExcludeFile(project, config))
            config.includeFile.orNull?.let { includeFilePath ->
                val includeFileObj = project.file(includeFilePath)
                if (includeFileObj.exists()) {
                    includeFilter.set(includeFileObj)
                } else {
                    LogUtil.essential("⚠️ SpotBugs include file not found: $includeFilePath")
                }
            }
            config.maxHeap.orNull?.let { heap ->
                maxHeapSize.set(heap)
            }
            if (config.bugCategories.get().isNotEmpty()) {
                visitors.set(config.bugCategories)
            }
            extraArgs.addAll(config.extraArgs)
            LogUtil.verbose("🔧 SpotBugs configuration:")
            LogUtil.verbose("   • Tool version: ${config.toolVersion.get()}")
            LogUtil.verbose("   • Effort: ${config.effort.get()}")
            LogUtil.verbose("   • Report level: ${config.reportLevel.get()}")
            config.maxHeap.orNull?.let { LogUtil.verbose("   • Max heap: $it") }
            config.timeout.orNull?.let { LogUtil.verbose("   • Timeout: ${it}ms") }
            if (config.bugCategories.get().isNotEmpty()) {
                LogUtil.verbose("   • Bug categories: ${config.bugCategories.get().joinToString(", ")}")
            }
        }
        val baseline = project.file(config.baselineFile.get())
        // Writing the baseline needs every finding: none may fail the build, or be left out by the old baseline.
        val writingBaseline = project.gradle.startParameter.taskNames.any { it.substringAfterLast(':') == SpotbugsBaselineTask.TASK_NAME }
        project.tasks.withType(SpotBugsTask::class.java).configureEach { task ->
            val main = task.name == MAIN_TASK
            configureSpotBugsTask(task, project, config, xml = config.xmlReports.get() || (main && writingBaseline))
            // Until 0.5.0 the timeout was only logged.
            config.timeout.orNull?.let { task.timeout.set(Duration.ofMillis(it.toLong())) }
            if (main && writingBaseline) {
                task.ignoreFailures = true
            } else if (main) {
                task.baselineFile.set(project.layout.file(project.provider { baseline.takeIf { it.isFile } }))
            }
        }
        project.plugins.withType(JavaPlugin::class.java) {
            project.tasks.register(SpotbugsBaselineTask.TASK_NAME, SpotbugsBaselineTask::class.java) { task ->
                task.dependsOn(MAIN_TASK)
                task.report.set(reportFile(project, MAIN_TASK, "xml"))
                task.baseline.set(baseline)
            }
        }
        LogUtil.verbose("✅ SpotBugs configured with version ${config.toolVersion.get()}")
    }

    const val MAIN_TASK = "spotbugsMain"

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
        config.excludeFile.orNull?.let { excludeFilePath ->
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

    /** Where a SpotBugs task writes its report; SonarQube reads the main task's XML from here. */
    fun reportFile(
        project: Project,
        taskName: String,
        extension: String,
    ): File = project.file("build/reports/spotbugs/$taskName.$extension")

    private fun configureSpotBugsTask(
        task: SpotBugsTask,
        project: Project,
        config: SpotBugsConfig,
        xml: Boolean,
    ) {
        task.reports { reports ->
            if (xml) {
                reports.create("xml") { report ->
                    report.required.set(true)
                    report.outputLocation.set(reportFile(project, task.name, "xml"))
                }
            }

            if (config.htmlReports.get()) {
                reports.create("html") { report ->
                    report.required.set(true)
                    report.outputLocation.set(reportFile(project, task.name, "html"))
                }
            }

            if (config.textReports.get()) {
                reports.create("text") { report ->
                    report.required.set(true)
                    report.outputLocation.set(reportFile(project, task.name, "txt"))
                }
            }

            if (config.sarifReports.get()) {
                try {
                    reports.create("sarif") { report ->
                        report.required.set(true)
                        report.outputLocation.set(reportFile(project, task.name, "sarif"))
                    }
                } catch (_: Exception) {
                    LogUtil.essential("⚠️ SARIF reports not supported in this SpotBugs version")
                }
            }
        }

        val html = config.htmlReports.get()
        val text = config.textReports.get()
        task.doLast {
            LogUtil.verbose("✅ SpotBugs analysis completed for ${task.name}")
            if (html) {
                LogUtil.verbose("📊 HTML report: build/reports/spotbugs/${task.name}.html")
            }
            if (xml) {
                LogUtil.verbose("📄 XML report: build/reports/spotbugs/${task.name}.xml")
            }
            if (text) {
                LogUtil.verbose("📝 Text report: build/reports/spotbugs/${task.name}.txt")
            }
        }
    }
}
