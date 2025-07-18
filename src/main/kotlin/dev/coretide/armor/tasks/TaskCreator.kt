/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.armor.tasks

import dev.coretide.armor.CodeArmorExtension
import dev.coretide.armor.ProjectType
import dev.coretide.armor.utils.ProjectDetector
import org.gradle.api.Project

object TaskCreator {
    fun createCustomTasks(
        project: Project,
        extension: CodeArmorExtension,
        projectType: ProjectType,
    ) {
        createQuickBuildTask(project)
        if (hasFormattingToolsEnabled(extension)) {
            createFormatCodeTask(project, extension)
        }
        if (hasQualityToolsEnabled(extension, projectType)) {
            createCodeQualityTask(project, extension, projectType)
        }
        if (hasSecurityToolsEnabled(extension)) {
            createFullAnalysisTask(project, extension)
        }
        createLogExclusionInfoTask(project)
    }

    private fun hasQualityToolsEnabled(
        extension: CodeArmorExtension,
        projectType: ProjectType,
    ): Boolean =
        extension.jacoco ||
            (extension.checkstyle && ProjectDetector.needsCheckstyle(projectType)) ||
            extension.spotbugs ||
            extension.spotless ||
            extension.sonarqube

    private fun hasFormattingToolsEnabled(extension: CodeArmorExtension): Boolean = extension.spotless

    private fun hasSecurityToolsEnabled(extension: CodeArmorExtension): Boolean = extension.owasp || extension.veracode

    private fun createQuickBuildTask(project: Project) {
        project.tasks.register("quickBuild") { task ->
            task.group = "build"
            task.description = "⚡ Fast development build (compile + test only, no quality checks)"
            val projectName = project.name
            task.dependsOn("assemble", "test")
            task.doLast {
                println("⚡ Quick build completed for $projectName")
                println("🚀 Ready for development (no quality checks)")
                println("💡 Run './gradlew codeQuality' before pushing")
            }
        }
    }

    private fun createFormatCodeTask(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        project.tasks.register("formatCode") { task ->
            task.group = "formatting"
            task.description = "⚡ Quick code formatting (development workflow)"
            val dependencies = mutableListOf<String>()
            if (extension.spotless) dependencies.add("spotlessApply")
            if (dependencies.isNotEmpty()) {
                task.dependsOn(*dependencies.toTypedArray())
            }
            task.doLast {
                println("✅ Code formatting completed successfully")
                if (extension.spotless) {
                    val formatter = extension.kotlinFormatter.name.lowercase()
                    println("🎨 Spotless formatting applied using $formatter")
                }
            }
        }
    }

    private fun createLogExclusionInfoTask(project: Project) {
        project.tasks.register("logExclusionInfo", LogExclusionInfoTask::class.java) { task ->
            task.group = "verification"
            task.description = "📋 Log coverage exclusion information for debugging"
        }
    }

    private fun createCodeQualityTask(
        project: Project,
        extension: CodeArmorExtension,
        projectType: ProjectType,
    ) {
        project.tasks.register("codeQuality") { task ->
            task.group = "verification"
            task.description = "🔍 All code quality checks"
            val projectName = project.name
            val dependencies = mutableListOf<String>()
            if (extension.checkstyle && ProjectDetector.needsCheckstyle(projectType)) {
                dependencies.addAll(listOf("checkstyleMain", "checkstyleTest"))
            }
            if (extension.spotless) dependencies.add("spotlessCheck")
            if (extension.spotbugs) dependencies.add("spotbugsMain")
            if (extension.jacoco) {
                dependencies.addAll(listOf("jacocoTestReport", "jacocoTestCoverageVerification"))
            }
            if (extension.sonarqube) dependencies.add("sonar")
            if (dependencies.isNotEmpty()) {
                task.dependsOn(*dependencies.toTypedArray())
            }
            task.doLast {
                println("✅ Pre-push code quality checks completed for $projectName")
                if (extension.jacoco) {
                    println("📊 JaCoCo coverage: build/reports/jacoco/test/html/index.html")
                }
                if (extension.spotbugs) {
                    println("📊 SpotBugs report: build/reports/spotbugs/main.html")
                }
                if (extension.checkstyle && ProjectDetector.needsCheckstyle(projectType)) {
                    println("📊 Checkstyle report: build/reports/checkstyle/main.html")
                }
                if (extension.sonarqube) {
                    println("🔍 SonarQube analysis uploaded")
                }
                println("🚀 Ready to push to SCM!")
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
                println("✅ Full analysis completed for $projectName")
                if (extension.owasp) {
                    println("📊 OWASP report: build/reports/dependency-check/dependency-check-report.html")
                }
                if (extension.veracode) {
                    if (hasVeracodeCredentials()) {
                        println("🔍 Veracode scan uploaded")
                    } else {
                        println("⚠️  Veracode credentials not found - scan skipped")
                    }
                }
                println("🎯 Complete analysis pipeline finished!")
            }
        }
    }

    private fun hasVeracodeCredentials(): Boolean =
        System.getenv("VERACODE_USERNAME") != null &&
            System.getenv("VERACODE_PASSWORD") != null
}
