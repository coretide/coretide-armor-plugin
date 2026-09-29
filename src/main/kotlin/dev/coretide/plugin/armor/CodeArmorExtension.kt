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
         * On CI (`CI=true`), a failed test is re-run up to this many times. One that then passes does not fail
         * the build, but is listed as flaky. 0 turns retries off.
         */
        var flakyTestRetries: Int = 2

        /** Tests that take at least this long are listed after each test run. 0 turns the list off. */
        var slowTestThresholdMillis: Long = 2000

        /**
         * Kover instead of JaCoCo for coverage, with the same thresholds and exclusions. It understands Kotlin's
         * inline functions and coroutines better. Opt-in.
         */
        var kover: Boolean = false

        /** PIT mutation testing, run on demand with `./gradlew pitest`. Opt-in. */
        var mutationTesting: Boolean = false

        /** The mutation score, in percent, `pitest` fails below. 0 only reports. */
        var mutationThreshold: Int = 0

        /**
         * Compiler warnings in production code fail the build: `-Xlint` with `-Werror` for Java, and
         * `allWarningsAsErrors` with `-Xjsr305=strict` for Kotlin, plus explicit API mode for Kotlin
         * libraries. Test code is not affected.
         */
        var strictCompilation: Boolean = false

        /**
         * Error Prone checks the Java sources as they compile, and its errors fail the build. It needs a JDK 21
         * compiler; with an older one, compilation goes ahead without it, with a warning. Opt-in.
         */
        var errorProne: Boolean = false

        /**
         * NullAway, an Error Prone check, fails the build where production Java code may dereference null. It
         * checks the project's own packages and believes `@Nullable` annotations (JSpecify's, for example).
         * Turns on Error Prone. Opt-in.
         */
        var nullAway: Boolean = false

        /**
         * `dependencyUpdates` lists dependencies that have a newer release. Pre-releases are only offered for a
         * dependency already on one. Part of the CI tier.
         */
        var dependencyUpdates: Boolean = true

        /**
         * A CycloneDX SBOM of the runtime dependencies (`cyclonedxBom`), and a report of their licences built from
         * it (`armorLicenseReport`). Both are part of the CI tier.
         */
        var sbom: Boolean = true

        /**
         * Licences, as SPDX ids (`GPL-3.0-only`) or names, that fail `armorLicenseReport` when a dependency can only
         * be used under one of them. Empty by default.
         */
        var forbiddenLicenses: MutableList<String> = mutableListOf()

        /**
         * The dependency-analysis plugin's `projectHealth`, in the CI tier: dependencies declared but not used, used
         * but only there transitively, or on the wrong configuration. It reports; it does not fail the build. Opt-in.
         */
        var dependencyAnalysis: Boolean = false

        /**
         * A released version of this library, `1.4.0` or `group:name:1.4.0`, that `armorApiCheck` compares the jar
         * with. It fails on binary incompatible changes. Part of the CI tier. Libraries only; opt-in.
         */
        var apiBaseline: String? = null

        /**
         * The Kotlin Gradle plugin's ABI validation (Kotlin 2.2+): the public API of a Kotlin library must match
         * the dump committed under `api/`. Part of the build tier. Libraries only; opt-in.
         */
        var kotlinAbiValidation: Boolean = false

        /**
         * ArchUnit on the test classpath, and `armorScaffoldArchitectureTests`, which writes a first architecture
         * test (package cycles, field injection, standard streams, generic exceptions). The tests run with the
         * others, so a violated rule fails the build. Opt-in.
         */
        var architectureTests: Boolean = false
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

        /**
         * A commit-msg hook, installed by `armorInstallGitHooks`, that rejects a commit whose message is not a
         * Conventional Commit: `type(scope)!: description`, with a type from [conventionalCommitTypes]. Opt-in.
         */
        var conventionalCommits: Boolean = false

        /** The types a Conventional Commit may start with. */
        var conventionalCommitTypes: MutableList<String> =
            mutableListOf("feat", "fix", "docs", "style", "refactor", "perf", "test", "build", "ci", "chore", "revert")

        /**
         * A pre-commit hook, installed by `armorInstallGitHooks`, that scans the staged changes with gitleaks and
         * blocks a commit that adds a secret. gitleaks is installed separately; without it, the hook warns and lets
         * the commit through. Opt-in.
         */
        var secretScan: Boolean = false
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
