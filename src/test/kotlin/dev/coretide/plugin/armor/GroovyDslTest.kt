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

import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** The `codeArmor` block in a Groovy build script, which every other test writes in Kotlin. */
class GroovyDslTest {
    private fun writeGroovyProject(
        dir: File,
        armorConfig: String,
        print: List<String>,
    ) {
        dir.resolve("settings.gradle").writeText("rootProject.name = 'groovy-fixture'\n")
        dir.resolve("build.gradle").writeText(
            """
            |plugins {
            |    id 'java'
            |    id 'dev.coretide.plugin.armor'
            |}
            |
            |repositories {
            |    mavenCentral()
            |}
            |
            |codeArmor {
            |${armorConfig.prependIndent("    ")}
            |}
            |
            |tasks.register('printSettings') {
            |    def armor = codeArmor
            |    def lines = [
            |        ${print.joinToString(",\n|        ")}
            |    ]
            |    doLast { lines.each { println 'GROOVY ' + it } }
            |}
            """.trimMargin(),
        )
        dir.resolve("src/main/java/com/example").mkdirs()
        dir.resolve("src/main/java/com/example/Sample.java").writeText("package com.example;\n\npublic class Sample {}\n")
    }

    @Test
    fun `every codeArmor block works in a Groovy build script`(
        @TempDir dir: File,
    ) {
        writeGroovyProject(
            dir,
            """
            gitHooks { enabled = false }
            versionFromGit = false
            coverage {
                minimum = 0.5
                exclusions = ['Dto']
            }
            diffCoverage { minimum = 0.8 }
            tests { flakyRetries = 1 }
            compilation { strict = false }
            dependencyHealth { forbiddenLicenses = ['GPL-3.0-only'] }
            owasp { nvdApiKey = providers.environmentVariable('CODEARMOR_UNSET_KEY').orElse('from-a-provider') }
            sonarqube { projectKey = 'groovy-key' }
            spotbugs {
                effort = 'MIN'
                reportLevel = 'LOW'
            }
            detekt.typeResolution = true
            checks {
                build = ['spotbugsMain']
            }
            codeStats {
                enabled = false
            }
            toolVersions {
                jacoco = '0.8.13'
            }
            """.trimIndent(),
            print =
                listOf(
                    "'EFFORT ' + armor.spotbugs.effort.get()",
                    "'REPORT_LEVEL ' + armor.spotbugs.reportLevel.get()",
                    "'BUILD ' + armor.checks.build.get()",
                    "'COVERAGE ' + armor.coverage.minimum.get()",
                    "'EXCLUSIONS ' + armor.coverage.exclusions.get()",
                    "'DIFF ' + armor.diffCoverage.minimum.get()",
                    "'RETRIES ' + armor.tests.flakyRetries.get()",
                    "'LICENSES ' + armor.dependencyHealth.forbiddenLicenses.get()",
                    "'NVD_KEY ' + armor.owasp.nvdApiKey.get()",
                    "'SONAR_KEY ' + armor.sonarqube.projectKey.get()",
                    "'TYPE_RESOLUTION ' + armor.detekt.typeResolution.get()",
                    "'JACOCO ' + jacoco.toolVersion",
                ),
        )

        val output = ArmorTestFixture.run(dir, "printSettings").output

        assertContains(output, "GROOVY EFFORT MIN")
        assertContains(output, "GROOVY REPORT_LEVEL LOW")
        assertContains(output, "GROOVY BUILD [spotbugsMain]")
        assertContains(output, "GROOVY COVERAGE 0.5")
        assertContains(output, "GROOVY EXCLUSIONS [Dto]")
        assertContains(output, "GROOVY DIFF 0.8")
        assertContains(output, "GROOVY RETRIES 1")
        assertContains(output, "GROOVY LICENSES [GPL-3.0-only]")
        assertContains(output, "GROOVY NVD_KEY from-a-provider")
        assertContains(output, "GROOVY SONAR_KEY groovy-key")
        assertContains(output, "GROOVY TYPE_RESOLUTION true")
        assertContains(output, "GROOVY JACOCO 0.8.13")
        assertFalse(output.contains("is deprecated"), output)
    }

    @Test
    fun `the settings of 0_4_0 still work in a Groovy build script, and say what replaces them`(
        @TempDir dir: File,
    ) {
        writeGroovyProject(
            dir,
            """
            enableGitHooks = false
            enableVersionFromGit = false
            coverageMinimum = 0.5
            coverageExclusions = ['Dto']
            spotbugs = false
            detekt = false
            sonarqube = false
            owasp = false
            """.trimIndent(),
            print =
                listOf(
                    "'COVERAGE ' + armor.coverage.minimum.get()",
                    "'EXCLUSIONS ' + armor.coverage.exclusions.get()",
                    "'SPOTBUGS ' + armor.spotbugs.enabled.get()",
                    "'DETEKT ' + armor.detekt.enabled.get()",
                    "'SONARQUBE ' + armor.sonarqube.enabled.get()",
                    "'OWASP ' + armor.owasp.enabled.get()",
                    "'HOOKS ' + armor.gitHooks.enabled.get()",
                ),
        )

        val output = ArmorTestFixture.run(dir, "printSettings").output

        assertContains(output, "GROOVY COVERAGE 0.5")
        assertContains(output, "GROOVY EXCLUSIONS [Dto]")
        assertContains(output, "GROOVY SPOTBUGS false")
        assertContains(output, "GROOVY DETEKT false")
        assertContains(output, "GROOVY SONARQUBE false")
        assertContains(output, "GROOVY OWASP false")
        assertContains(output, "GROOVY HOOKS false")
        assertContains(output, "coverageMinimum is deprecated, and 1.0.0 removes it. Use coverage { minimum = 0.5 }.")
        assertContains(output, "coverageExclusions is deprecated, and 1.0.0 removes it. Use coverage { exclusions = listOf(\"Dto\") }.")
        assertContains(output, "spotbugs = false is deprecated, and 1.0.0 removes it. Use spotbugs { enabled = false }.")
        assertContains(output, "enableGitHooks is deprecated, and 1.0.0 removes it. Use gitHooks { enabled = false }.")
    }
}
