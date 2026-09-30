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

import org.gradle.api.provider.Property

/** Checks that a library's public API stays compatible. Libraries only. */
abstract class LibraryApiConfig {
    /**
     * A released version of this library, `1.4.0` or `group:name:1.4.0`, that `armorApiCheck`, in the CI tier,
     * compares the jar with. It fails on binary incompatible changes.
     */
    abstract val baseline: Property<String>

    /**
     * The Kotlin Gradle plugin's ABI validation (Kotlin 2.2+), in the build tier: the public API of a Kotlin library
     * must match the dump committed under `api/`. Opt-in.
     */
    abstract val kotlinAbiValidation: Property<Boolean>
}
