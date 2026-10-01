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
import dev.coretide.plugin.armor.config.CompilationConfig
import dev.coretide.plugin.armor.config.CoverageConfig
import dev.coretide.plugin.armor.config.DependencyHealthConfig
import dev.coretide.plugin.armor.config.Deprecations
import dev.coretide.plugin.armor.config.DetektConfig
import dev.coretide.plugin.armor.config.DiffCoverageConfig
import dev.coretide.plugin.armor.config.GitHooksConfig
import dev.coretide.plugin.armor.config.LibraryApiConfig
import dev.coretide.plugin.armor.config.MutationTestingConfig
import dev.coretide.plugin.armor.config.OwaspConfig
import dev.coretide.plugin.armor.config.SonarqubeConfig
import dev.coretide.plugin.armor.config.SpotBugsConfig
import dev.coretide.plugin.armor.config.TestsConfig
import dev.coretide.plugin.armor.config.ToolVersionsConfig
import dev.coretide.plugin.armor.configurator.ArchitectureTestsConfigurator
import dev.coretide.plugin.armor.configurator.ErrorProneConfigurator
import dev.coretide.plugin.armor.configurator.JacocoConfigurator
import dev.coretide.plugin.armor.configurator.MutationTestingConfigurator
import dev.coretide.plugin.armor.configurator.VeracodeConfigurator
import dev.coretide.plugin.armor.enumeration.ArmorLogLevel
import dev.coretide.plugin.armor.enumeration.CodeStatsScope
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * The `codeArmor { }` block. Each tool and area has a block of its own, `coverage { minimum = 0.8 }`, and every
 * setting is a Gradle property, so it also takes a provider: `owasp { nvdApiKey = providers.environmentVariable("KEY") }`.
 *
 * The flat settings of 0.4.0 and earlier, such as `coverageMinimum`, still work, and are deprecated: the build log and
 * `armorInfo` name each one used, with what to write instead. 1.0.0 removes them.
 */
