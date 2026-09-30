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

import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.task.DiffCoverageTask
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

/** `armorDiffCoverage`: coverage of the lines changed since the base branch, from the coverage report the build writes. */
object DiffCoverageConfigurator {
    /** Where `armorDiffCoverage` leaves its result, under the build directory, for `armorReport`. */
    const val RESULT = "reports/codearmor/diff-coverage.json"

    fun enabled(extension: CodeArmorExtension): Boolean = extension.diffCoverage.enabled.get() && (extension.coverage.enabled.get() || extension.coverage.kover.get())

    fun configure(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        if (!enabled(extension)) return
        project.plugins.withType(JavaPlugin::class.java) {
            val kover = KoverConfigurator.usesKover(project, extension)
            val reportTask = if (kover) "koverXmlReport" else "jacocoTestReport"
            val report = if (kover) "reports/kover/report.xml" else "reports/jacoco/test/jacocoTestReport.xml"
            val main = project.extensions.getByType(SourceSetContainer::class.java).named(SourceSet.MAIN_SOURCE_SET_NAME)
            project.tasks.register(DiffCoverageTask.TASK_NAME, DiffCoverageTask::class.java) { task ->
                task.dependsOn(project.tasks.named { it == reportTask })
                task.coverageReport.set(project.layout.buildDirectory.file(report))
                task.sourceDirectories.from(main.map { it.allSource.srcDirs })
                task.projectDirectory.set(project.layout.projectDirectory)
                task.base.set(project.provider { extension.diffCoverage.base.orNull?.takeIf { it.isNotBlank() } })
                task.minimum.set(extension.diffCoverage.minimum)
                task.result.set(project.layout.buildDirectory.file(RESULT))
            }
        }
    }
}
