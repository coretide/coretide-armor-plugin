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
import dev.coretide.plugin.armor.ProjectType
import dev.coretide.plugin.armor.configurator.ApiCompatibilityConfigurator
import dev.coretide.plugin.armor.configurator.DependencyHealthConfigurator
import dev.coretide.plugin.armor.configurator.DetektConfigurator
import dev.coretide.plugin.armor.configurator.DiffCoverageConfigurator
import dev.coretide.plugin.armor.configurator.IntegrationTestsConfigurator
import dev.coretide.plugin.armor.configurator.KoverConfigurator
import dev.coretide.plugin.armor.configurator.SonarqubeConfigurator
import dev.coretide.plugin.armor.configurator.VeracodeConfigurator
import dev.coretide.plugin.armor.util.LogUtil
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin

object TaskCreator {
    /**
     * The check tiers and the tasks around them. [repository] marks the project CodeArmor is applied to, rather
     * than a module of a multi-module build: its `armorInfo` also covers the git hooks.
     */
    fun createCustomTasks(
        project: Project,
        extension: CodeArmorExtension,
        projectType: ProjectType,
        repository: Boolean,
    ) {
        // The check tiers need a JVM project: compile, test and the SpotBugs and JaCoCo tasks only
        // exist once a Java plugin is applied. Without this, `build` would fail in, say, a docs or
        // aggregator project that applies CodeArmor.
        // Before the tiers, which leave out the default tasks a project does not have.
        if (repository && extension.secretScan.get()) SecretScanTask.register(project)
        var tiers: ArmorInfoTask.Tiers? = null
        if (project.plugins.hasPlugin(JavaBasePlugin::class.java)) {
            createQuickBuildTask(project)
            val buildTier = tasksPresent(project, extension.checks.build.get(), defaultBuildTier(extension), OPTIONAL_DEFAULT_TASKS)
            val ciTier = tasksPresent(project, extension.checks.ci.get(), defaultCiTier(project, extension), OPTIONAL_CI_TASKS)
            if (buildTier.isNotEmpty()) {
                createCodeQualityTask(project, extension, buildTier)
            }
            if (buildTier.isNotEmpty() || ciTier.isNotEmpty() || VeracodeConfigurator.enabled(extension)) {
                createFullAnalysisTask(project, extension, ciTier)
            }
            tiers = ArmorInfoTask.Tiers(buildTier, ciTier)
        }
        createLogExclusionInfoTask(project, extension)
        createScaffoldConfigsTask(project)
        ArmorInfoTask.register(project, extension, projectType.displayName, tiers, module = true, repository = repository)
    }

    /** The local checks `codeQuality`, and so `build`, runs by default: those of the tools switched on. */
    fun defaultBuildTier(extension: CodeArmorExtension): List<String> =
        buildList {
            if (extension.tests.integrationTests.get()) add(IntegrationTestsConfigurator.TASK_NAME)
            if (extension.spotbugs.enabled.get()) add("spotbugsMain")
            if (extension.detekt.enabled.get()) add(DetektConfigurator.taskName(extension))
            // With coverage.kover, Kotlin projects run Kover's tasks and Java-only projects JaCoCo's; each project
            // leaves out the ones it does not have.
            if (extension.coverage.kover.get()) addAll(KoverConfigurator.BUILD_TIER_TASKS)
            if (extension.coverage.enabled.get() || extension.coverage.kover.get()) addAll(JACOCO_TASKS)
            if (DiffCoverageConfigurator.enabled(extension)) add(DiffCoverageTask.TASK_NAME)
            if (extension.libraryApi.kotlinAbiValidation.get()) add(ApiCompatibilityConfigurator.KOTLIN_ABI_CHECK_TASK)
        }

    private val JACOCO_TASKS = listOf("jacocoTestReport", "jacocoTestCoverageVerification")

    /**
     * Default tier tasks that only some projects have: detekt exists in Kotlin projects only, with
     * coverage.kover a project has either Kover's or JaCoCo's tasks, and those, diff coverage and integration tests
     * need the Java plugin.
     */
    private val OPTIONAL_DEFAULT_TASKS =
        setOf(
            DetektConfigurator.TASK_NAME,
            DetektConfigurator.TYPE_RESOLUTION_TASK,
            ApiCompatibilityConfigurator.KOTLIN_ABI_CHECK_TASK,
            DiffCoverageTask.TASK_NAME,
            IntegrationTestsConfigurator.TASK_NAME,
        ) +
            KoverConfigurator.BUILD_TIER_TASKS + JACOCO_TASKS

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

    /**
     * The network and server checks `fullAnalysis` adds by default: those of the tools switched on, SonarQube
     * only once a server or token is configured.
     */
    fun defaultCiTier(
        project: Project,
        extension: CodeArmorExtension,
    ): List<String> =
        buildList {
            if (extension.owasp.enabled.get()) add("dependencyCheckAnalyze")
            if (extension.dependencyHealth.updates.get()) add(DependencyHealthConfigurator.UPDATES_TASK)
            if (extension.dependencyHealth.sbom.get()) add(DependencyHealthConfigurator.LICENSE_TASK)
            if (extension.dependencyHealth.analysis.get()) add(DependencyHealthConfigurator.ANALYSIS_TASK)
            if (!extension.libraryApi.baseline.orNull.isNullOrBlank()) add(ApiCompatibilityConfigurator.API_CHECK_TASK)
            if (extension.sonarqube.enabled.get() && SonarqubeConfigurator.isConfigured(project, extension)) add(SONAR_TASK)
            if (extension.secretScan.get()) add(SecretScanTask.TASK_NAME)
        }

    private const val SONAR_TASK = "sonar"

    /**
     * The dependency analysis only covers Java and Kotlin projects, and the API check only libraries. The secret scan
     * covers the repository, so a multi-module build runs it once, from the root.
     */
    private val OPTIONAL_CI_TASKS =
        setOf(DependencyHealthConfigurator.ANALYSIS_TASK, ApiCompatibilityConfigurator.API_CHECK_TASK, SecretScanTask.TASK_NAME)

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
                    if (extension.coverage.enabled.get()) {
                        LogUtil.verbose("📊 JaCoCo coverage: build/reports/jacoco/test/html/index.html")
                    }
                    if (extension.spotbugs.enabled.get()) {
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
            if (VeracodeConfigurator.enabled(extension) && hasVeracodeCredentials()) {
                // CodeArmor does not create veracodeUpload; a separately applied Veracode plugin
                // does. Matched by name, lazily, so fullAnalysis still runs when no such plugin
                // is applied and no other task gets created just to be checked.
                task.dependsOn(project.tasks.named { name -> name == "veracodeUpload" })
            }
            val veracodeUploadPresent = "veracodeUpload" in project.tasks.names
            val sonarInTier = SONAR_TASK in ciTier
            val sonarUnconfigured = extension.sonarqube.enabled.get() && !SonarqubeConfigurator.isConfigured(project, extension)
            task.doLast {
                LogUtil.verbose("✅ Full analysis completed for $projectName")
                if (extension.owasp.enabled.get()) {
                    LogUtil.verbose("📊 OWASP report: build/reports/dependency-check/dependency-check-report.html")
                }
                if (sonarInTier) {
                    LogUtil.verbose("🔍 SonarQube analysis uploaded")
                } else if (sonarUnconfigured) {
                    LogUtil.essential(
                        "⚠️  SonarQube skipped: no server configured. Set sonarqube { hostUrl } or SONAR_HOST_URL, " +
                            "or SONAR_TOKEN for SonarQube Cloud.",
                    )
                }
                if (VeracodeConfigurator.enabled(extension)) {
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
