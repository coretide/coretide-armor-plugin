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

open class CheckstyleConfig {
    var toolVersion: String = "10.20.1"
    var maxWarnings: Int = 0
    var ignoreFailures: Boolean = false
    var showViolations: Boolean = true
    var configFile: String? = null
    var suppressionFile: String? = null
    var xmlReports: Boolean = true
    var htmlReports: Boolean = true
    var sarifReports: Boolean = false
    var targetIncludes: List<String> = listOf("src/main/java/**/*.java", "src/test/java/**/*.java")
    var targetExcludes: List<String> = listOf("**/generated/**", "**/build/**", "**/target/**")
    var configProperties: MutableMap<String, Any> = mutableMapOf()
    var maxErrors: Int = 0
    var enableRulesSummary: Boolean = true

    @Suppress("unused")
    fun addConfigProperty(
        key: String,
        value: Any,
    ) {
        configProperties[key] = value
    }

    @Suppress("unused")
    fun addConfigProperties(properties: Map<String, Any>) {
        configProperties.putAll(properties)
    }
}
