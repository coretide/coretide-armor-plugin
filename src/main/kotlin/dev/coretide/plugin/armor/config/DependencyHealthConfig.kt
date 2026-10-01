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

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/** The dependency checks in the CI tier, besides OWASP Dependency-Check. */
abstract class DependencyHealthConfig {
    /**
     * `dependencyUpdates` lists dependencies that have a newer release. Pre-releases are only offered for a dependency
     * already on one.
     */
    abstract val updates: Property<Boolean>

    /** A CycloneDX SBOM of the runtime dependencies (`cyclonedxBom`), and a licence report built from it (`armorLicenseReport`). */
    abstract val sbom: Property<Boolean>

    /**
     * Licences, as SPDX ids (`GPL-3.0-only`) or names, that fail `armorLicenseReport` when a dependency can only be
     * used under one of them.
     */
    abstract val forbiddenLicenses: ListProperty<String>

    /**
     * The dependency-analysis plugin's `projectHealth`: dependencies declared but not used, used but only there
     * transitively, or on the wrong configuration. It reports; it does not fail the build. Opt-in.
     */
    abstract val analysis: Property<Boolean>
}
