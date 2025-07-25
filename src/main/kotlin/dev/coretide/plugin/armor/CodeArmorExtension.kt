/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor

import dev.coretide.plugin.armor.config.CheckstyleConfig
import dev.coretide.plugin.armor.config.JavaFormatterConfig
import dev.coretide.plugin.armor.config.KotlinFormatterConfig
import dev.coretide.plugin.armor.config.SpotBugsConfig
import dev.coretide.plugin.armor.config.SpotlessFormats
import dev.coretide.plugin.armor.enumeration.ArmorLogLevel
import dev.coretide.plugin.armor.enumeration.JavaFormatter
import dev.coretide.plugin.armor.enumeration.KotlinFormatter

@Suppress("unused")
open class CodeArmorExtension {
    var autoDetect: Boolean = true
    var jacoco: Boolean = true
    var checkstyle: Boolean = true
    var spotbugs: Boolean = true
    var spotless: Boolean = true
    var owasp: Boolean = true
    var veracode: Boolean = false
    var sonarqube: Boolean = true
    var projectType: ProjectType? = null
    var isMultiModule: Boolean = false
    var coverageMinimum: Double = 0.30
    var coverageClassMinimum: Double = 0.25
    var coverageInclusions: MutableList<String> = mutableListOf()
    var coverageExclusions: MutableList<String> = mutableListOf()
    var coverageIncludeDefaultExclusions: Boolean = true
    var owaspFailBuildOnCVSS: Double = 9.0
    var owaspSuppressionFile: String? = null
    var owaspAutoUpdate: Boolean = false
    var owaspNvdApiKey: String? = null
    var owaspNvdApiDelay: Int = 4000
    var owaspNvdMaxRetryCount: Int = 10
    var owaspNvdValidForHours: Int = 24
    var spotlessApplyLicenseHeader: Boolean = false
    var sonarHostUrl: String = "http://localhost:9000"
    var sonarProjectKey: String? = ""
    var sonarProjectName: String? = ""
    var sonarToken: String? = ""
    var sonarQualityGateWait: Boolean = false
    var sonarJavaVersion: String = "11"
    var veracodeUsername: String = ""
    var veracodePassword: String = ""
    var enableGitHooks: Boolean = true
    var preCommitEnabled: Boolean = true
    var prePushEnabled: Boolean = true
    var enableVersionFromGit: Boolean = true
    var enableResourceProcessing: Boolean = true
    var spotlessFormats: SpotlessFormats = SpotlessFormats()
    var javaFormatter: JavaFormatter = JavaFormatter.ECLIPSE
    var javaFormatterConfig: JavaFormatterConfig = JavaFormatterConfig()
    var kotlinFormatter: KotlinFormatter = KotlinFormatter.KTLINT
    var kotlinFormatterConfig: KotlinFormatterConfig = KotlinFormatterConfig()
    var checkstyleConfig: CheckstyleConfig = CheckstyleConfig()
    var spotbugsConfig: SpotBugsConfig = SpotBugsConfig()
    var logLevel: ArmorLogLevel = ArmorLogLevel.ESSENTIAL

    fun spotlessFormats(configure: SpotlessFormats.() -> Unit) {
        spotlessFormats.configure()
    }

    fun javaFormatter(configure: JavaFormatterConfig.() -> Unit) {
        javaFormatterConfig.configure()
    }

    fun kotlinFormatter(configure: KotlinFormatterConfig.() -> Unit) {
        kotlinFormatterConfig.configure()
    }

    fun checkstyle(configure: CheckstyleConfig.() -> Unit) {
        checkstyleConfig.configure()
    }

    fun spotbugs(configure: SpotBugsConfig.() -> Unit) {
        spotbugsConfig.configure()
    }
}
