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

open class SpotBugsConfig {
    var toolVersion: String = "4.8.6"
    var effort: String = "MAX"
    var reportLevel: String = "HIGH"
    var ignoreFailures: Boolean = false
    var showStackTraces: Boolean = true
    var showProgress: Boolean = true
    var excludeFile: String? = null
    var includeFile: String? = null
    var xmlReports: Boolean = true
    var htmlReports: Boolean = true
    var textReports: Boolean = false
    var sarifReports: Boolean = false
    var maxHeap: String? = null
    var timeout: Int? = null
    var bugCategories: List<String> = emptyList()
    var extraArgs: List<String> = emptyList()
}
