/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.config

import org.gradle.api.SupportsKotlinAssignmentOverloading
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * SonarQube analysis. `sonar` is in the CI tier once a server or a token is configured: here, through
 * `SONAR_HOST_URL` or `SONAR_TOKEN`, or through the `sonar.host.url` or `sonar.token` system properties. Listing it
 * in `checks.ci` runs it regardless.
 */
@SupportsKotlinAssignmentOverloading
abstract class SonarqubeConfig
    @Inject
    constructor(
        deprecations: Deprecations,
    ) : ToolConfig("sonarqube", deprecations) {
        /** The server; `SONAR_HOST_URL` takes precedence. Unset, the SonarScanner's default, SonarQube Cloud. */
        abstract val hostUrl: Property<String>

        /** Unset, `group:name`. */
        abstract val projectKey: Property<String>

        /** Unset, the project's name. */
        abstract val projectName: Property<String>

        /** Unset, the SonarScanner reads `SONAR_TOKEN`, the usual way on CI. */
        abstract val token: Property<String>

        /** Whether `sonar` waits for the quality gate, and fails when the gate does. */
        abstract val qualityGateWait: Property<Boolean>

        /** The Java version the sources are analysed as. Unset, the project's toolchain or compatibility settings. */
        abstract val javaVersion: Property<String>
    }
