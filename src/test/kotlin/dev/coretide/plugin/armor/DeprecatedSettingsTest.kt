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

import org.gradle.testfixtures.ProjectBuilder
import java.lang.reflect.Modifier
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

/** The flat settings of 0.4.0: each sets the one that replaced it, and records what to write instead. */
@Suppress("DEPRECATION")
class DeprecatedSettingsTest {
    private fun extension(): CodeArmorExtension = ProjectBuilder.builder().build().extensions.create("codeArmor", CodeArmorExtension::class.java)

    @Test
    fun `each deprecated setting sets the one that replaced it`() {
        val armor = extension()

        armor.jacoco = false
        armor.kover = true
        armor.coverageMinimum = 0.8
        armor.coverageClassMinimum = 0.7
        armor.coverageInclusions = mutableListOf("core")
        armor.coverageExclusions.add("Dto")
        armor.coverageIncludeDefaultExclusions = false
        armor.setDiffCoverage(false)
        armor.diffCoverageMinimum = 0.9
        armor.diffCoverageBase = "develop"
        armor.integrationTests = true
        armor.junitPlatform = false
        armor.flakyTestRetries = 3
        armor.slowTestThresholdMillis = 500
        armor.architectureTests = true
        armor.setMutationTesting(true)
        armor.mutationThreshold = 60
        armor.strictCompilation = true
        armor.errorProne = true
        armor.nullAway = true
        armor.setSpotbugs(false)
        armor.spotbugsConfig.effort.set("MIN")
        armor.setDetekt(false)
        armor.detektTypeResolution = true
        armor.setOwasp(false)
        armor.owaspFailBuildOnCVSS = 7.0
        armor.owaspSuppressionFile = "suppressions.xml"
        armor.owaspAutoUpdate = false
        armor.owaspNvdApiKey = "key"
        armor.owaspNvdApiDelay = 1000
        armor.owaspNvdMaxRetryCount = 3
        armor.owaspNvdValidForHours = 12
        armor.dependencyUpdates = false
        armor.sbom = false
        armor.forbiddenLicenses = mutableListOf("GPL-3.0-only")
        armor.dependencyAnalysis = true
        armor.apiBaseline = "1.0.0"
        armor.kotlinAbiValidation = true
        armor.setSonarqube(false)
        armor.sonarHostUrl = "https://sonar.example.com"
        armor.sonarProjectKey = "key"
        armor.sonarProjectName = "name"
        armor.sonarToken = "token"
        armor.sonarQualityGateWait = true
        armor.sonarJavaVersion = "17"
        armor.enableGitHooks = false
        armor.prePushEnabled = false
        armor.conventionalCommits = true
        armor.conventionalCommitTypes.add("wip")
        armor.enableVersionFromGit = false
        armor.enableResourceProcessing = false

        with(armor.coverage) {
            assertEquals(false, enabled.get())
            assertEquals(true, kover.get())
            assertEquals(0.8, minimum.get())
            assertEquals(0.7, classMinimum.get())
            assertEquals(listOf("core"), inclusions.get())
            assertEquals(listOf("Dto"), exclusions.get())
            assertEquals(false, defaultExclusions.get())
        }
        with(armor.diffCoverage) {
            assertEquals(false, enabled.get())
            assertEquals(0.9, minimum.get())
            assertEquals("develop", base.get())
        }
        with(armor.tests) {
            assertEquals(true, integrationTests.get())
            assertEquals(false, junitPlatform.get())
            assertEquals(3, flakyRetries.get())
            assertEquals(500L, slowThresholdMillis.get())
            assertEquals(true, architectureTests.get())
        }
        assertEquals(true, armor.mutationTesting.enabled.get())
        assertEquals(60, armor.mutationTesting.threshold.get())
        assertEquals(listOf(true, true, true), with(armor.compilation) { listOf(strict.get(), errorProne.get(), nullAway.get()) })
        assertEquals(false, armor.spotbugs.enabled.get())
        assertEquals("MIN", armor.spotbugs.effort.get())
        assertEquals(false, armor.detekt.enabled.get())
        assertEquals(true, armor.detekt.typeResolution.get())
        with(armor.owasp) {
            assertEquals(false, enabled.get())
            assertEquals(7.0, failBuildOnCvss.get())
            assertEquals("suppressions.xml", suppressionFile.get())
            assertEquals(false, autoUpdate.get())
            assertEquals("key", nvdApiKey.get())
            assertEquals(listOf(1000, 3, 12), listOf(nvdApiDelay.get(), nvdMaxRetryCount.get(), nvdValidForHours.get()))
        }
        with(armor.dependencyHealth) {
            assertEquals(false, updates.get())
            assertEquals(false, sbom.get())
            assertEquals(listOf("GPL-3.0-only"), forbiddenLicenses.get())
            assertEquals(true, analysis.get())
        }
        assertEquals("1.0.0", armor.libraryApi.baseline.get())
        assertEquals(true, armor.libraryApi.kotlinAbiValidation.get())
        with(armor.sonarqube) {
            assertEquals(false, enabled.get())
            assertEquals(
                listOf("https://sonar.example.com", "key", "name", "token", "17"),
                listOf(hostUrl.get(), projectKey.get(), projectName.get(), token.get(), javaVersion.get()),
            )
            assertEquals(true, qualityGateWait.get())
        }
        with(armor.gitHooks) {
            assertEquals(false, enabled.get())
            assertEquals(false, prePush.get())
            assertEquals(true, conventionalCommits.get())
            assertEquals(CodeArmorExtension.DEFAULT_COMMIT_TYPES + "wip", conventionalCommitTypes.get())
        }
        assertEquals(false, armor.versionFromGit.get())
        assertEquals(false, armor.resourceProcessing.get())
    }

