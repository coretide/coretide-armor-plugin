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
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * OWASP Dependency-Check, in the CI tier: known vulnerabilities in the dependencies that ship, those of
 * `runtimeClasspath`.
 */
@SupportsKotlinAssignmentOverloading
abstract class OwaspConfig
    @Inject
    constructor(
        deprecations: Deprecations,
        objects: ObjectFactory,
    ) : ToolConfig("owasp", deprecations) {
        /** The CVSS score, from 0 to 10, at or above which a finding fails the build. */
        val failBuildOnCvss: Property<Double> = objects.property(Double::class.java)

        /** `failBuildOnCvss = 0.8` in a Groovy build script, where a decimal is a BigDecimal, which a Double property refuses. */
        fun setFailBuildOnCvss(value: Number) = failBuildOnCvss.set(value.toDouble())

        /** A suppression file of the build's own, relative to the project. CodeArmor suppresses nothing itself. */
        abstract val suppressionFile: Property<String>

        /**
         * Downloads the NVD vulnerability data when there is none, and refreshes it after [nvdValidForHours]. Without
         * it, a scan fails on a machine that has no data yet, such as a fresh CI runner.
         */
        abstract val autoUpdate: Property<Boolean>

        /** The NVD API key. Unset, the `nvd.api.key` Gradle property, then the `NVD_API_KEY` environment variable. */
        abstract val nvdApiKey: Property<String>

        /** Milliseconds between requests to the NVD API. */
        abstract val nvdApiDelay: Property<Int>

        /** How often a failed request to the NVD API is retried. */
        abstract val nvdMaxRetryCount: Property<Int>

        /** How many hours the downloaded NVD data counts as current. */
        abstract val nvdValidForHours: Property<Int>
    }
