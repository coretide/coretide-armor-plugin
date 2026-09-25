/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.codestats

import dev.coretide.plugin.armor.config.CodeStatsConfig
import dev.coretide.plugin.armor.enumeration.CodeStatsScope
import org.gradle.api.Project
import java.io.File
import java.io.StringReader
import java.util.Properties

/**
 * The code stats settings in effect, after a developer's own settings are applied.
 *
 * Precedence, highest first: `-P` on the command line, then `~/.gradle/gradle.properties` (the
 * developer's own), then the project's gradle.properties and build script (shared by the team).
 * [CodeStatsScope.GLOBAL] is only accepted from the developer's own settings.
 */
class CodeStatsSettings(
    val enabled: Boolean,
    val scope: CodeStatsScope,
    val warnings: List<String>,
) {
    companion object {
        const val ENABLED_PROPERTY = "codearmor.codestats.enabled"
        const val SCOPE_PROPERTY = "codearmor.codestats.scope"

        fun resolve(
            project: Project,
            config: CodeStatsConfig,
        ): CodeStatsSettings =
            resolve(
                buildEnabled = config.enabled.get(),
                buildScope = config.scope.get(),
                developer = developerProperties(project),
                projectEnabled = project.providers.gradleProperty(ENABLED_PROPERTY).orNull,
                projectScope = project.providers.gradleProperty(SCOPE_PROPERTY).orNull,
            )

        /**
         * [developer] holds `-P` and `~/.gradle/gradle.properties` values. [projectEnabled] and
         * [projectScope] are the values Gradle sees from every source, which includes the project's
         * gradle.properties.
         */
        fun resolve(
            buildEnabled: Boolean,
            buildScope: CodeStatsScope,
            developer: Map<String, String>,
            projectEnabled: String?,
            projectScope: String?,
        ): CodeStatsSettings {
            val warnings = mutableListOf<String>()

            val enabledValue = developer[ENABLED_PROPERTY] ?: projectEnabled
            val enabled =
                enabledValue?.let { value ->
                    value.trim().toBooleanStrictOrNull()
                        ?: null.also { warnings += "$ENABLED_PROPERTY=$value is not true or false; ignored" }
                } ?: buildEnabled

            val developerScope =
                developer[SCOPE_PROPERTY]?.let { value ->
                    CodeStatsScope.entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                        ?: null.also { warnings += "$SCOPE_PROPERTY=$value is not REPO or GLOBAL; ignored" }
                }
            if (developer[SCOPE_PROPERTY] == null && projectScope != null) {
                warnings +=
                    "$SCOPE_PROPERTY in the project's gradle.properties is ignored: only your own " +
                    "~/.gradle/gradle.properties or -P can choose the scope"
            }
            val scope =
                when {
                    developerScope != null -> developerScope
                    buildScope == CodeStatsScope.GLOBAL -> {
                        warnings +=
                            "codeStats.scope = GLOBAL in a build script is ignored, so code stats stays in this " +
                            "repository: a shared build file must not change every developer's global git " +
                            "configuration. Set $SCOPE_PROPERTY=GLOBAL in ~/.gradle/gradle.properties instead."
                        CodeStatsScope.REPO
                    }
                    else -> buildScope
                }
            return CodeStatsSettings(enabled, scope, warnings)
        }

        /** `-P` values over the developer's `~/.gradle/gradle.properties`. */
        private fun developerProperties(project: Project): Map<String, String> {
            val userHomeFile = File(project.gradle.gradleUserHomeDir, "gradle.properties")
            val userHome = Properties()
            project.providers
                .fileContents(project.objects.fileProperty().fileValue(userHomeFile))
                .asText
                .orNull
                ?.let { userHome.load(StringReader(it)) }
            val commandLine = project.gradle.startParameter.projectProperties
            return listOf(ENABLED_PROPERTY, SCOPE_PROPERTY)
                .mapNotNull { key -> (commandLine[key] ?: userHome.getProperty(key))?.let { key to it } }
                .toMap()
        }
    }
}