    @Test
    fun `each deprecated setting reads the one that replaced it`() {
        val armor = extension()
        armor.coverage.minimum.set(0.6)
        armor.coverage.exclusions.set(listOf("Dto"))
        armor.owasp.nvdApiKey.set("key")
        armor.gitHooks.enabled.set(false)

        assertEquals(0.6, armor.coverageMinimum)
        assertEquals(listOf("Dto"), armor.coverageExclusions)
        assertEquals("key", armor.owaspNvdApiKey)
        assertEquals(null, armor.sonarHostUrl)
        assertEquals(false, armor.enableGitHooks)
        // Reading is not a use of the setting.
        assertEquals(emptyMap(), armor.deprecations.settings)
    }

    @Test
    fun `the build is told what to write instead, as it can be copied`() {
        val armor = extension()

        armor.coverageMinimum = 0.8
        armor.sonarHostUrl = "https://sonar.example.com"
        armor.forbiddenLicenses = mutableListOf("GPL-3.0-only")
        armor.setDetekt(false)
        armor.enableVersionFromGit = false
        armor.veracode = true

        assertEquals(
            listOf(
                "coverageMinimum is deprecated, and 1.0.0 removes it. Use coverage { minimum = 0.8 }.",
                "sonarHostUrl is deprecated, and 1.0.0 removes it. Use sonarqube { hostUrl = \"https://sonar.example.com\" }.",
                "forbiddenLicenses is deprecated, and 1.0.0 removes it. Use dependencyHealth { forbiddenLicenses = listOf(\"GPL-3.0-only\") }.",
                "detekt = false is deprecated, and 1.0.0 removes it. Use detekt { enabled = false }.",
                "enableVersionFromGit is deprecated, and 1.0.0 removes it. Use versionFromGit = false.",
                "veracode is deprecated, and 1.0.0 removes it. To keep running your Veracode plugin's upload in " +
                    "fullAnalysis, list it in the CI tier: checks { ci.add(\"veracodeUpload\") }",
            ),
            armor.deprecations.messages(),
        )
    }

    @Test
    fun `every setter on the extension is a deprecated setting that says so`() {
        // The blocks and the new settings are read-only properties, so any setter is one of 0.4.0's flat settings.
        val setters =
            CodeArmorExtension::class.java.declaredMethods.filter {
                Modifier.isPublic(it.modifiers) && it.name.startsWith("set") && it.parameterCount == 1
            }
        assertTrue(setters.size > 40, "${setters.map { it.name }}")
        setters.forEach { setter ->
            val armor = extension()
            val value: Any =
                when (setter.parameterTypes.single()) {
                    Boolean::class.javaPrimitiveType, Boolean::class.javaObjectType -> true
                    Int::class.javaPrimitiveType, Int::class.javaObjectType -> 7
                    Long::class.javaPrimitiveType, Long::class.javaObjectType -> 7L
                    Double::class.javaPrimitiveType, Double::class.javaObjectType -> 0.7
                    String::class.java -> "value"
                    List::class.java -> mutableListOf("value")
                    else -> error("${setter.name} takes a ${setter.parameterTypes.single()}")
                }

            setter.invoke(armor, value)

            assertEquals(1, armor.deprecations.settings.size, setter.name)
        }
    }
}
