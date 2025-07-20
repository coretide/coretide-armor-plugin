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

import dev.coretide.plugin.armor.enumeration.LineEndingType

open class JavaFormatterConfig {
    var googleJavaFormatVersion: String = "1.15"
    var googleJavaFormatAOSP: Boolean = true
    var eclipseVersion: String = "4.26"
    var eclipseConfigFile: String? = null
    var palantirJavaFormatVersion: String = "2.28"
    var customFormatterCommand: String? = null
    var customFormatterArgs: List<String> = emptyList()
    var indentSize: Int = 4
    var removeUnusedImports: Boolean = true
    var endWithNewline: Boolean = true
    var trimTrailingWhitespace: Boolean = true
    var leadingTabsToSpaces: Boolean = true
    var lineEndings: LineEndingType = LineEndingType.UNIX
    var targetIncludes: List<String> = listOf("src/main/java/**/*.java", "src/test/java/**/*.java")
    var targetExcludes: List<String> = listOf("**/generated/**", "**/build/**")
    var importOrderFile: String? = null
}
