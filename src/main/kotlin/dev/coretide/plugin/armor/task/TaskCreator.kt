/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.task

import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.util.LogUtil
import org.gradle.api.Project

object TaskCreator {
    fun createCustomTasks(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        createQuickBuildTask(project)
        if (hasQualityToolsEnabled(extension)) {
            createCodeQualityTask(project, extension)
        }
        if (hasSecurityToolsEnabled(extension)) {
            createFullAnalysisTask(project, extension)
        }
        createLogExclusionInfoTask(project, extension)
        createScaffoldConfigsTask(project)
    }

    private fun createScaffoldConfigsTask(project: Project) {
        project.tasks.register("armorScaffoldConfigs", ScaffoldConfigsTask::class.java) { task ->
            task.configDirectory.set(project.layout.projectDirectory.dir("config"))
        }
    }

    private fun hasQualityToolsEnabled(extension: CodeArmorExtension): Boolean =
        extension.jacoco ||
            extension.spotbugs ||
            extension.sonarqube

    private fun hasSecurityToolsEnabled(extension: CodeArmorExtension): Boolean = extension.owasp || extension.veracode

    private fun createQuickBuildTask(project: Project) {
        project.tasks.register("quickBuild") { task ->
            task.group = "build"
            task.description = "⚡ Fast development build (compile + test only, no quality checks)"
            val projectName = project.name
            task.dependsOn("assemble", "test")
            task.doLast {
                LogUtil.verbose("⚡ Quick build completed for $projectName")
                LogUtil.verbose("🚀 Ready for development (no quality checks)")
                LogUtil.verbose("💡 Run './gradlew codeQuality' before pushing")
            }
        }
    }

    private fun createLogExclusionInfoTask(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        project.tasks.register("logExclusionInfo", LogExclusionInfoTask::class.java) { task ->
            task.group = "verification"
            task.description = "📋 Log coverage exclusion information for debugging"
            task.reportLines.set(LogExclusionInfoTask.renderReport(extension))
        }
    }

    private fun createCodeQualityTask(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        project.tasks.register("codeQuality") { task ->
            task.group = "verification"
            task.description = "🔍 All code quality checks"
            val projectName = project.name
            val dependencies = mutableListOf<String>()
            if (extension.spotbugs) dependencies.add("spotbugsMain")
            if (extension.jacoco) {
                dependencies.addAll(listOf("jacocoTestReport", "jacocoTestCoverageVerification"))
            }
            if (extension.sonarqube) dependencies.add("sonar")
            if (dependencies.isNotEmpty()) {
                task.dependsOn(*dependencies.toTypedArray())
            }
            task.doLast {
                LogUtil.verbose("✅ Pre-push code quality checks completed for $projectName")
                if (extension.jacoco) {
                    LogUtil.verbose("📊 JaCoCo coverage: build/reports/jacoco/test/html/index.html")
                }
                if (extension.spotbugs) {
                    LogUtil.verbose("📊 SpotBugs report: build/reports/spotbugs/main.html")
                }
                if (extension.sonarqube) {
                    LogUtil.verbose("🔍 SonarQube analysis uploaded")
                }
                LogUtil.verbose("🚀 Ready to push to SCM!")
            }
        }
    }

    private fun createFullAnalysisTask(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        project.tasks.register("fullAnalysis") { task ->
            task.group = "verification"
            task.description = "🔒 Full security + quality analysis"
            val projectName = project.name
            val dependencies = mutableListOf<String>()
            if (project.tasks.findByName("codeQuality") != null) {
                dependencies.add("codeQuality")
            }
            if (extension.owasp) dependencies.add("dependencyCheckAnalyze")

            if (extension.veracode && hasVeracodeCredentials()) {
                dependencies.add("veracodeUpload")
            }
            if (dependencies.isNotEmpty()) {
                task.dependsOn(*dependencies.toTypedArray())
            }
            task.doLast {
                LogUtil.verbose("✅ Full analysis completed for $projectName")
                if (extension.owasp) {
                    LogUtil.verbose("📊 OWASP report: build/reports/dependency-check/dependency-check-report.html")
                }
                if (extension.veracode) {
                    if (hasVeracodeCredentials()) {
                        LogUtil.verbose("🔍 Veracode scan uploaded")
                    } else {
                        LogUtil.verbose("⚠️  Veracode credentials not found - scan skipped")
                    }
                }
                LogUtil.verbose("🎯 Complete analysis pipeline finished!")
            }
        }
    }

    private fun hasVeracodeCredentials(): Boolean =
        System.getenv("VERACODE_USERNAME") != null &&
            System.getenv("VERACODE_PASSWORD") != null
}
