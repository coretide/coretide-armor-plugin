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
import dev.coretide.plugin.armor.configurator.ApiCompatibilityConfigurator
import dev.coretide.plugin.armor.configurator.DependencyHealthConfigurator
import dev.coretide.plugin.armor.configurator.DetektConfigurator
import dev.coretide.plugin.armor.configurator.KoverConfigurator
import dev.coretide.plugin.armor.util.LogUtil
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin

object TaskCreator {
    fun createCustomTasks(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        // The check tiers need a JVM project: compile, test and the SpotBugs and JaCoCo tasks only
        // exist once a Java plugin is applied. Without this, `build` would fail in, say, a docs or
        // aggregator project that applies CodeArmor.
        if (project.plugins.hasPlugin(JavaBasePlugin::class.java)) {
            createQuickBuildTask(project)
            val buildTier = tasksPresent(project, extension.checks.build.get(), defaultBuildTier(extension), OPTIONAL_DEFAULT_TASKS)
            val ciTier = tasksPresent(project, extension.checks.ci.get(), defaultCiTier(extension), OPTIONAL_CI_TASKS)
            if (buildTier.isNotEmpty()) {
                createCodeQualityTask(project, extension, buildTier)
            }
            if (buildTier.isNotEmpty() || ciTier.isNotEmpty() || extension.veracode) {
                createFullAnalysisTask(project, extension, ciTier)
            }
        }
        createLogExclusionInfoTask(project, extension)
        createScaffoldConfigsTask(project)
    }

    /** The local checks `codeQuality`, and so `build`, runs by default: those of the tools switched on. */
    fun defaultBuildTier(extension: CodeArmorExtension): List<String> =
        buildList {
            if (extension.spotbugs) add("spotbugsMain")
            if (extension.detekt) add(DetektConfigurator.TASK_NAME)
            // With kover = true, Kotlin projects run Kover's tasks and Java-only projects JaCoCo's; each project
            // leaves out the ones it does not have.
            if (extension.kover) addAll(KoverConfigurator.BUILD_TIER_TASKS)
            if (extension.jacoco || extension.kover) addAll(JACOCO_TASKS)
            if (extension.kotlinAbiValidation) add(ApiCompatibilityConfigurator.KOTLIN_ABI_CHECK_TASK)
        }

    private val JACOCO_TASKS = listOf("jacocoTestReport", "jacocoTestCoverageVerification")

    /**
     * Default tier tasks that only some projects have: detekt exists in Kotlin projects only, and with
     * kover = true a project has either Kover's or JaCoCo's tasks.
     */
    private val OPTIONAL_DEFAULT_TASKS =
        setOf(DetektConfigurator.TASK_NAME, ApiCompatibilityConfigurator.KOTLIN_ABI_CHECK_TASK) + KoverConfigurator.BUILD_TIER_TASKS +
            JACOCO_TASKS

    /**
     * The tier's tasks that exist in this project. A default task that only some projects have is left out
     * where it is missing; a task the build listed itself is kept, so a typo still fails the build.
     */
    private fun tasksPresent(
        project: Project,
        tier: List<String>,
        defaults: List<String>,
        optional: Set<String>,
    ): List<String> = tier.filter { name -> name !in optional || name !in defaults || name in project.tasks.names }

    /** The network and server checks `fullAnalysis` adds by default: those of the tools switched on. */
    fun defaultCiTier(extension: CodeArmorExtension): List<String> =
        buildList {
            if (extension.owasp) add("dependencyCheckAnalyze")
            if (extension.dependencyUpdates) add(DependencyHealthConfigurator.UPDATES_TASK)
            if (extension.sbom) add(DependencyHealthConfigurator.LICENSE_TASK)
            if (extension.dependencyAnalysis) add(DependencyHealthConfigurator.ANALYSIS_TASK)
            if (!extension.apiBaseline.isNullOrBlank()) add(ApiCompatibilityConfigurator.API_CHECK_TASK)
            if (extension.sonarqube) add("sonar")
        }

    /** The dependency analysis only covers Java and Kotlin projects, and the API check only libraries. */
    private val OPTIONAL_CI_TASKS = setOf(DependencyHealthConfigurator.ANALYSIS_TASK, ApiCompatibilityConfigurator.API_CHECK_TASK)

    private fun createScaffoldConfigsTask(project: Project) {
        project.tasks.register("armorScaffoldConfigs", ScaffoldConfigsTask::class.java) { task ->
            task.configDirectory.set(project.layout.projectDirectory.dir("config"))
        }
    }

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

    /**
     * The build tier: local checks that need no network. `check`, and so `build`, depends on it.
     */
    private fun createCodeQualityTask(
        project: Project,
        extension: CodeArmorExtension,
        buildTier: List<String>,
    ) {
        val codeQuality =
            project.tasks.register("codeQuality") { task ->
                task.group = "verification"
                task.description = "🔍 Local code quality checks (also run by build)"
                val projectName = project.name
                task.dependsOn(buildTier)
                task.doLast {
                    LogUtil.verbose("✅ Code quality checks completed for $projectName")
                    if (extension.jacoco) {
                        LogUtil.verbose("📊 JaCoCo coverage: build/reports/jacoco/test/html/index.html")
                    }
                    if (extension.spotbugs) {
                        LogUtil.verbose("📊 SpotBugs report: build/reports/spotbugs/main.html")
                    }
                }
            }
        project.plugins.withType(LifecycleBasePlugin::class.java) {
            project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) { check ->
                check.dependsOn(codeQuality)
            }
        }
    }

    /**
     * The CI tier: the local checks plus those that need a network or a server.
     */
    private fun createFullAnalysisTask(
        project: Project,
        extension: CodeArmorExtension,
        ciTier: List<String>,
    ) {
        project.tasks.register("fullAnalysis") { task ->
            task.group = "verification"
            task.description = "🔒 Full security + quality analysis, for CI"
            val projectName = project.name
            if ("codeQuality" in project.tasks.names) {
                task.dependsOn("codeQuality")
            }
            task.dependsOn(ciTier)
            if (extension.veracode && hasVeracodeCredentials()) {
                // CodeArmor does not create veracodeUpload; a separately applied Veracode plugin
                // does. Matched by name, lazily, so fullAnalysis still runs when no such plugin
                // is applied and no other task gets created just to be checked.
                task.dependsOn(project.tasks.named { name -> name == "veracodeUpload" })
            }
            val veracodeUploadPresent = "veracodeUpload" in project.tasks.names
            task.doLast {
                LogUtil.verbose("✅ Full analysis completed for $projectName")
                if (extension.owasp) {
                    LogUtil.verbose("📊 OWASP report: build/reports/dependency-check/dependency-check-report.html")
                }
                if (extension.sonarqube) {
                    LogUtil.verbose("🔍 SonarQube analysis uploaded")
                }
                if (extension.veracode) {
                    when {
                        !hasVeracodeCredentials() -> LogUtil.verbose("⚠️  Veracode credentials not found - scan skipped")
                        !veracodeUploadPresent -> LogUtil.verbose("⚠️  No veracodeUpload task - scan skipped")
                        else -> LogUtil.verbose("🔍 Veracode scan uploaded")
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
