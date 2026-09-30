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

/** PIT mutation testing, run on demand with `./gradlew pitest`. Opt-in. */
@SupportsKotlinAssignmentOverloading
abstract class MutationTestingConfig
    @Inject
    constructor(
        deprecations: Deprecations,
    ) : ToolConfig("mutationTesting", deprecations) {
        /** The mutation score, in percent, `pitest` fails below. 0 only reports. */
        abstract val threshold: Property<Int>
    }
