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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** The `codeArmor` block in a Groovy build script, which every other test writes in Kotlin. */
class GroovyDslTest {
    @Test
    fun `every codeArmor setting and block works in a Groovy build script`(
        @TempDir dir: File,
    ) {
        dir.resolve("settings.gradle").writeText("rootProject.name = 'groovy-fixture'\n")
        dir.resolve("build.gradle").writeText(
            """
            plugins {
                id 'java'
                id 'dev.coretide.plugin.armor'
            }

            repositories {
                mavenCentral()
            }

            codeArmor {
                enableGitHooks = false
                enableVersionFromGit = false
                coverageMinimum = 0.5
                coverageExclusions = ['Dto']
                forbiddenLicenses = ['GPL-3.0-only']
                conventionalCommitTypes = ['feat', 'fix']
                spotbugs {
                    effort = 'MIN'
                    reportLevel = 'LOW'
                }
                checks {
                    build = ['spotbugsMain']
                }
                codeStats {
                    enabled = false
                }
            }

            tasks.register('printSettings') {
                def armor = codeArmor
                def lines = [
                    'EFFORT ' + armor.spotbugsConfig.effort,
                    'REPORT_LEVEL ' + armor.spotbugsConfig.reportLevel,
                    'BUILD ' + armor.checks.build.get(),
                    'COVERAGE ' + armor.coverageMinimum,
                    'EXCLUSIONS ' + armor.coverageExclusions,
                ]
                doLast { lines.each { println 'GROOVY ' + it } }
            }
            """.trimIndent(),
        )
        dir.resolve("src/main/java/com/example").mkdirs()
        dir.resolve("src/main/java/com/example/Sample.java").writeText("package com.example;\n\npublic class Sample {}\n")

        val output = ArmorTestFixture.run(dir, "printSettings").output

        assertContains(output, "GROOVY EFFORT MIN")
        assertContains(output, "GROOVY REPORT_LEVEL LOW")
        assertContains(output, "GROOVY BUILD [spotbugsMain]")
        assertContains(output, "GROOVY COVERAGE 0.5")
        assertContains(output, "GROOVY EXCLUSIONS [Dto]")
    }
}
