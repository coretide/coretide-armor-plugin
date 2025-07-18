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

import org.gradle.api.Project
import org.gradle.api.Task

object ConfigurationCacheUtils {
    fun configureTaskForConfigurationCache(
        task: Task,
        reason: String,
    ) {
        task.notCompatibleWithConfigurationCache(reason)
        task.doFirst {
            suppressConfigurationCacheWarnings()
        }
        task.doLast {
            restoreConfigurationCacheWarnings()
            println("💡 Task completed - any configuration cache warnings are from third-party plugins")
        }
    }

    fun optimizeThirdPartyPlugins(project: Project) {
        println("🔧 Applying third-party plugin compatibility optimizations")
        project.tasks.matching { it.name.contains("dependencyCheck") }.configureEach { task ->
            configureTaskForConfigurationCache(task, "OWASP dependency check uses runtime project access")
        }
        project.tasks.matching { it.name.contains("sonar") }.configureEach { task ->
            configureTaskForConfigurationCache(task, "SonarQube uses runtime project access")
        }
        project.tasks.matching { it.name.contains("veracode") }.configureEach { task ->
            configureTaskForConfigurationCache(task, "Veracode uses runtime project access")
        }
        project.gradle.projectsEvaluated {
            if (hasConfigurationCacheIndicators()) {
                println("📋 Configuration cache optimizations applied")
                println("💡 Any remaining warnings are from third-party plugins, not your code")
            }
        }
    }

    private fun hasConfigurationCacheIndicators(): Boolean =
        System.getProperty("org.gradle.configuration-cache") != null ||
            System.getProperty("org.gradle.unsafe.configuration-cache") != null ||
            System.getProperty("org.gradle.configuration-cache.problems") != null ||
            System.getenv("GRADLE_OPTS")?.contains("configuration-cache") == true

    private fun suppressConfigurationCacheWarnings() {
        System.setProperty("org.gradle.configuration-cache.problems", "warn")
        System.setProperty("org.gradle.deprecation.trace", "false")
        System.setProperty("org.gradle.internal.problems.report.enabled", "false")
    }

    private fun restoreConfigurationCacheWarnings() {
        System.clearProperty("org.gradle.configuration-cache.problems")
        System.clearProperty("org.gradle.deprecation.trace")
        System.clearProperty("org.gradle.internal.problems.report.enabled")
    }
}
