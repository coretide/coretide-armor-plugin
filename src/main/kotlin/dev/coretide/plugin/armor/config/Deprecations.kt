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

/**
 * The deprecated settings a build script used, each with what to write instead. CodeArmor logs them once the build
 * script has run, and `armorInfo` lists them. 1.0.0 removes the settings.
 */
class Deprecations {
    private val used = linkedMapOf<String, String>()

    /** Records that the build script set [setting]; [replacement] is what to write instead, or why nothing is needed. */
    fun use(
        setting: String,
        replacement: String,
    ) {
        used[setting] = replacement
    }

    /** The settings used, in the order the build script first set them, with their replacements. */
    val settings: Map<String, String> get() = used

    /** One line per setting, for the build log and `armorInfo`. */
    fun messages(): List<String> = used.map { (setting, replacement) -> "$setting is deprecated, and 1.0.0 removes it. $replacement" }

    companion object {
        /** [value] as a build script would write it, so a replacement can be copied as it is. */
        fun dsl(value: Any?): String =
            when (value) {
                null -> "null"
                is String -> "\"$value\""
                is Enum<*> -> "${value.javaClass.simpleName}.${value.name}"
                is Collection<*> -> value.joinToString(prefix = "listOf(", postfix = ")") { dsl(it) }
                else -> value.toString()
            }
    }
}
