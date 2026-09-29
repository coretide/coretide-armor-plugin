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

/** How CodeArmor configures OWASP Dependency Check, read back from its extension. Nothing is scanned. */
class OwaspTest {
    /** Prints the settings once CodeArmor has configured the project, and the JVM-wide ones it must leave alone. */
    private val printOwasp =
        """
        gradle.projectsEvaluated {
            val check = project.extensions.getByType(org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension::class.java)
            val lines =
                listOf(
                    "SCAN " + check.scanConfigurations.get(),
                    "SKIP_PROJECTS " + check.skipProjects.get(),
                    "CENTRAL " + check.analyzers.centralEnabled.get(),
                    "RETIREJS " + check.analyzers.retirejs.enabled.get(),
                    "NVD_KEY " + check.nvd.apiKey.orNull,
                    "NVD_DELAY " + check.nvd.delay.get(),
                    "SYSTEM_CENTRAL " + System.getProperty("analyzer.central.enabled"),
                    "SYSTEM_NVD_KEY " + System.getProperty("nvd.api.key"),
                )
            project.tasks.register("printOwasp") { doLast { lines.forEach { println("OWASP " + it) } } }
        }
        """.trimIndent()

    @Test
    fun `OWASP scans what ships, and is configured through its extension, not JVM-wide properties`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(
            dir,
            armorConfig = "    owaspNvdApiKey = \"test-key\"\n    owaspNvdApiDelay = 1234",
            extraScript = printOwasp,
        )

        val output = ArmorTestFixture.run(dir, "printOwasp").output

        // Not every resolvable configuration: CodeArmor's own tools would otherwise be scanned and fail the build.
        assertContains(output, "OWASP SCAN [runtimeClasspath]")
        assertContains(output, "OWASP SKIP_PROJECTS []")
        assertContains(output, "OWASP CENTRAL false")
        assertContains(output, "OWASP RETIREJS false")
        assertContains(output, "OWASP NVD_KEY test-key")
        assertContains(output, "OWASP NVD_DELAY 1234")
        // System properties outlive the build in the Gradle daemon and are shared by every project.
        assertContains(output, "OWASP SYSTEM_CENTRAL null")
        assertContains(output, "OWASP SYSTEM_NVD_KEY null")
    }
}
