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

import dev.coretide.plugin.armor.configurator.ErrorProneConfigurator
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** `errorProne` and `nullAway`: Error Prone and NullAway on the Java sources, both opt-in. */
class ErrorProneTest {
    @Test
    fun `an Error Prone error fails the build only with errorProne`(
        @TempDir lenient: File,
        @TempDir checked: File,
    ) {
        listOf(lenient to "", checked to "    compilation { errorProne = true }").forEach { (dir, config) ->
            ArmorTestFixture.writeProject(dir, armorConfig = config)
            writeDeadException(dir)
        }

        val passed = ArmorTestFixture.run(lenient, "compileJava")
        val failure = ArmorTestFixture.runAndFail(checked, "compileJava")

        assertFalse(passed.output.contains("[DeadException]"))
        assertContains(failure.output, "[DeadException]")
    }

    @Test
    fun `Error Prone warnings fail the build only together with strictCompilation`(
        @TempDir lenient: File,
        @TempDir strict: File,
    ) {
        listOf(lenient to "", strict to "\n    compilation { strict = true }").forEach { (dir, config) ->
            ArmorTestFixture.writeProject(dir, armorConfig = "    compilation { errorProne = true }$config")
            dir.resolve("src/main/java/com/example/Named.java").writeText(
                """
                package com.example;

                public class Named {
                    public String toString() {
                        return "named";
                    }
                }
                """.trimIndent(),
            )
        }

        val warned = ArmorTestFixture.run(lenient, "compileJava")
        val failure = ArmorTestFixture.runAndFail(strict, "compileJava")

        assertContains(warned.output, "[MissingOverride]")
        assertContains(failure.output, "[MissingOverride]")
        assertContains(failure.output, "warnings found and -Werror specified")
    }

    @Test
    fun `NullAway fails production code that returns null undeclared, not annotated code or tests`(
        @TempDir careless: File,
        @TempDir careful: File,
    ) {
        listOf(careless, careful).forEach { ArmorTestFixture.writeProject(it, armorConfig = "    compilation { nullAway = true }") }
        careless.resolve("src/main/java/com/example/Lookup.java").writeText(
            """
            package com.example;

            public class Lookup {
                public String find(String key) {
                    return key.isEmpty() ? null : key;
                }
            }
            """.trimIndent(),
        )
        // NullAway believes any annotation called Nullable.
        careful.resolve("src/main/java/com/example/Nullable.java").writeText(
            """
            package com.example;

            public @interface Nullable {}
            """.trimIndent(),
        )
        careful.resolve("src/main/java/com/example/Lookup.java").writeText(
            """
            package com.example;

            public class Lookup {
                public @Nullable String find(String key) {
                    return key.isEmpty() ? null : key;
                }
            }
            """.trimIndent(),
        )
        careful.resolve("src/test/java/com/example").mkdirs()
        careful.resolve("src/test/java/com/example/Fixtures.java").writeText(
            """
            package com.example;

            class Fixtures {
                static String missing() {
                    return null;
                }
            }
            """.trimIndent(),
        )

        val failure = ArmorTestFixture.runAndFail(careless, "compileJava")
        ArmorTestFixture.run(careful, "compileJava", "compileTestJava")

        assertContains(failure.output, "[NullAway] returning @Nullable expression from method with @NonNull return type")
    }

    @Test
    fun `below JDK 21 the compiler runs without Error Prone and says so`(
        @TempDir dir: File,
    ) {
        val jdk17 = findJdk17()
        assumeTrue(jdk17 != null, "No JDK 17 installation found")

        ArmorTestFixture.writeProject(
            dir,
            armorConfig = "    compilation { errorProne = true }",
            extraScript =
                """
                java {
                    toolchain {
                        languageVersion = JavaLanguageVersion.of(17)
                    }
                }
                """.trimIndent(),
        )
        dir.resolve("gradle.properties").writeText(
            """
            org.gradle.java.installations.auto-detect=false
            org.gradle.java.installations.auto-download=false
            org.gradle.java.installations.paths=${jdk17!!.absolutePath.replace("\\", "/")}
            """.trimIndent(),
        )
        writeDeadException(dir)

        val result = ArmorTestFixture.run(dir, "compileJava")

        assertContains(
            result.output,
            "Error Prone needs a JDK 21 compiler, and :compileJava uses JDK 17. It compiled without Error Prone.",
        )
    }

    @Test
    fun `NullAway checks the project's packages, or only NullMarked code without any`() {
        assertEquals(
            mapOf("NullAway:AnnotatedPackages" to "com.example,org.acme", "NullAway:TreatGeneratedAsUnannotated" to "true"),
            ErrorProneConfigurator.nullAwayOptions(setOf("org.acme", "com.example")),
        )
        assertEquals(
            mapOf("NullAway:OnlyNullMarked" to "true", "NullAway:TreatGeneratedAsUnannotated" to "true"),
            ErrorProneConfigurator.nullAwayOptions(emptySet()),
        )
    }

    /** A JDK 17 to compile with: the one GitHub's runners advertise, or a Linux distribution's. */
    private fun findJdk17(): File? {
        System.getenv("JAVA_HOME_17_X64")?.let { home -> File(home).takeIf { it.isDirectory }?.let { return it } }
        return File("/usr/lib/jvm")
            .listFiles()
            ?.filter { it.isDirectory && Regex("""(^|\D)17(\D|$)""").containsMatchIn(it.name) }
            ?.firstOrNull { it.resolve("bin/javac").isFile }
    }

    /** An exception created and dropped, which Error Prone reports as an error. */
    private fun writeDeadException(dir: File) {
        dir.resolve("src/main/java/com/example/Checks.java").writeText(
            """
            package com.example;

            public class Checks {
                public void requirePositive(int value) {
                    if (value < 0) {
                        new IllegalArgumentException("negative");
                    }
                }
            }
            """.trimIndent(),
        )
    }
}
