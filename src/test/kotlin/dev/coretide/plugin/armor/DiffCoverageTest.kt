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

import dev.coretide.plugin.armor.task.DiffCoverageTask
import dev.coretide.plugin.armor.util.DiffCoverage
import java.io.File
import java.nio.file.Files
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** `armorDiffCoverage`: coverage of the lines changed since the base branch. */
class DiffCoverageTest {
    private val junit =
        """
        dependencies {
            testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
            testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        }
        """.trimIndent()

    private val git = ArmorTestFixture.isolatedGitEnvironment

    /**
     * A repository whose base branch has the fixture's Sample, and a feature branch that adds a tested method and an
     * untested one. Returns the base branch's name.
     */
    private fun writeChangedProject(
        dir: File,
        armorConfig: String = "",
    ): String {
        ArmorTestFixture.writeProject(dir, armorConfig = armorConfig, extraScript = junit)
        dir.resolve("src/test/java/com/example").mkdirs()
        dir.resolve("src/test/java/com/example/SampleTest.java").writeText(test("assertEquals(3, new Sample().add(1, 2));"))
        ArmorTestFixture.initGitRepository(dir)
        val base = ArmorTestFixture.git(dir, "rev-parse", "--abbrev-ref", "HEAD").trim()
        ArmorTestFixture.git(dir, "checkout", "--quiet", "-b", "feature")
        dir.resolve("src/main/java/com/example/Sample.java").writeText(
            """
            package com.example;

            public class Sample {
                public int add(int a, int b) {
                    return a + b;
                }

                public int twice(int value) {
                    return value * 2;
                }

                public int thrice(int value) {
                    return value * 3;
                }
            }
            """.trimIndent(),
        )
        dir.resolve("src/test/java/com/example/SampleTest.java").writeText(
            test("assertEquals(3, new Sample().add(1, 2));\n        assertEquals(4, new Sample().twice(2));"),
        )
        ArmorTestFixture.git(dir, "add", "--all")
        ArmorTestFixture.git(
            dir,
            "-c", "user.name=Armor Test",
            "-c", "user.email=armor@example.test",
            "-c", "commit.gpgsign=false",
            "commit", "--quiet", "--message", "feature",
        )
        return base
    }

    private fun test(body: String) =
        """
        package com.example;

        import static org.junit.jupiter.api.Assertions.assertEquals;

        import org.junit.jupiter.api.Test;

        class SampleTest {
            @Test
            void works() {
                $body
            }
        }
        """.trimIndent()

    @Test
    fun `build reports the coverage of the changed lines, and names the ones no test ran`(
        @TempDir dir: File,
    ) {
        val base = writeChangedProject(dir, armorConfig = "    coverage { minimum = 0.0; classMinimum = 0.0 }")

        val result = ArmorTestFixture.runWithEnvironment(dir, "build", set = git)

        assertContains(result.output, "📐 Diff coverage: 50.0% of 2 changed lines since $base")
        assertContains(result.output, "src/main/java/com/example/Sample.java: untested lines 13")
        // And in the summary.
        assertContains(result.output, "✅ Diff coverage: 50.0% of 2 changed lines since $base")
    }

    @Test
    fun `diffCoverage minimum fails the build below it`(
        @TempDir dir: File,
    ) {
        writeChangedProject(dir, armorConfig = "    coverage { minimum = 0.0; classMinimum = 0.0 }\n    diffCoverage { minimum = 0.8 }")

        val result = ArmorTestFixture.runAndFailWithEnvironment(dir, "armorDiffCoverage", set = git)

        assertContains(result.output, "Diff coverage 50.0% is below its minimum, 80.0%")
    }

