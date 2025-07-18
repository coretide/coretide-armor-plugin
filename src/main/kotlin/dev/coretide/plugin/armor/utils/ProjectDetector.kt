/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.utils

import dev.coretide.plugin.armor.ProjectType
import org.gradle.api.Project

object ProjectDetector {
    fun detectProjectType(project: Project): ProjectType {
        val hasJavaPlugin =
            project.plugins.hasPlugin("java") ||
                project.plugins.hasPlugin("java-library") ||
                project.plugins.hasPlugin("application")
        val hasKotlinPlugin = project.plugins.hasPlugin("org.jetbrains.kotlin.jvm")
        val hasSpringBootPlugin = project.plugins.hasPlugin("org.springframework.boot")
        val hasJavaFiles =
            project
                .fileTree("src")
                .matching {
                    it.include("**/*.java")
                }.files
                .isNotEmpty()
        val hasKotlinFiles =
            project
                .fileTree("src")
                .matching {
                    it.include("**/*.kt")
                }.files
                .isNotEmpty()
        val isApplication =
            hasSpringBootPlugin ||
                project.plugins.hasPlugin("application") ||
                project.name.endsWith("-app") ||
                project.name.endsWith("-service")
        return when {
            hasKotlinFiles && hasJavaFiles -> if (isApplication) ProjectType.MIXED_APPLICATION else ProjectType.MIXED_LIBRARY
            hasKotlinFiles || hasKotlinPlugin -> if (isApplication) ProjectType.KOTLIN_APPLICATION else ProjectType.KOTLIN_LIBRARY
            hasJavaFiles || hasJavaPlugin -> if (isApplication) ProjectType.JAVA_APPLICATION else ProjectType.JAVA_LIBRARY
            else -> ProjectType.JAVA_LIBRARY
        }
    }

    fun detectMultiModule(project: Project): Boolean = project.subprojects.isNotEmpty()

    fun needsCheckstyle(projectType: ProjectType): Boolean =
        projectType in
            listOf(ProjectType.JAVA_APPLICATION, ProjectType.JAVA_LIBRARY, ProjectType.MIXED_APPLICATION, ProjectType.MIXED_LIBRARY)
}
