/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.util

import dev.coretide.plugin.armor.ProjectType
import org.gradle.api.Project

object ProjectDetector {
    /** Plugins that make a project a runnable application rather than a library. */
    private val APPLICATION_PLUGINS =
        listOf(
            "application",
            "org.springframework.boot",
            "io.quarkus",
            "io.micronaut.application",
            "io.ktor.plugin",
        )

    fun detectProjectType(project: Project): ProjectType {
        val hasJavaPlugin =
            project.plugins.hasPlugin("java") ||
                project.plugins.hasPlugin("java-library") ||
                project.plugins.hasPlugin("application")
        val hasKotlinPlugin = project.plugins.hasPlugin("org.jetbrains.kotlin.jvm")
        // Production sources only: Kotlin tests in a Java project do not make it a Kotlin project.
        val hasJavaFiles = hasSourceFiles(project, "**/*.java")
        val hasKotlinFiles = hasSourceFiles(project, "**/*.kt")
        val isApplication = APPLICATION_PLUGINS.any { project.plugins.hasPlugin(it) }
        return when {
            hasKotlinFiles && hasJavaFiles -> if (isApplication) ProjectType.MIXED_APPLICATION else ProjectType.MIXED_LIBRARY
            hasKotlinFiles || hasKotlinPlugin -> if (isApplication) ProjectType.KOTLIN_APPLICATION else ProjectType.KOTLIN_LIBRARY
            hasJavaFiles || hasJavaPlugin -> if (isApplication) ProjectType.JAVA_APPLICATION else ProjectType.JAVA_LIBRARY
            else -> ProjectType.JAVA_LIBRARY
        }
    }

    fun detectMultiModule(project: Project): Boolean = project.subprojects.isNotEmpty()

    private fun hasSourceFiles(
        project: Project,
        pattern: String,
    ): Boolean =
        project
            .fileTree("src/main")
            .matching { it.include(pattern) }
            .files
            .isNotEmpty()
}
