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

import org.gradle.api.Project
import org.gradle.api.Task

object ConfigurationCacheUtil {
    /**
     * Marks a third-party task as configuration-cache incompatible.
     *
     * Note this disables the configuration cache for any invocation whose task graph includes it —
     * that is Gradle's behaviour for this API, not something armor can narrow.
     */
    fun configureTaskForConfigurationCache(
        task: Task,
        reason: String,
    ) {
        task.notCompatibleWithConfigurationCache(reason)
    }

    fun optimizeThirdPartyPlugins(project: Project) {
        LogUtil.verbose("🔧 Applying third-party plugin compatibility optimizations")
        project.tasks.matching { it.name.contains("dependencyCheck") }.configureEach { task ->
            configureTaskForConfigurationCache(task, "OWASP dependency check uses runtime project access")
        }
        project.tasks.matching { it.name.contains("sonar") }.configureEach { task ->
            configureTaskForConfigurationCache(task, "SonarQube uses runtime project access")
        }
        project.tasks.matching { it.name.contains("veracode") }.configureEach { task ->
            configureTaskForConfigurationCache(task, "Veracode uses runtime project access")
        }
    }
}
