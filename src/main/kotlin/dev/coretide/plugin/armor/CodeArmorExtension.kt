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

import dev.coretide.plugin.armor.config.ChecksConfig
import dev.coretide.plugin.armor.config.CodeStatsConfig
import dev.coretide.plugin.armor.config.SpotBugsConfig
import dev.coretide.plugin.armor.enumeration.ArmorLogLevel
import dev.coretide.plugin.armor.enumeration.CodeStatsScope
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class CodeArmorExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        var autoDetect: Boolean = true
        var jacoco: Boolean = true
        var spotbugs: Boolean = true

        /**
         * detekt for projects that apply the Kotlin JVM plugin; its `detekt` task joins the build tier.
         * Configure it in `config/detekt/detekt.yml`; findings already in the code can go in a baseline
         * (`./gradlew detektBaseline`).
         */
        var detekt: Boolean = true
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

        /**
         * Runs test tasks that are still on Gradle's default JUnit 4 runner on the JUnit Platform. A test
         * task that chose TestNG, or configured the JUnit Platform itself, keeps its choice. Set to false to
         * keep JUnit 4.
         */
        var junitPlatform: Boolean = true

        /**
         * Compiler warnings in production code fail the build: `-Xlint` with `-Werror` for Java, and
         * `allWarningsAsErrors` with `-Xjsr305=strict` for Kotlin, plus explicit API mode for Kotlin
         * libraries. Test code is not affected.
         */
        var strictCompilation: Boolean = false
        var owaspFailBuildOnCVSS: Double = 9.0
        var owaspSuppressionFile: String? = null
        var owaspAutoUpdate: Boolean = false
        var owaspNvdApiKey: String? = null
        var owaspNvdApiDelay: Int = 4000
        var owaspNvdMaxRetryCount: Int = 10
        var owaspNvdValidForHours: Int = 24
        var sonarHostUrl: String = "http://localhost:9000"
        var sonarProjectKey: String? = ""
        var sonarProjectName: String? = ""
        var sonarToken: String? = ""
        var sonarQualityGateWait: Boolean = false
        /**
         * The Java version SonarQube analyses the sources as. Left unset, the SonarScanner reads it from
         * the project's Java toolchain or compatibility settings.
         */
        var sonarJavaVersion: String? = null
        var enableGitHooks: Boolean = true
        var prePushEnabled: Boolean = true
        var enableVersionFromGit: Boolean = true
        var enableResourceProcessing: Boolean = true
        var spotbugsConfig: SpotBugsConfig = SpotBugsConfig()
        var logLevel: ArmorLogLevel = ArmorLogLevel.ESSENTIAL

        /** Which tasks the pre-push, build and CI check tiers run. See [ChecksConfig]. */
        val checks: ChecksConfig =
            objects.newInstance(ChecksConfig::class.java).apply {
                prePush.convention(listOf("quickBuild"))
            }

        /** Reporting commit activity to Code::Stats. Off by default; see [CodeStatsConfig]. */
        val codeStats: CodeStatsConfig =
            objects.newInstance(CodeStatsConfig::class.java).apply {
                enabled.convention(false)
                scope.convention(CodeStatsScope.REPO)
            }

        @Suppress("unused")
        fun spotbugs(configure: SpotBugsConfig.() -> Unit) {
            spotbugsConfig.configure()
        }

        @Suppress("unused")
        fun checks(action: Action<ChecksConfig>) {
            action.execute(checks)
        }

        @Suppress("unused")
        fun codeStats(action: Action<CodeStatsConfig>) {
            action.execute(codeStats)
        }
    }