abstract class CodeArmorExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        /** The deprecated settings the build script used, with what replaces each; logged once it has run. */
        val deprecations = Deprecations()

        /** The kind of project. Unset, CodeArmor detects it from the plugins and the sources. */
        abstract val projectType: Property<ProjectType>

        abstract val logLevel: Property<ArmorLogLevel>

        /**
         * gitleaks: a pre-commit hook, installed by `armorInstallGitHooks`, that blocks a commit adding a secret, and
         * `armorSecretScan` over the whole history in `fullAnalysis`. gitleaks is installed separately. Opt-in.
         */
        abstract val secretScan: Property<Boolean>

        /** Sets the project version from the latest git tag, when the build sets none. */
        abstract val versionFromGit: Property<Boolean>

        /** In applications, replaces `@version@`-style tokens in `src/main/resources` with the project's values. */
        abstract val resourceProcessing: Property<Boolean>

        /** Test coverage with JaCoCo or Kover; see [CoverageConfig]. */
        val coverage: CoverageConfig =
            objects.newInstance(CoverageConfig::class.java).apply {
                enabled.convention(true)
                kover.convention(false)
                minimum.convention(0.30)
                classMinimum.convention(0.25)
                defaultExclusions.convention(true)
            }

        /** The coverage of the lines changed since the base branch; see [DiffCoverageConfig]. */
        val diffCoverage: DiffCoverageConfig =
            objects.newInstance(DiffCoverageConfig::class.java, deprecations).apply {
                enabled.convention(true)
            }

        /** How tests run, and the kinds of tests CodeArmor adds; see [TestsConfig]. */
        val tests: TestsConfig =
            objects.newInstance(TestsConfig::class.java).apply {
                junitPlatform.convention(true)
                flakyRetries.convention(2)
                slowThresholdMillis.convention(2000L)
                integrationTests.convention(false)
                architectureTests.convention(false)
            }

        /** PIT mutation testing; see [MutationTestingConfig]. */
        val mutationTesting: MutationTestingConfig =
            objects.newInstance(MutationTestingConfig::class.java, deprecations).apply {
                enabled.convention(false)
                threshold.convention(0)
            }

        /** Checks as the production code compiles; see [CompilationConfig]. */
        val compilation: CompilationConfig =
            objects.newInstance(CompilationConfig::class.java).apply {
                strict.convention(false)
                errorProne.convention(false)
                nullAway.convention(false)
            }

        /** SpotBugs; see [SpotBugsConfig]. */
        val spotbugs: SpotBugsConfig =
            objects.newInstance(SpotBugsConfig::class.java, deprecations).apply {
                enabled.convention(true)
                toolVersion.convention(SpotBugsConfig.DEFAULT_TOOL_VERSION)
                effort.convention("MAX")
                reportLevel.convention("HIGH")
                ignoreFailures.convention(false)
                showStackTraces.convention(true)
                showProgress.convention(true)
                baselineFile.convention("config/spotbugs/baseline.xml")
                xmlReports.convention(true)
                htmlReports.convention(true)
                textReports.convention(false)
                sarifReports.convention(true)
            }

        /** detekt, for Kotlin projects; see [DetektConfig]. */
        val detekt: DetektConfig =
            objects.newInstance(DetektConfig::class.java, deprecations).apply {
                enabled.convention(true)
                typeResolution.convention(false)
            }

        /** OWASP Dependency-Check; see [OwaspConfig]. */
        val owasp: OwaspConfig =
            objects.newInstance(OwaspConfig::class.java, deprecations).apply {
                enabled.convention(true)
                failBuildOnCvss.convention(9.0)
                autoUpdate.convention(true)
                nvdApiDelay.convention(4000)
                nvdMaxRetryCount.convention(10)
                nvdValidForHours.convention(24)
            }

        /** Dependency updates, the SBOM and licences, and dependency analysis; see [DependencyHealthConfig]. */
        val dependencyHealth: DependencyHealthConfig =
            objects.newInstance(DependencyHealthConfig::class.java).apply {
                updates.convention(true)
                sbom.convention(true)
                analysis.convention(false)
            }

        /** API compatibility checks for libraries; see [LibraryApiConfig]. */
        val libraryApi: LibraryApiConfig =
            objects.newInstance(LibraryApiConfig::class.java).apply {
                kotlinAbiValidation.convention(false)
            }

        /** SonarQube; see [SonarqubeConfig]. */
        val sonarqube: SonarqubeConfig =
            objects.newInstance(SonarqubeConfig::class.java, deprecations).apply {
                enabled.convention(true)
                qualityGateWait.convention(false)
            }

        /** The git hooks `armorInstallGitHooks` installs; see [GitHooksConfig]. */
        val gitHooks: GitHooksConfig =
            objects.newInstance(GitHooksConfig::class.java).apply {
                enabled.convention(true)
                prePush.convention(true)
                conventionalCommits.convention(false)
                // Set, not a convention, so conventionalCommitTypes.add("wip") adds to them.
                conventionalCommitTypes.set(DEFAULT_COMMIT_TYPES)
            }

        /** Which tasks the pre-push, build and CI check tiers run. See [ChecksConfig]. */
        val checks: ChecksConfig =
            objects.newInstance(ChecksConfig::class.java).apply {
                prePush.set(listOf("quickBuild"))
            }

        /** Reporting commit activity to Code::Stats. Off by default; see [CodeStatsConfig]. */
        val codeStats: CodeStatsConfig =
            objects.newInstance(CodeStatsConfig::class.java).apply {
                enabled.convention(false)
                scope.convention(CodeStatsScope.REPO)
            }

        /** Overrides for the versions of the tools CodeArmor runs; see [ToolVersionsConfig]. */
        val toolVersions: ToolVersionsConfig =
            objects.newInstance(ToolVersionsConfig::class.java).apply {
                jacoco.convention(JacocoConfigurator.TOOL_VERSION)
                pitest.convention(MutationTestingConfigurator.PITEST_VERSION)
                errorProne.convention(ErrorProneConfigurator.ERROR_PRONE_VERSION)
                nullAway.convention(ErrorProneConfigurator.NULLAWAY_VERSION)
                archUnit.convention(ArchitectureTestsConfigurator.ARCHUNIT_VERSION)
            }

        init {
            logLevel.convention(ArmorLogLevel.ESSENTIAL)
            secretScan.convention(false)
            versionFromGit.convention(true)
            resourceProcessing.convention(true)
        }

        // The blocks take a Gradle Action, not a Kotlin lambda, so they also work in Groovy build scripts.

        fun coverage(action: Action<CoverageConfig>) = action.execute(coverage)

        fun diffCoverage(action: Action<DiffCoverageConfig>) = action.execute(diffCoverage)

        fun tests(action: Action<TestsConfig>) = action.execute(tests)

        fun mutationTesting(action: Action<MutationTestingConfig>) = action.execute(mutationTesting)

        fun compilation(action: Action<CompilationConfig>) = action.execute(compilation)

        fun spotbugs(action: Action<SpotBugsConfig>) = action.execute(spotbugs)

        fun detekt(action: Action<DetektConfig>) = action.execute(detekt)

        fun owasp(action: Action<OwaspConfig>) = action.execute(owasp)

        fun dependencyHealth(action: Action<DependencyHealthConfig>) = action.execute(dependencyHealth)

        fun libraryApi(action: Action<LibraryApiConfig>) = action.execute(libraryApi)

        fun sonarqube(action: Action<SonarqubeConfig>) = action.execute(sonarqube)

        fun gitHooks(action: Action<GitHooksConfig>) = action.execute(gitHooks)

        fun checks(action: Action<ChecksConfig>) = action.execute(checks)

        fun codeStats(action: Action<CodeStatsConfig>) = action.execute(codeStats)

        fun toolVersions(action: Action<ToolVersionsConfig>) = action.execute(toolVersions)

        // ---------------------------------------------------------------------------------------------------------
        // Deprecated in 0.5.0; 1.0.0 removes them. Each reads and writes the setting that replaced it.
        // ---------------------------------------------------------------------------------------------------------

        /** Whether the deprecated [isMultiModule] forced a multi-module build. */
        internal var forcedMultiModule = false
            private set

        @Deprecated("Set projectType to choose the project type; unset, CodeArmor detects it. 1.0.0 removes autoDetect.")
        var autoDetect: Boolean = true
            set(value) {
                deprecations.use("autoDetect", "Set projectType to choose the project type; unset, CodeArmor detects it.")
                field = value
            }

        @Deprecated("CodeArmor detects a multi-module build from its subprojects. 1.0.0 removes isMultiModule.")
        var isMultiModule: Boolean
            get() = forcedMultiModule
            set(value) {
                deprecations.use("isMultiModule", "CodeArmor detects a multi-module build from its subprojects.")
                forcedMultiModule = value
            }

        @Deprecated(
            "1.0.0 removes it. To keep running your Veracode plugin's upload in fullAnalysis, list it in the CI " +
                "tier: checks { ci.add(\"veracodeUpload\") }",
        )
        var veracode: Boolean = false
            set(value) {
                deprecations.use(
                    "veracode",
                    "To keep running your Veracode plugin's upload in fullAnalysis, list it in the CI tier: " +
                        VeracodeConfigurator.REPLACEMENT,
                )
                field = value
            }

        @Deprecated("Use versionFromGit = …; 1.0.0 removes enableVersionFromGit.")
        var enableVersionFromGit: Boolean
            get() = versionFromGit.get()
            set(value) = renamed("enableVersionFromGit", "versionFromGit", value, versionFromGit)

        @Deprecated("Use resourceProcessing = …; 1.0.0 removes enableResourceProcessing.")
        var enableResourceProcessing: Boolean
            get() = resourceProcessing.get()
            set(value) = renamed("enableResourceProcessing", "resourceProcessing", value, resourceProcessing)

        /** `spotbugs = false` in a Groovy build script; Kotlin ones go through [SpotBugsConfig.assign]. */
        @Deprecated("Use spotbugs { enabled = … }; 1.0.0 removes spotbugs = true|false.")
        @Suppress("DEPRECATION")
        fun setSpotbugs(enabled: Boolean) = spotbugs.assign(enabled)

        @Deprecated("Use spotbugs { … }; 1.0.0 removes spotbugsConfig.")
        val spotbugsConfig: SpotBugsConfig
            get() {
                deprecations.use("spotbugsConfig", "Use spotbugs { … }, or spotbugs.")
                return spotbugs
            }

        @Deprecated("Use detekt { enabled = … }; 1.0.0 removes detekt = true|false.")
        @Suppress("DEPRECATION")
        fun setDetekt(enabled: Boolean) = detekt.assign(enabled)

        @Deprecated("Use detekt { typeResolution = … }; 1.0.0 removes detektTypeResolution.")
        var detektTypeResolution: Boolean
            get() = detekt.typeResolution.get()
            set(value) = moved("detektTypeResolution", "detekt", "typeResolution", value, detekt.typeResolution)

        @Deprecated("Use owasp { enabled = … }; 1.0.0 removes owasp = true|false.")
        @Suppress("DEPRECATION")
        fun setOwasp(enabled: Boolean) = owasp.assign(enabled)

        @Deprecated("Use owasp { failBuildOnCvss = … }; 1.0.0 removes owaspFailBuildOnCVSS.")
        var owaspFailBuildOnCVSS: Double
            get() = owasp.failBuildOnCvss.get()
            set(value) = moved("owaspFailBuildOnCVSS", "owasp", "failBuildOnCvss", value, owasp.failBuildOnCvss)

        @Deprecated("Use owasp { suppressionFile = … }; 1.0.0 removes owaspSuppressionFile.")
        var owaspSuppressionFile: String?
            get() = owasp.suppressionFile.orNull
            set(value) = moved("owaspSuppressionFile", "owasp", "suppressionFile", value, owasp.suppressionFile)

        @Deprecated("Use owasp { autoUpdate = … }; 1.0.0 removes owaspAutoUpdate.")
        var owaspAutoUpdate: Boolean
            get() = owasp.autoUpdate.get()
            set(value) = moved("owaspAutoUpdate", "owasp", "autoUpdate", value, owasp.autoUpdate)

        @Deprecated("Use owasp { nvdApiKey = … }; 1.0.0 removes owaspNvdApiKey.")
        var owaspNvdApiKey: String?
            get() = owasp.nvdApiKey.orNull
            set(value) = moved("owaspNvdApiKey", "owasp", "nvdApiKey", value, owasp.nvdApiKey)

        @Deprecated("Use owasp { nvdApiDelay = … }; 1.0.0 removes owaspNvdApiDelay.")
        var owaspNvdApiDelay: Int
            get() = owasp.nvdApiDelay.get()
            set(value) = moved("owaspNvdApiDelay", "owasp", "nvdApiDelay", value, owasp.nvdApiDelay)

        @Deprecated("Use owasp { nvdMaxRetryCount = … }; 1.0.0 removes owaspNvdMaxRetryCount.")
        var owaspNvdMaxRetryCount: Int
            get() = owasp.nvdMaxRetryCount.get()
            set(value) = moved("owaspNvdMaxRetryCount", "owasp", "nvdMaxRetryCount", value, owasp.nvdMaxRetryCount)

        @Deprecated("Use owasp { nvdValidForHours = … }; 1.0.0 removes owaspNvdValidForHours.")
        var owaspNvdValidForHours: Int
            get() = owasp.nvdValidForHours.get()
            set(value) = moved("owaspNvdValidForHours", "owasp", "nvdValidForHours", value, owasp.nvdValidForHours)

        @Deprecated("Use sonarqube { enabled = … }; 1.0.0 removes sonarqube = true|false.")
        @Suppress("DEPRECATION")
        fun setSonarqube(enabled: Boolean) = sonarqube.assign(enabled)

        @Deprecated("Use sonarqube { hostUrl = … }; 1.0.0 removes sonarHostUrl.")
        var sonarHostUrl: String?
            get() = sonarqube.hostUrl.orNull
            set(value) = moved("sonarHostUrl", "sonarqube", "hostUrl", value, sonarqube.hostUrl)

        @Deprecated("Use sonarqube { projectKey = … }; 1.0.0 removes sonarProjectKey.")
        var sonarProjectKey: String?
            get() = sonarqube.projectKey.orNull
            set(value) = moved("sonarProjectKey", "sonarqube", "projectKey", value, sonarqube.projectKey)

        @Deprecated("Use sonarqube { projectName = … }; 1.0.0 removes sonarProjectName.")
        var sonarProjectName: String?
            get() = sonarqube.projectName.orNull
            set(value) = moved("sonarProjectName", "sonarqube", "projectName", value, sonarqube.projectName)

        @Deprecated("Use sonarqube { token = … }; 1.0.0 removes sonarToken.")
        var sonarToken: String?
            get() = sonarqube.token.orNull
            set(value) = moved("sonarToken", "sonarqube", "token", value, sonarqube.token)

        @Deprecated("Use sonarqube { qualityGateWait = … }; 1.0.0 removes sonarQualityGateWait.")
        var sonarQualityGateWait: Boolean
            get() = sonarqube.qualityGateWait.get()
            set(value) = moved("sonarQualityGateWait", "sonarqube", "qualityGateWait", value, sonarqube.qualityGateWait)

        @Deprecated("Use sonarqube { javaVersion = … }; 1.0.0 removes sonarJavaVersion.")
        var sonarJavaVersion: String?
            get() = sonarqube.javaVersion.orNull
            set(value) = moved("sonarJavaVersion", "sonarqube", "javaVersion", value, sonarqube.javaVersion)

        @Deprecated("Use coverage { enabled = … }; 1.0.0 removes jacoco.")
        var jacoco: Boolean
            get() = coverage.enabled.get()
            set(value) = moved("jacoco", "coverage", "enabled", value, coverage.enabled)

        @Deprecated("Use coverage { kover = … }; 1.0.0 removes it from here.")
        var kover: Boolean
            get() = coverage.kover.get()
            set(value) = moved("kover", "coverage", "kover", value, coverage.kover)

        @Deprecated("Use coverage { minimum = … }; 1.0.0 removes coverageMinimum.")
        var coverageMinimum: Double
            get() = coverage.minimum.get()
            set(value) = moved("coverageMinimum", "coverage", "minimum", value, coverage.minimum)

        @Deprecated("Use coverage { classMinimum = … }; 1.0.0 removes coverageClassMinimum.")
        var coverageClassMinimum: Double
            get() = coverage.classMinimum.get()
            set(value) = moved("coverageClassMinimum", "coverage", "classMinimum", value, coverage.classMinimum)

        @Deprecated("Use coverage { inclusions = … }; 1.0.0 removes coverageInclusions.")
        var coverageInclusions: MutableList<String>
            get() = movedList("coverageInclusions", "coverage", "inclusions", coverage.inclusions)
            set(value) = moved("coverageInclusions", "coverage", "inclusions", value, coverage.inclusions)

        @Deprecated("Use coverage { exclusions = … }; 1.0.0 removes coverageExclusions.")
        var coverageExclusions: MutableList<String>
            get() = movedList("coverageExclusions", "coverage", "exclusions", coverage.exclusions)
            set(value) = moved("coverageExclusions", "coverage", "exclusions", value, coverage.exclusions)

        @Deprecated("Use coverage { defaultExclusions = … }; 1.0.0 removes coverageIncludeDefaultExclusions.")
        var coverageIncludeDefaultExclusions: Boolean
            get() = coverage.defaultExclusions.get()
            set(value) =
                moved("coverageIncludeDefaultExclusions", "coverage", "defaultExclusions", value, coverage.defaultExclusions)

        @Deprecated("Use diffCoverage { enabled = … }; 1.0.0 removes diffCoverage = true|false.")
        @Suppress("DEPRECATION")
        fun setDiffCoverage(enabled: Boolean) = diffCoverage.assign(enabled)

        @Deprecated("Use diffCoverage { minimum = … }; 1.0.0 removes diffCoverageMinimum.")
        var diffCoverageMinimum: Double?
            get() = diffCoverage.minimum.orNull
            set(value) = moved("diffCoverageMinimum", "diffCoverage", "minimum", value, diffCoverage.minimum)

        @Deprecated("Use diffCoverage { base = … }; 1.0.0 removes diffCoverageBase.")
        var diffCoverageBase: String?
            get() = diffCoverage.base.orNull
            set(value) = moved("diffCoverageBase", "diffCoverage", "base", value, diffCoverage.base)

        @Deprecated("Use tests { integrationTests = … }; 1.0.0 removes it from here.")
        var integrationTests: Boolean
            get() = tests.integrationTests.get()
            set(value) = moved("integrationTests", "tests", "integrationTests", value, tests.integrationTests)

        @Deprecated("Use tests { junitPlatform = … }; 1.0.0 removes it from here.")
        var junitPlatform: Boolean
            get() = tests.junitPlatform.get()
            set(value) = moved("junitPlatform", "tests", "junitPlatform", value, tests.junitPlatform)

        @Deprecated("Use tests { flakyRetries = … }; 1.0.0 removes flakyTestRetries.")
        var flakyTestRetries: Int
            get() = tests.flakyRetries.get()
            set(value) = moved("flakyTestRetries", "tests", "flakyRetries", value, tests.flakyRetries)

        @Deprecated("Use tests { slowThresholdMillis = … }; 1.0.0 removes slowTestThresholdMillis.")
        var slowTestThresholdMillis: Long
            get() = tests.slowThresholdMillis.get()
            set(value) = moved("slowTestThresholdMillis", "tests", "slowThresholdMillis", value, tests.slowThresholdMillis)

        @Deprecated("Use tests { architectureTests = … }; 1.0.0 removes it from here.")
        var architectureTests: Boolean
            get() = tests.architectureTests.get()
            set(value) = moved("architectureTests", "tests", "architectureTests", value, tests.architectureTests)

        @Deprecated("Use mutationTesting { enabled = … }; 1.0.0 removes mutationTesting = true|false.")
        @Suppress("DEPRECATION")
        fun setMutationTesting(enabled: Boolean) = mutationTesting.assign(enabled)

        @Deprecated("Use mutationTesting { threshold = … }; 1.0.0 removes mutationThreshold.")
        var mutationThreshold: Int
            get() = mutationTesting.threshold.get()
            set(value) = moved("mutationThreshold", "mutationTesting", "threshold", value, mutationTesting.threshold)

        @Deprecated("Use compilation { strict = … }; 1.0.0 removes strictCompilation.")
        var strictCompilation: Boolean
            get() = compilation.strict.get()
            set(value) = moved("strictCompilation", "compilation", "strict", value, compilation.strict)

        @Deprecated("Use compilation { errorProne = … }; 1.0.0 removes it from here.")
        var errorProne: Boolean
            get() = compilation.errorProne.get()
            set(value) = moved("errorProne", "compilation", "errorProne", value, compilation.errorProne)

        @Deprecated("Use compilation { nullAway = … }; 1.0.0 removes it from here.")
        var nullAway: Boolean
            get() = compilation.nullAway.get()
            set(value) = moved("nullAway", "compilation", "nullAway", value, compilation.nullAway)

        @Deprecated("Use dependencyHealth { updates = … }; 1.0.0 removes dependencyUpdates.")
        var dependencyUpdates: Boolean
            get() = dependencyHealth.updates.get()
            set(value) = moved("dependencyUpdates", "dependencyHealth", "updates", value, dependencyHealth.updates)

        @Deprecated("Use dependencyHealth { sbom = … }; 1.0.0 removes it from here.")
        var sbom: Boolean
            get() = dependencyHealth.sbom.get()
            set(value) = moved("sbom", "dependencyHealth", "sbom", value, dependencyHealth.sbom)

        @Deprecated("Use dependencyHealth { forbiddenLicenses = … }; 1.0.0 removes it from here.")
        var forbiddenLicenses: MutableList<String>
            get() = movedList("forbiddenLicenses", "dependencyHealth", "forbiddenLicenses", dependencyHealth.forbiddenLicenses)
            set(value) = moved("forbiddenLicenses", "dependencyHealth", "forbiddenLicenses", value, dependencyHealth.forbiddenLicenses)

        @Deprecated("Use dependencyHealth { analysis = … }; 1.0.0 removes dependencyAnalysis.")
        var dependencyAnalysis: Boolean
            get() = dependencyHealth.analysis.get()
            set(value) = moved("dependencyAnalysis", "dependencyHealth", "analysis", value, dependencyHealth.analysis)

        @Deprecated("Use libraryApi { baseline = … }; 1.0.0 removes apiBaseline.")
        var apiBaseline: String?
            get() = libraryApi.baseline.orNull
            set(value) = moved("apiBaseline", "libraryApi", "baseline", value, libraryApi.baseline)

        @Deprecated("Use libraryApi { kotlinAbiValidation = … }; 1.0.0 removes it from here.")
        var kotlinAbiValidation: Boolean
            get() = libraryApi.kotlinAbiValidation.get()
            set(value) = moved("kotlinAbiValidation", "libraryApi", "kotlinAbiValidation", value, libraryApi.kotlinAbiValidation)

        @Deprecated("Use gitHooks { enabled = … }; 1.0.0 removes enableGitHooks.")
        var enableGitHooks: Boolean
            get() = gitHooks.enabled.get()
            set(value) = moved("enableGitHooks", "gitHooks", "enabled", value, gitHooks.enabled)

        @Deprecated("Use gitHooks { prePush = … }; 1.0.0 removes prePushEnabled.")
        var prePushEnabled: Boolean
            get() = gitHooks.prePush.get()
            set(value) = moved("prePushEnabled", "gitHooks", "prePush", value, gitHooks.prePush)

        @Deprecated("Use gitHooks { conventionalCommits = … }; 1.0.0 removes it from here.")
        var conventionalCommits: Boolean
            get() = gitHooks.conventionalCommits.get()
            set(value) = moved("conventionalCommits", "gitHooks", "conventionalCommits", value, gitHooks.conventionalCommits)

        @Deprecated("Use gitHooks { conventionalCommitTypes = … }; 1.0.0 removes it from here.")
        var conventionalCommitTypes: MutableList<String>
            get() = movedList("conventionalCommitTypes", "gitHooks", "conventionalCommitTypes", gitHooks.conventionalCommitTypes)
            set(value) =
                moved("conventionalCommitTypes", "gitHooks", "conventionalCommitTypes", value, gitHooks.conventionalCommitTypes)

        /** A setting that moved into a block: records the use, with the line that replaces it, and sets the new one. */
        private fun <T : Any> moved(
            setting: String,
            block: String,
            name: String,
            value: T?,
            property: Property<T>,
        ) {
            deprecations.use(setting, "Use $block { $name = ${Deprecations.dsl(value)} }.")
            property.set(value)
        }

        private fun moved(
            setting: String,
            block: String,
            name: String,
            value: List<String>,
            property: ListProperty<String>,
        ) {
            deprecations.use(setting, "Use $block { $name = ${Deprecations.dsl(value)} }.")
            property.set(value.toList())
        }

        /** A setting that became a top-level property of its own. */
        private fun renamed(
            setting: String,
            name: String,
            value: Boolean,
            property: Property<Boolean>,
        ) {
            deprecations.use(setting, "Use $name = $value.")
            property.set(value)
        }

        /** A list setting that moved into a block, as a list that writes through to the new property. */
        private fun movedList(
            setting: String,
            block: String,
            name: String,
            property: ListProperty<String>,
        ): MutableList<String> =
            WriteThroughList(property) {
                deprecations.use(setting, "Use $block { $name.add(…) }, or $name = listOf(…).")
            }

        /** A deprecated list setting: reads the property it moved to, and writes changes back to it. */
        private class WriteThroughList(
            private val property: ListProperty<String>,
            private val onWrite: () -> Unit,
        ) : AbstractMutableList<String>() {
            override val size: Int get() = property.get().size

            override fun get(index: Int): String = property.get()[index]

            override fun add(
                index: Int,
                element: String,
            ) = update { it.add(index, element) }

            override fun removeAt(index: Int): String {
                var removed = ""
                update { removed = it.removeAt(index) }
                return removed
            }

            override fun set(
                index: Int,
                element: String,
            ): String {
                var previous = ""
                update { previous = it.set(index, element) }
                return previous
            }

            private fun update(change: (MutableList<String>) -> Unit) {
                onWrite()
                property.set(property.get().toMutableList().also(change))
            }
        }

        companion object {
            val DEFAULT_COMMIT_TYPES =
                listOf("feat", "fix", "docs", "style", "refactor", "perf", "test", "build", "ci", "chore", "revert")
        }
    }
