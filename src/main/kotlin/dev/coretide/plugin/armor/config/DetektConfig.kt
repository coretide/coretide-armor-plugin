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
 * detekt, for projects that apply the Kotlin JVM plugin; its `detekt` task joins the build tier. Configure it in
 * `config/detekt/detekt.yml`; findings already in the code can go in a baseline (`./gradlew detektBaseline`).
 */
@SupportsKotlinAssignmentOverloading
abstract class DetektConfig
    @Inject
    constructor(
        deprecations: Deprecations,
    ) : ToolConfig("detekt", deprecations) {
        /**
         * `detektMain`, which analyses the main sources against their compile classpath, takes `detekt`'s place in the
         * build tier: rules that need types run too. Slower. Its baseline is `detekt-baseline-main.xml`, written by
         * `./gradlew detektBaselineMain`. Opt-in.
         */
        abstract val typeResolution: Property<Boolean>
    }
