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
 * The default SpotBugs filter leaves out test and configuration classes by whole name suffix, and the classes nested
 * in them. Before 0.4.0 it left out any class with `Test` or `Config` anywhere in its name.
 */
class SpotbugsFilterTest {
    /** Classes the default filter leaves out. */
    private val excluded = listOf("SampleTest", "SampleTests", "SampleIT", "SampleTestCase", "AppConfig", "WebConfiguration")

    /** Classes whose names only contain `Test` or `Config`, which the pre-0.4.0 filter left out. */
    private val kept = listOf("TestimonialService", "AbTestRouter", "ConfigParser", "ConfigurationLoader")

    /** A method SpotBugs always flags, with high confidence: it calls itself forever. */
    private val recursive = "    public int loop(int value) {\n        return loop(value);\n    }\n"

    @Test
    fun `the default filter leaves out test and configuration classes by name suffix, and nothing else`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir)
        val sources = dir.resolve("src/main/java/com/example")
        (excluded + kept).forEach { name ->
            sources.resolve("$name.java").writeText("package com.example;\n\npublic class $name {\n$recursive}\n")
        }
        // A class nested in a configuration class goes with it.
        sources.resolve("AppConfig.java").writeText(
            "package com.example;\n\npublic class AppConfig {\n    public static class Inner {\n$recursive    }\n}\n",
        )

        ArmorTestFixture.runAndFail(dir, "spotbugsMain")

        val report = dir.resolve("build/reports/spotbugs/spotbugsMain.xml").readText()
        kept.forEach { assertContains(report, "classname=\"com.example.$it\"") }
        (excluded + "AppConfig\$Inner").forEach {
            assertFalse(report.contains("classname=\"com.example.$it\""), "$it was not left out")
        }
    }
}
