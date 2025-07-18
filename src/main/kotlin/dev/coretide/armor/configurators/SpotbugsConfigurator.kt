/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.armor.configurators

import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsExtension
import com.github.spotbugs.snom.SpotBugsTask
import dev.coretide.armor.CodeArmorExtension
import dev.coretide.armor.utils.FileUtils
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

object SpotbugsConfigurator {
    fun configureSpotbugs(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        project.pluginManager.apply("com.github.spotbugs")
        project.configure<SpotBugsExtension> {
            toolVersion.set("4.8.6")
            ignoreFailures.set(false)
            showStackTraces.set(true)
            showProgress.set(true)
            effort.set(
                Effort
                    .valueOf(extension.spotbugsEffort),
            )
            reportLevel.set(
                Confidence
                    .valueOf(extension.spotbugsReportLevel),
            )
            extension.spotbugsExcludeFile?.let { excludeFile ->
                val excludeFileObj = project.file(excludeFile)
                if (excludeFileObj.exists()) {
                    excludeFilter.set(excludeFileObj)
                } else {
                    val defaultExcludeFile = FileUtils.createDefaultSpotbugsExclude(project, excludeFileObj)
                    excludeFilter.set(defaultExcludeFile)
                }
            } ?: run {
                val defaultExcludeFile = FileUtils.createDefaultSpotbugsExclude(project)
                excludeFilter.set(defaultExcludeFile)
            }
        }

        project.tasks.withType(SpotBugsTask::class.java).configureEach { task ->
            task.reports {
                it.create("xml") { report ->
                    report.required.set(true)
                    report.outputLocation.set(
                        project.file("build/reports/spotbugs/${task.name}.xml"),
                    )
                }
                it.create("html") { report ->
                    report.required.set(true)
                    report.outputLocation.set(
                        project.file("build/reports/spotbugs/${task.name}.html"),
                    )
                }
                it.create("sarif") { report ->
                    report.required.set(false)
                    report.outputLocation.set(
                        project.file("build/reports/spotbugs/${task.name}.sarif"),
                    )
                }
            }

            task.doLast {
                println("✅ SpotBugs analysis completed for ${task.name}")
                println("📊 Reports available at: build/reports/spotbugs/")
            }
        }
    }
}
