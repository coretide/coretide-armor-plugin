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

import dev.coretide.armor.CodeArmorExtension
import dev.coretide.armor.utils.FileUtils
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

        project.configure<CheckstyleExtension> {
            toolVersion = "10.20.1"
            maxWarnings = extension.checkstyleMaxWarnings
            isIgnoreFailures = false
            isShowViolations = true

            val configFile =
                extension.checkstyleConfigFile?.let { project.file(it) }
                    ?: FileUtils.createDefaultCheckstyleConfig(project)
            this.configFile = configFile
            val suppressionFile =
                extension.checkstyleSuppressionFile?.let { project.file(it) }
                    ?: FileUtils.createDefaultCheckstyleSuppression(project)
            configProperties =
                mapOf(
                    "checkstyle.suppressions.file" to suppressionFile.absolutePath,
                )
        }

        project.tasks.withType(Checkstyle::class.java).configureEach { task ->
            task.reports { reports ->
                reports.xml.required.set(true)
                reports.html.required.set(true)
            }
            task.exclude("**/generated/**")
            task.exclude("**/build/**")
            task.exclude("**/target/**")

            task.doLast {
                println("✅ Checkstyle analysis completed")
            }
        }
    }
}
