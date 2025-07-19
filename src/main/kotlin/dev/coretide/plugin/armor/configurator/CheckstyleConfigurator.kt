/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.configurator

import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.config.CheckstyleConfig
import dev.coretide.plugin.armor.util.FileUtil
import dev.coretide.plugin.armor.util.LogUtil
import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.kotlin.dsl.configure

object CheckstyleConfigurator {
    fun configureCheckstyle(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        project.pluginManager.apply("checkstyle")

        val config = extension.checkstyleConfig

        project.configure<CheckstyleExtension> {
            toolVersion = config.toolVersion
            maxWarnings = config.maxWarnings
            maxErrors = config.maxErrors
            isIgnoreFailures = config.ignoreFailures
            isShowViolations = config.showViolations
            val configFile = resolveConfigFile(project, config)
            this.configFile = configFile
            val suppressionFile = resolveSuppressionFile(project, config)
            val allConfigProperties =
                mutableMapOf<String, Any>().apply {
                    put("checkstyle.suppressions.file", suppressionFile.absolutePath)
                    putAll(config.configProperties)
                }
            configProperties = allConfigProperties
            LogUtil.verbose("🔧 Checkstyle configuration:")
            LogUtil.verbose("   • Tool version: ${config.toolVersion}")
            LogUtil.verbose("   • Config file: ${configFile.absolutePath}")
            LogUtil.verbose("   • Suppression file: ${suppressionFile.absolutePath}")
            LogUtil.verbose("   • Max warnings: ${config.maxWarnings}")
            LogUtil.verbose("   • Max errors: ${config.maxErrors}")
            if (config.configProperties.isNotEmpty()) {
                LogUtil.verbose("   • Custom properties: ${config.configProperties.keys.joinToString(", ")}")
            }
        }
        project.tasks.withType(Checkstyle::class.java).configureEach { task ->
            configureCheckstyleTask(task, config)
        }
        LogUtil.essential("✅ Checkstyle configured with version ${config.toolVersion}")
    }

    private fun resolveConfigFile(
        project: Project,
        config: CheckstyleConfig,
    ) = config.configFile?.let { project.file(it) }
        ?: FileUtil.createDefaultCheckstyleConfig(project)

    private fun resolveSuppressionFile(
        project: Project,
        config: CheckstyleConfig,
    ) = config.suppressionFile?.let { project.file(it) }
        ?: FileUtil.createDefaultCheckstyleSuppression(project)

    private fun configureCheckstyleTask(
        task: Checkstyle,
        config: CheckstyleConfig,
    ) {
        task.include(*config.targetIncludes.toTypedArray())
        task.exclude(*config.targetExcludes.toTypedArray())
        task.reports { reports ->
            reports.xml.required.set(config.xmlReports)
            reports.html.required.set(config.htmlReports)
            if (config.sarifReports) {
                try {
                    @Suppress("UnstableApiUsage")
                    reports.sarif.required.set(true)
                } catch (_: Exception) {
                    LogUtil.essential("⚠️ SARIF reports not supported in this Gradle version")
                }
            }
        }

        task.doLast {
            LogUtil.verbose("✅ Checkstyle analysis completed")
            if (config.htmlReports) {
                LogUtil.verbose("📊 HTML report: ${task.reports.html.outputLocation.get().asFile}")
            }
            if (config.xmlReports) {
                LogUtil.verbose("📄 XML report: ${task.reports.xml.outputLocation.get().asFile}")
            }
            if (config.enableRulesSummary &&
                task.reports.html.required
                    .get()
            ) {
                LogUtil.verbose("📈 Check the HTML report for detailed rule violations summary")
            }
        }
    }
}
