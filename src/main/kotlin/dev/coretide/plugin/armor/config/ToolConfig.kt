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

/**
 * A tool's settings block, named like the tool: `detekt { typeResolution = true }`. Until 0.5.0 the same name was an
 * on/off switch, `detekt = false`; that still works, through [assign] in Kotlin build scripts and a setter on the
 * extension in Groovy ones, and is deprecated in favour of [enabled].
 */
abstract class ToolConfig(
    private val name: String,
    private val deprecations: Deprecations,
) {
    /** Whether CodeArmor runs the tool. */
    abstract val enabled: Property<Boolean>

    /** `detekt = false` in a Kotlin build script: Gradle's Kotlin DSL turns the assignment into this call. */
    @Deprecated("Set enabled inside the block instead, as in detekt { enabled = false }. 1.0.0 removes this form.")
    fun assign(enabled: Boolean) {
        deprecations.use("$name = $enabled", "Use $name { enabled = $enabled }.")
        this.enabled.set(enabled)
    }
}
