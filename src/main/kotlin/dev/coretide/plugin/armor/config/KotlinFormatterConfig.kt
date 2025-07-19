/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.config

import dev.coretide.plugin.armor.enumeration.KtfmtStyle

open class KotlinFormatterConfig {
    var ktlintVersion: String = "1.6.0"
    var ktlintEditorConfigOverrides: MutableMap<String, String> = mutableMapOf()
    var ktfmtVersion: String = "0.46"
    var ktfmtStyle: KtfmtStyle = KtfmtStyle.KOTLINLANG
    var customFormatterCommand: String? = null
    var customFormatterArgs: List<String> = emptyList()
    var endWithNewline: Boolean = true
    var trimTrailingWhitespace: Boolean = true
    var targetIncludes: List<String> = listOf("src/main/kotlin/**/*.kt", "src/test/kotlin/**/*.kt")
    var targetExcludes: List<String> = listOf("**/generated/**", "**/build/**")
}
