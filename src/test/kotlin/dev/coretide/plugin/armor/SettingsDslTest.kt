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

/** The settings blocks in a Kotlin build script, and the flat settings of 0.4.0 they replace. */
class SettingsDslTest {
    /** Prints settings, read back from the extension once the build script has run. */
    private fun printing(vararg settings: Pair<String, String>): String =
        """
        tasks.register("printSettings") {
            val armor = codeArmor
            val lines = listOf(${settings.joinToString { (label, value) -> "\"$label \" + $value" }})
            doLast { lines.forEach { println("DSL " + it) } }
        }
        """.trimIndent()

    @Test
    fun `the blocks take values and providers, and add to list defaults`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(
            dir,
            armorConfig =
                """
                coverage {
                    minimum = 0.8
                    exclusions.add("Dto")
                }
                tests { slowThresholdMillis = 5000 }
                owasp { nvdApiKey = providers.environmentVariable("CODEARMOR_UNSET_KEY").orElse("from-a-provider") }
                sonarqube { token = providers.gradleProperty("sonarToken") }
                gitHooks { conventionalCommitTypes.add("wip") }
                detekt.enabled = false
                projectType = dev.coretide.plugin.armor.ProjectType.JAVA_APPLICATION
                """.trimIndent().prependIndent("    "),
            extraScript =
                printing(
                    "MINIMUM" to "armor.coverage.minimum.get()",
                    "EXCLUSIONS" to "armor.coverage.exclusions.get()",
                    "SLOW" to "armor.tests.slowThresholdMillis.get()",
                    "NVD_KEY" to "armor.owasp.nvdApiKey.get()",
                    "TOKEN" to "armor.sonarqube.token.get()",
                    "TYPES" to "armor.gitHooks.conventionalCommitTypes.get()",
                    "DETEKT" to "armor.detekt.enabled.get()",
                ),
        )

        val output = ArmorTestFixture.run(dir, "printSettings", "-PsonarToken=from-a-property").output

        assertContains(output, "DSL MINIMUM 0.8")
        assertContains(output, "DSL EXCLUSIONS [Dto]")
        assertContains(output, "DSL SLOW 5000")
        assertContains(output, "DSL NVD_KEY from-a-provider")
        assertContains(output, "DSL TOKEN from-a-property")
        assertContains(output, "DSL TYPES [feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert, wip]")
        assertContains(output, "DSL DETEKT false")
        assertContains(output, "🛡️ CodeArmor: Detected Java Application project")
        assertFalse(output.contains("is deprecated"), output)
    }

    @Test
    fun `the settings of 0_4_0 still work, and the build and armorInfo say what replaces them`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(
            dir,
            armorConfig =
                """
                coverageMinimum = 0.8
                coverageExclusions.add("Dto")
                detekt = false
                sonarHostUrl = "https://sonar.example.com"
                diffCoverageMinimum = 0.9
                """.trimIndent().prependIndent("    "),
            extraScript =
                printing(
                    "MINIMUM" to "armor.coverage.minimum.get()",
                    "EXCLUSIONS" to "armor.coverage.exclusions.get()",
                    "DETEKT" to "armor.detekt.enabled.get()",
                    "HOST" to "armor.sonarqube.hostUrl.get()",
                    "DIFF" to "armor.diffCoverage.minimum.get()",
                ) +
                    // Gradle reuses a build script it compiled before, and its warnings with it: this one is new each run.
                    "\n// ${System.nanoTime()}",
        )

        val output = ArmorTestFixture.run(dir, "printSettings", "armorInfo").output

        assertContains(output, "DSL MINIMUM 0.8")
        assertContains(output, "DSL EXCLUSIONS [Dto]")
        assertContains(output, "DSL DETEKT false")
        assertContains(output, "DSL HOST https://sonar.example.com")
        assertContains(output, "DSL DIFF 0.9")
        // Kotlin's compiler warns as the build script compiles; CodeArmor says what to write instead.
        assertContains(output, "'var coverageMinimum: Double' is deprecated. Use coverage { minimum = … }")
        assertContains(output, "⚠️ CodeArmor: coverageMinimum is deprecated, and 1.0.0 removes it. Use coverage { minimum = 0.8 }.")
        assertContains(output, "⚠️ CodeArmor: coverageExclusions is deprecated, and 1.0.0 removes it. Use coverage { exclusions.add(…) }")
        assertContains(output, "⚠️ CodeArmor: detekt = false is deprecated, and 1.0.0 removes it. Use detekt { enabled = false }.")
        assertContains(output, "Use sonarqube { hostUrl = \"https://sonar.example.com\" }.")
        assertContains(output, "  ⚠️  diffCoverageMinimum is deprecated, and 1.0.0 removes it. Use diffCoverage { minimum = 0.9 }.")
    }
}