    @Test
    fun `Kover's report works as well as JaCoCo's`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, language = ArmorTestFixture.Language.KOTLIN, armorConfig = "    coverage { kover = true }")
        ArmorTestFixture.initGitRepository(dir)
        val base = ArmorTestFixture.git(dir, "rev-parse", "--abbrev-ref", "HEAD").trim()
        val sample = dir.resolve("src/main/kotlin/com/example/Sample.kt")
        // Uncommitted: diff coverage compares the working tree with the base branch.
        sample.writeText(sample.readText().trimEnd().removeSuffix("}") + "\n    public fun twice(value: Int): Int = value * 2\n}\n")

        val result = ArmorTestFixture.runWithEnvironment(dir, "armorDiffCoverage", set = git + ("GITHUB_BASE_REF" to base))

        assertContains(result.output, "📐 Diff coverage: 0.0% of 1 changed lines since $base")
        assertContains(result.output, "src/main/kotlin/com/example/Sample.kt: untested lines 9")
    }

    @Test
    fun `without a git repository it says so and passes`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, armorConfig = "    diffCoverage { minimum = 0.8 }", extraScript = junit)
        dir.resolve("src/test/java/com/example").mkdirs()
        dir.resolve("src/test/java/com/example/SampleTest.java").writeText(test("assertEquals(3, new Sample().add(1, 2));"))

        val result = ArmorTestFixture.runWithEnvironment(dir, "armorDiffCoverage", set = git)

        assertContains(result.output, "Diff coverage skipped: not a git repository")
    }

    @Test
    fun `a diff's added and changed lines, by file`() {
        val diff =
            """
            diff --git a/src/A.java b/src/A.java
            --- a/src/A.java
            +++ b/src/A.java
            @@ -3 +3 @@ class A {
            -old
            +new
            @@ -10,0 +11,3 @@ class A {
            +one
            +two
            +three
            @@ -20,2 +22,0 @@ class A {
            -gone
            -gone
            diff --git a/src/Gone.java b/src/Gone.java
            deleted file mode 100644
            --- a/src/Gone.java
            +++ /dev/null
            @@ -1,2 +0,0 @@
            -a
            -b
            diff --git a/src/Old.java b/src/Renamed.java
            similarity index 100%
            rename from src/Old.java
            rename to src/Renamed.java
            diff --git a/src/Moved.java b/src/Edited.java
            similarity index 90%
            rename from src/Moved.java
            rename to src/Edited.java
            --- a/src/Moved.java
            +++ b/src/Edited.java
            @@ -7 +7 @@
            -was
            +is
            diff --git a/src/With Space.java b/src/With Space.java
            --- a/src/With Space.java${"\t"}
            +++ b/src/With Space.java${"\t"}
            @@ -0,0 +1 @@
            +x
            diff --git "a/src/Caf\303\251.java" "b/src/Caf\303\251.java"
            --- "a/src/Caf\303\251.java"
            +++ "b/src/Caf\303\251.java"
            @@ -1 +1,2 @@
            +y
            """.trimIndent()

        val changed = DiffCoverage.changedLines(diff)

        assertEquals(
            mapOf(
                "src/A.java" to setOf(3, 11, 12, 13),
                // A rename counts only the lines it changed.
                "src/Edited.java" to setOf(7),
                "src/With Space.java" to setOf(1),
                "src/Café.java" to setOf(1, 2),
            ),
            changed,
        )
    }

    @Test
    fun `only lines with code in files the report covers count`(
        @TempDir dir: File,
    ) {
        val report = dir.resolve("jacoco.xml")
        report.writeText(
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <!DOCTYPE report PUBLIC "-//JACOCO//DTD Report 1.1//EN" "report.dtd">
            <report name="x">
              <package name="com/example">
                <sourcefile name="A.java"><line nr="5" mi="0" ci="3" mb="0" cb="0"/><line nr="6" mi="2" ci="0" mb="0" cb="0"/></sourcefile>
                <sourcefile name="Util.kt"><line nr="3" mi="1" ci="0" mb="0" cb="0"/></sourcefile>
              </package>
            </report>
            """.trimIndent(),
        )
        val sources = listOf(dir.resolve("src/main/java"), dir.resolve("src/main/kotlin"))
        val changed =
            mapOf(
                "src/main/java/com/example/A.java" to setOf(4, 5, 6),
                "src/main/kotlin/util/Util.kt" to setOf(3),
                "src/test/java/com/example/ATest.java" to setOf(1),
                "README.md" to setOf(1),
            )

        val files = DiffCoverage.measure(changed, dir, sources, DiffCoverage.lineCoverage(report))

        assertEquals(
            listOf(
                DiffCoverage.FileCoverage("src/main/java/com/example/A.java", 2, listOf(6)),
                // A Kotlin file in a directory that does not match its package, found by name.
                DiffCoverage.FileCoverage("src/main/kotlin/util/Util.kt", 1, listOf(3)),
            ),
            files,
        )
    }

    @Test
    fun `a source directory behind a symbolic link still matches`(
        @TempDir dir: File,
    ) {
        // As on macOS, where git reports /private/var for a build under /var.
        val real = dir.resolve("real").apply { resolve("src/main/java/com/example").mkdirs() }
        val link = dir.resolve("link")
        val linked = runCatching { Files.createSymbolicLink(link.toPath(), real.toPath()) }.isSuccess
        assumeTrue(linked, "symbolic links are not available here")

        val files =
            DiffCoverage.measure(
                mapOf("src/main/java/com/example/A.java" to setOf(5)),
                real,
                listOf(link.resolve("src/main/java")),
                mapOf("com/example/A.java" to mapOf(5 to true)),
            )

        assertEquals(listOf(DiffCoverage.FileCoverage("src/main/java/com/example/A.java", 1, emptyList())), files)
    }

    @Test
    fun `the base branch comes from the CI's pull request target`() {
        assertEquals("main", DiffCoverageTask.ciTargetBranch(mapOf("GITHUB_BASE_REF" to "main")))
        assertEquals("develop", DiffCoverageTask.ciTargetBranch(mapOf("SYSTEM_PULLREQUEST_TARGETBRANCH" to "refs/heads/develop")))
        assertEquals(null, DiffCoverageTask.ciTargetBranch(mapOf("GITHUB_BASE_REF" to "")))
        assertFalse(DiffCoverageTask.ciTargetBranch(emptyMap()) != null)
    }
}
