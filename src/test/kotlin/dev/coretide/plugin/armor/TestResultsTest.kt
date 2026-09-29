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

import dev.coretide.plugin.armor.util.TestResults
import java.io.File
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Reading flaky and slow tests out of JUnit XML results. */
class TestResultsTest {
    @Test
    fun `a test that failed and then passed is flaky, one that kept failing is not`(
        @TempDir dir: File,
    ) {
        dir.resolve("TEST-com.example.SampleTest.xml").writeText(
            """
            <testsuite name="com.example.SampleTest">
              <testcase name="flaky()" classname="com.example.SampleTest" time="0.01"><failure message="boom"/></testcase>
              <testcase name="flaky()" classname="com.example.SampleTest" time="0.01"/>
              <testcase name="broken()" classname="com.example.SampleTest" time="0.01"><failure message="boom"/></testcase>
              <testcase name="broken()" classname="com.example.SampleTest" time="0.01"><failure message="boom"/></testcase>
              <testcase name="fine()" classname="com.example.SampleTest" time="0.01"/>
            </testsuite>
            """.trimIndent(),
        )

        assertEquals(listOf("com.example.SampleTest > flaky()"), TestResults.flaky(TestResults.read(dir)))
    }

    @Test
    fun `slow tests are those over the threshold, slowest first`(
        @TempDir dir: File,
    ) {
        dir.resolve("TEST-com.example.SlowTest.xml").writeText(
            """
            <testsuite name="com.example.SlowTest">
              <testcase name="quick()" classname="com.example.SlowTest" time="0.5"/>
              <testcase name="slow()" classname="com.example.SlowTest" time="2.5"/>
              <testcase name="slower()" classname="com.example.SlowTest" time="4"/>
              <testcase name="slowButFailed()" classname="com.example.SlowTest" time="9"><error message="x"/></testcase>
            </testsuite>
            """.trimIndent(),
        )

        val slow = TestResults.slow(TestResults.read(dir), thresholdMillis = 2000)

        assertEquals(listOf("com.example.SlowTest > slower()" to 4000L, "com.example.SlowTest > slow()" to 2500L), slow.map { it.id to it.millis })
    }

    @Test
    fun `a missing results directory reads as no tests`(
        @TempDir dir: File,
    ) {
        assertEquals(emptyList(), TestResults.read(dir.resolve("missing")))
    }
}
