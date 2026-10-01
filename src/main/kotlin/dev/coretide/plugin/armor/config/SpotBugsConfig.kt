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
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/** SpotBugs, on the main sources; `spotbugsMain` is in the build tier. */
@SupportsKotlinAssignmentOverloading
abstract class SpotBugsConfig
    @Inject
    constructor(
        deprecations: Deprecations,
    ) : ToolConfig("spotbugs", deprecations) {
        abstract val toolVersion: Property<String>

        /** `MIN`, `LESS`, `DEFAULT`, `MORE` or `MAX`. */
        abstract val effort: Property<String>

        /** The lowest confidence reported: `LOW`, `MEDIUM`, `DEFAULT` or `HIGH`. */
        abstract val reportLevel: Property<String>

        abstract val ignoreFailures: Property<Boolean>

        abstract val showStackTraces: Property<Boolean>

        abstract val showProgress: Property<Boolean>

        /** An exclude filter of the build's own, relative to the project. Unset, CodeArmor's default filter. */
        abstract val excludeFile: Property<String>

        /**
         * The findings already in the code, which `spotbugsMain` leaves out: written by `armorSpotbugsBaseline`, and
         * meant to be committed. Used when the file exists.
         */
        abstract val baselineFile: Property<String>

        /** An include filter, relative to the project: only what it matches is reported. */
        abstract val includeFile: Property<String>

        abstract val xmlReports: Property<Boolean>

        abstract val htmlReports: Property<Boolean>

        abstract val textReports: Property<Boolean>

        /** For GitHub code scanning and other SARIF viewers; see `armorSarifReport`. */
        abstract val sarifReports: Property<Boolean>

        /** The SpotBugs JVM's maximum heap, such as `1g`. */
        abstract val maxHeap: Property<String>

        /** Milliseconds a SpotBugs task may run before it fails. */
        abstract val timeout: Property<Int>

        /** Only these detectors run, when set. */
        abstract val bugCategories: ListProperty<String>

        abstract val extraArgs: ListProperty<String>

        companion object {
            const val DEFAULT_TOOL_VERSION = "4.10.3"
        }
    }
