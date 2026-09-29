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

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.github.benmanes.gradle.versions.updates.resolutionstrategy.ComponentFilter
import com.github.benmanes.gradle.versions.updates.resolutionstrategy.ComponentSelectionWithCurrent
import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.ProjectType
import dev.coretide.plugin.armor.task.LicenseReportTask
import dev.coretide.plugin.armor.util.LogUtil
import org.cyclonedx.gradle.BaseCyclonedxTask
import org.cyclonedx.gradle.CyclonedxAggregateTask
import org.cyclonedx.gradle.CyclonedxDirectTask
import org.cyclonedx.model.Component
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

/**
 * What the project depends on: newer versions (`dependencyUpdates`), a CycloneDX SBOM with a licence report built
 * from it (`cyclonedxBom`, `armorLicenseReport`), and, opt-in, dependencies that are declared but unused or used
 * but not declared (`projectHealth`).
 */
object DependencyHealthConfigurator {
    const val UPDATES_TASK = "dependencyUpdates"
    const val SBOM_TASK = "cyclonedxBom"
    const val LICENSE_TASK = "armorLicenseReport"
    const val ANALYSIS_TASK = "projectHealth"
    const val ANALYSIS_PLUGIN_ID = "com.autonomousapps.dependency-analysis"

    private const val VERSIONS_PLUGIN_ID = "com.github.ben-manes.versions"
    private const val SBOM_PLUGIN_ID = "org.cyclonedx.bom"

    /** What ships: the SBOM leaves out test and tool configurations, such as SpotBugs' and JaCoCo's. */
    val SBOM_CONFIGURATIONS = listOf("runtimeClasspath")

    fun configure(
        project: Project,
        extension: CodeArmorExtension,
        projectType: ProjectType,
    ) {
        if (extension.dependencyUpdates) configureUpdates(project)
        if (extension.sbom) configureSbom(project, extension, projectType)
        if (extension.dependencyAnalysis) configureAnalysis(project)
    }

    /** The dependency-analysis plugin must also be on the root project of a multi-module build. */
    fun configureAnalysisRoot(
        root: Project,
        extension: CodeArmorExtension,
    ) {
        if (extension.dependencyAnalysis) root.pluginManager.apply(ANALYSIS_PLUGIN_ID)
    }

    private fun configureUpdates(project: Project) {
        project.pluginManager.apply(VERSIONS_PLUGIN_ID)
        project.tasks.named(UPDATES_TASK, DependencyUpdatesTask::class.java).configure { task ->
            task.rejectVersionIf(RejectUnstableUpgrade)
            task.outputFormatter = "plain,json,html"
            task.notCompatibleWithConfigurationCache("The versions plugin reads the project's configurations as it runs")
        }
    }

    private fun configureSbom(
        project: Project,
        extension: CodeArmorExtension,
        projectType: ProjectType,
    ) {
        project.pluginManager.apply(SBOM_PLUGIN_ID)
        val type = if (projectType.isApplication) Component.Type.APPLICATION else Component.Type.LIBRARY
        project.tasks.withType(BaseCyclonedxTask::class.java).configureEach { it.projectType.convention(type) }
        project.tasks.withType(CyclonedxDirectTask::class.java).configureEach { it.includeConfigs.convention(SBOM_CONFIGURATIONS) }
        val sbom = project.tasks.named(SBOM_TASK, CyclonedxAggregateTask::class.java)
        project.tasks.register(LICENSE_TASK, LicenseReportTask::class.java) { task ->
            task.group = "verification"
            task.description = "📜 Lists the licences of the dependencies in the SBOM"
            task.sbom.set(sbom.flatMap { it.xmlOutput })
            task.forbiddenLicenses.set(extension.forbiddenLicenses)
            task.report.set(project.layout.buildDirectory.file("reports/codearmor/licenses.txt"))
        }
        LogUtil.verbose("📜 SBOM and licence report configured: ./gradlew $LICENSE_TASK")
    }

    private fun configureAnalysis(project: Project) {
        project.plugins.withType(JavaPlugin::class.java) {
            val root = project.rootProject
            if (root != project && !root.pluginManager.hasPlugin(ANALYSIS_PLUGIN_ID)) {
                LogUtil.essential(
                    "⚠️ CodeArmor: dependencyAnalysis needs the dependency-analysis plugin on the root project too; " +
                        "${project.path} is left out",
                )
                return@withType
            }
            project.pluginManager.apply(ANALYSIS_PLUGIN_ID)
        }
    }

    /** Pre-releases are only offered for a dependency that is already on one. */
    private object RejectUnstableUpgrade : ComponentFilter {
        override fun reject(selection: ComponentSelectionWithCurrent): Boolean =
            isUnstable(selection.candidate.version) && !isUnstable(selection.currentVersion)
    }

    /** A version with a qualifier other than a release marker: `-M1`, `-RC2`, `-beta`, `-SNAPSHOT`. */
    fun isUnstable(version: String): Boolean {
        val releaseMarker = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
        val plainRelease = Regex("^[0-9,.v-]+(-r|-jre|-android)?$").matches(version)
        return !releaseMarker && !plainRelease
    }
}
