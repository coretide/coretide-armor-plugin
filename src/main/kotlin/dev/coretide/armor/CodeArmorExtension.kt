/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.armor

import dev.coretide.armor.config.SpotlessFormats

enum class KotlinFormatter {
    KTLINT,
    KTFMT,
}

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
    var checkstyleConfigFile: String? = null
    var checkstyleSuppressionFile: String? = null
    var checkstyleMaxWarnings: Int = 0
    var spotlessApplyLicenseHeader: Boolean = false
    var spotbugsExcludeFile: String? = null
    var spotbugsEffort: String = "MAX"
    var spotbugsReportLevel: String = "HIGH"
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
    var kotlinFormatter: KotlinFormatter = KotlinFormatter.KTLINT
    var ktlintVersion: String = "1.6.0"
    var ktfmtVersion: String = "0.46"
    var ktfmtStyle: KtfmtStyle = KtfmtStyle.KOTLINLANG // For ktfmt
    var ktlintEditorConfigOverrides: MutableMap<String, String> = mutableMapOf()

    fun spotlessFormats(configure: SpotlessFormats.() -> Unit) {
        spotlessFormats.configure()
    }

    fun ktlintRules(configure: MutableMap<String, String>.() -> Unit) {
        ktlintEditorConfigOverrides.configure()
    }
}

enum class KtfmtStyle {
    KOTLINLANG,
    GOOGLE,
    META,
}
