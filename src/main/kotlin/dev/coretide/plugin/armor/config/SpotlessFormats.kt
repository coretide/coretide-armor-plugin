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

open class SpotlessFormats {
    var json: Boolean = false
    var xml: Boolean = false
    var yaml: Boolean = false
    var properties: Boolean = false
    var markdown: Boolean = false
    var html: Boolean = false
    var css: Boolean = false
    var sql: Boolean = false
    var jsonIndentSpaces: Int = 2
    var yamlTabWidth: Int = 2
    var yamlPrintWidth: Int = 120
    var markdownTabWidth: Int = 2
    var markdownPrintWidth: Int = 120
    var markdownProseWrap: String = "preserve"
    var htmlTabWidth: Int = 2
    var htmlPrintWidth: Int = 120
    var htmlBracketSameLine: Boolean = false
    var cssTabWidth: Int = 2
    var cssPrintWidth: Int = 120
    var cssSingleQuote: Boolean = false
    var sqlUppercaseKeywords: Boolean = true
}
