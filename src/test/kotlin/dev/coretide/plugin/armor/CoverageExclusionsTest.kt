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

/**
 * The default coverage exclusions leave out entry points, configuration and generated code, by whole suffix or
 * package, and nothing else. A build's own exclusions still match anywhere in a name.
 */
class CoverageExclusionsTest {
    /** Classes, by path under `src/main/java`, that the defaults leave out. */
    private val excluded =
        listOf(
            "com/example/DemoApplication",
            "com/example/AppConfig",
            "com/example/WebConfiguration",
            "com/example/UserMapperImpl",
            "com/example/User_",
            "com/example/generated/Generated",
        )

    /** Classes whose names only contain a word the pre-0.3.0 defaults matched anywhere. */
    private val kept =
        listOf(
            "com/example/DatabaseService",
            "com/example/KnowledgeBase",
            "com/example/ConfigParser",
            "com/example/ErrorHandler",
            "com/example/model/User",
            "com/example/util/Strings",
        )

    private fun writeProject(
        dir: File,
        armorConfig: String = "",
    ) {
        ArmorTestFixture.writeProject(
            dir,
            // Coverage verification would fail on the untested classes; only the report matters here.
            armorConfig = "    coverage { minimum = 0.0; classMinimum = 0.0 }\n$armorConfig",
            extraScript =
                """
                dependencies {
                    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
                    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
                }
                """.trimIndent(),
        )
        (excluded + kept).forEach { path ->
            val name = path.substringAfterLast('/')
            val pkg = path.substringBeforeLast('/').replace('/', '.')
            dir.resolve("src/main/java/$path.java").apply { parentFile.mkdirs() }.writeText(
                "package $pkg;\n\npublic class $name {\n    public int value() {\n        return 1;\n    }\n}\n",
            )
        }
        dir.resolve("src/test/java/com/example/SampleTest.java").apply { parentFile.mkdirs() }.writeText(
            """
            package com.example;

            import org.junit.jupiter.api.Test;

            class SampleTest {
                @Test
                void adds() {
                    org.junit.jupiter.api.Assertions.assertEquals(3, new Sample().add(1, 2));
                }
            }
            """.trimIndent(),
        )
    }

    private fun coverageReport(dir: File): String = dir.resolve("build/reports/jacoco/test/jacocoTestReport.xml").readText()

    @Test
    fun `the defaults leave out entry points, configuration and generated code, and nothing else`(
        @TempDir dir: File,
    ) {
        writeProject(dir)

        ArmorTestFixture.run(dir, "test")

        val report = coverageReport(dir)
        excluded.forEach { assertFalse(report.contains("<class name=\"$it\""), "$it is in the report") }
        kept.forEach { assertContains(report, "<class name=\"$it\"") }
    }

    @Test
    fun `the upgrade guide's snippet brings back the old defaults`(
        @TempDir dir: File,
    ) {
        // As CHANGELOG.md's "Upgrading from 0.2.x" gives it.
        writeProject(
            dir,
            armorConfig =
                """
                    coverageExclusions.addAll(
                        listOf(
                            "annotation", "model", "dto", "entity", "entities", "mapper", "util", "utils", "helper", "helpers",
                            "config", "Application", "Config", "Configuration", "Repository", "generated", "Test", "Mock",
                            "Stubs", "Dummy", "Fake", "Abstract", "Base", "Exception", "Error", "logging",
                        ),
                    )
                """.trimIndent(),
        )

        ArmorTestFixture.run(dir, "test")

        val report = coverageReport(dir)
        // The old defaults matched case-sensitively: "Base" never left out DatabaseService.
        (excluded + kept - "com/example/DatabaseService").forEach {
            assertFalse(report.contains("<class name=\"$it\""), "$it is in the report")
        }
        assertContains(report, "<class name=\"com/example/DatabaseService\"")
    }

    @Test
    fun `a build's own exclusions match anywhere in a name, or a package`(
        @TempDir dir: File,
    ) {
        writeProject(dir, armorConfig = "    coverage { exclusions = mutableListOf(\"Database\", \"model\") }")

        ArmorTestFixture.run(dir, "test")

        val report = coverageReport(dir)
        assertFalse(report.contains("com/example/DatabaseService"))
        assertFalse(report.contains("com/example/model/User"))
        assertContains(report, "<class name=\"com/example/ConfigParser\"")
    }
}
