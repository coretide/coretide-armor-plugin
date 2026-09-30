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
 * `armorDiffCoverage`, in the build tier: the coverage of the lines changed since the base branch, as a pull request
 * shows it. It reports; [minimum] also makes it fail.
 */
@SupportsKotlinAssignmentOverloading
abstract class DiffCoverageConfig
    @Inject
    constructor(
        deprecations: Deprecations,
        objects: ObjectFactory,
    ) : ToolConfig("diffCoverage", deprecations) {
        /** The share of changed lines tests must cover, from 0 to 1, or the build fails. Unset, it only reports. */
        val minimum: Property<Double> = objects.property(Double::class.java)

        /** `minimum = 0.8` in a Groovy build script, where a decimal is a BigDecimal, which a Double property refuses. */
        fun setMinimum(value: Number) = minimum.set(value.toDouble())

        /**
         * The branch to compare with. Unset, the pull request's target branch on GitHub Actions, GitLab, Jenkins,
         * Azure Pipelines or Bitbucket, then `origin/HEAD`, `origin/main`, `origin/master`, `main` or `master`.
         */
        abstract val base: Property<String>
    }
