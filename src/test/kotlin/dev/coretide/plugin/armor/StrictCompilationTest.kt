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

import dev.coretide.plugin.armor.ArmorTestFixture.Language
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** `strictCompilation`: warnings in production code fail the build, for Java and Kotlin. */
class StrictCompilationTest {
    @Test
    fun `a Java warning fails the build only with strictCompilation`(
        @TempDir lenient: File,
        @TempDir strict: File,
    ) {
        listOf(lenient to "", strict to "    strictCompilation = true").forEach { (dir, config) ->
            ArmorTestFixture.writeProject(dir, armorConfig = config)
            writeJavaDeprecationUse(dir.resolve("src/main/java/com/example"))
        }

        ArmorTestFixture.run(lenient, "compileJava")
        val failure = ArmorTestFixture.runAndFail(strict, "compileJava")

        assertContains(failure.output, "[deprecation] Old in com.example has been deprecated")
        assertContains(failure.output, "warnings found and -Werror specified")
    }

    @Test
    fun `strictCompilation leaves Java test code alone`(
        @TempDir dir: File,
    ) {
        // Tests use deprecated APIs on purpose, to test them.
        ArmorTestFixture.writeProject(dir, armorConfig = "    strictCompilation = true")
        writeJavaDeprecationUse(dir.resolve("src/test/java/com/example"))

        ArmorTestFixture.run(dir, "compileTestJava")
    }

    @Test
    fun `a Kotlin warning fails the build only with strictCompilation`(
        @TempDir lenient: File,
        @TempDir strict: File,
    ) {
        listOf(lenient to "", strict to "    strictCompilation = true").forEach { (dir, config) ->
            ArmorTestFixture.writeProject(dir, language = Language.KOTLIN, armorConfig = config)
            dir.resolve("src/main/kotlin/com/example/Old.kt").writeText(
                """
                package com.example

                @Deprecated("Use add")
                public fun old(): Int = 1

                public fun usesOld(): Int = old()
                """.trimIndent(),
            )
        }

        ArmorTestFixture.run(lenient, "compileKotlin")
        val failure = ArmorTestFixture.runAndFail(strict, "compileKotlin")

        assertContains(failure.output, "is deprecated. Use add")
        assertContains(failure.output, "warnings found and -Werror specified")
    }

    @Test
    fun `Kotlin libraries must declare their public API, applications need not`(
        @TempDir library: File,
        @TempDir application: File,
    ) {
        val implicitApi = "package com.example\n\nfun greet() = \"hello\"\n"
        ArmorTestFixture.writeProject(library, language = Language.KOTLIN, armorConfig = "    strictCompilation = true")
        library.resolve("src/main/kotlin/com/example/Greet.kt").writeText(implicitApi)
        ArmorTestFixture.writeProject(
            application,
            language = Language.KOTLIN,
            extraPlugins = listOf("application"),
            armorConfig = "    strictCompilation = true",
        )
        application.resolve("src/main/kotlin/com/example/Greet.kt").writeText(implicitApi)

        val failure = ArmorTestFixture.runAndFail(library, "compileKotlin")
        assertContains(failure.output, "Visibility must be specified in explicit API mode")
        ArmorTestFixture.run(application, "compileKotlin")
    }

    @Test
    fun `Kotlin compiler arguments the build already passes are not repeated`(
        @TempDir dir: File,
    ) {
        // A repeated -X argument is a compiler warning, which strictCompilation would turn into an error.
        ArmorTestFixture.writeProject(
            dir,
            language = Language.KOTLIN,
            armorConfig = "    strictCompilation = true",
            extraScript =
                """
                kotlin {
                    explicitApi()
                    compilerOptions { freeCompilerArgs.add("-Xjsr305=strict") }
                }
                tasks.register("printKotlinArgs") {
                    val compile = tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin")
                    val args = compile.flatMap { it.compilerOptions.freeCompilerArgs }
                    val strict = compile.flatMap { it.compilerOptions.allWarningsAsErrors }
                    doLast {
                        println("ARGS " + args.get())
                        println("WARNINGS_AS_ERRORS " + strict.get())
                    }
                }
                """.trimIndent(),
        )

        val result = ArmorTestFixture.run(dir, "printKotlinArgs", "compileKotlin")

        // The Kotlin plugin itself adds -Xexplicit-api=strict for explicitApi().
        val args = result.output.lineSequence().first { it.startsWith("ARGS ") }.removePrefix("ARGS [").removeSuffix("]").split(", ")
        assertEquals(listOf("-Xexplicit-api=strict", "-Xjsr305=strict"), args.sorted())
        assertContains(result.output, "WARNINGS_AS_ERRORS true")
    }

    @Test
    fun `strictCompilation is off by default`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(
            dir,
            extraScript =
                """
                tasks.register("printJavaArgs") {
                    val args = tasks.compileJava.map { it.options.compilerArgs.toList() }
                    doLast { println("JAVA_ARGS " + args.get()) }
                }
                """.trimIndent(),
        )

        val result = ArmorTestFixture.run(dir, "printJavaArgs")

        assertEquals("JAVA_ARGS []", result.output.lineSequence().first { it.startsWith("JAVA_ARGS ") })
    }

    private fun writeJavaDeprecationUse(dir: File) {
        dir.mkdirs()
        dir.resolve("Old.java").writeText(
            """
            package com.example;

            @Deprecated
            public class Old {
            }
            """.trimIndent(),
        )
        dir.resolve("UsesOld.java").writeText(
            """
            package com.example;

            public class UsesOld {
                public Object make() {
                    return new Old();
                }
            }
            """.trimIndent(),
        )
    }
}
