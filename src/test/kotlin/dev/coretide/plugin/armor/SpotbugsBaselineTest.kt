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

import dev.coretide.plugin.armor.task.SpotbugsBaselineTask
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** `armorSpotbugsBaseline`: SpotBugs findings already in the code are accepted, and only new ones fail the build. */
class SpotbugsBaselineTest {
    /** A class SpotBugs always flags, with high confidence: a method that calls itself forever. */
    private fun writeRecursive(
        dir: File,
        name: String,
        header: String = "",
    ) {
        dir.resolve("src/main/java/com/example/$name.java").writeText(
            "package com.example;\n\n$header" +
                "public class $name {\n    public int loop(int value) {\n        return loop(value);\n    }\n}\n",
        )
    }

    private fun findings(dir: File): String = dir.resolve("build/reports/spotbugs/spotbugsMain.xml").readText()

    @Test
    fun `a baseline accepts the findings already there, and a new one still fails`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir)
        writeRecursive(dir, "Recursive")

        ArmorTestFixture.runAndFail(dir, "spotbugsMain", "--configuration-cache")
        val found = findings(dir)
        val written = ArmorTestFixture.run(dir, "armorSpotbugsBaseline", "--configuration-cache")
        // Lines added above the finding move it; the baseline does not go by line.
        writeRecursive(dir, "Recursive", header = "// Moved down.\n\n")
        val accepted = ArmorTestFixture.run(dir, "spotbugsMain", "armorInfo", "--configuration-cache")
        val acceptedFindings = findings(dir)
        writeRecursive(dir, "AlsoRecursive")
        ArmorTestFixture.runAndFail(dir, "spotbugsMain", "--configuration-cache")
        val added = findings(dir)

        assertContains(found, "IL_INFINITE_RECURSIVE_LOOP")
        assertContains(written.output, "🐛 SpotBugs baseline: 1 findings accepted in")
        assertEquals(1, SpotbugsBaselineTask.count(dir.resolve("config/spotbugs/baseline.xml")))
        assertFalse(acceptedFindings.contains("<BugInstance"), acceptedFindings)
        assertContains(accepted.output, "✅ SpotBugs 4.10.3, with a baseline of 1 accepted findings")
        assertContains(added, "com.example.AlsoRecursive")
        assertFalse(added.contains("classname=\"com.example.Recursive\""), added)
    }

    @Test
    fun `writing the baseline again takes every finding, the accepted ones too`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir)
        writeRecursive(dir, "Recursive")
        ArmorTestFixture.run(dir, "armorSpotbugsBaseline")
        writeRecursive(dir, "AlsoRecursive")

        val rewritten = ArmorTestFixture.run(dir, "armorSpotbugsBaseline")

        assertContains(rewritten.output, "🐛 SpotBugs baseline: 2 findings accepted in")
        ArmorTestFixture.run(dir, "spotbugsMain")
    }

    @Test
    fun `without findings the baseline lists none, and build passes`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir)

        val written = ArmorTestFixture.run(dir, "armorSpotbugsBaseline")

        assertContains(written.output, "🐛 SpotBugs baseline: no findings to accept")
        assertEquals(0, SpotbugsBaselineTask.count(dir.resolve("config/spotbugs/baseline.xml")))
        ArmorTestFixture.run(dir, "spotbugsMain")
    }
}
