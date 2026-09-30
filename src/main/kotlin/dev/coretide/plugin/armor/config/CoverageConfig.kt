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

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Test coverage, measured by JaCoCo, or by Kover in Kotlin projects with [kover]. `jacocoTestCoverageVerification`
 * (`koverVerify` with Kover), in the build tier, fails the build below [minimum] overall or [classMinimum] in a class.
 */
abstract class CoverageConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        /** Whether CodeArmor measures coverage. */
        abstract val enabled: Property<Boolean>

        /**
         * Kover instead of JaCoCo in Kotlin projects, with the same thresholds and exclusions: it understands inline
         * functions and coroutines better. Java projects keep JaCoCo. Opt-in.
         */
        abstract val kover: Property<Boolean>

        /** The share of instructions (of lines, with Kover) tests must cover, from 0 to 1. */
        val minimum: Property<Double> = objects.property(Double::class.java)

        /** `minimum = 0.8` in a Groovy build script, where a decimal is a BigDecimal, which a Double property refuses. */
        fun setMinimum(value: Number) = minimum.set(value.toDouble())

        /** The share of lines tests must cover in each class, from 0 to 1. */
        val classMinimum: Property<Double> = objects.property(Double::class.java)

        /** `classMinimum = 0.8` in a Groovy build script, where a decimal is a BigDecimal, which a Double property refuses. */
        fun setClassMinimum(value: Number) = classMinimum.set(value.toDouble())

        /** Class patterns to measure; empty measures every class. */
        abstract val inclusions: ListProperty<String>

        /** Class patterns to leave out, besides the defaults. */
        abstract val exclusions: ListProperty<String>

        /** Whether to leave out entry points, configuration and generated code, as `logExclusionInfo` lists them. */
        abstract val defaultExclusions: Property<Boolean>
    }
