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

import dev.coretide.plugin.armor.configurator.MutationTestingConfigurator
import dev.coretide.plugin.armor.util.SourcePackages
import java.io.File
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** The packages PIT mutates: the project's own, never a pattern that matches everything. */
class SourcePackagesTest {
    @Test
    fun `the packages Java and Kotlin files declare are read`(
        @TempDir dir: File,
    ) {
        write(dir, "java/com/example/A.java", "// licence header\npackage com.example;\n\nclass A {}\n")
        write(dir, "kotlin/com/example/util/B.kt", "@file:JvmName(\"B\")\n\npackage com.example.util\n\nclass B\n")
        write(dir, "kotlin/NoPackage.kt", "class NoPackage\n")

        assertEquals(setOf("com.example", "com.example.util"), SourcePackages.declaredIn(listOf(dir.resolve("java"), dir.resolve("kotlin"))))
    }

    @Test
    fun `only the outermost packages are kept`() {
        assertEquals(
            setOf("com.example", "org.acme"),
            SourcePackages.roots(setOf("com.example", "com.example.util", "com.example.web.api", "org.acme")),
        )
    }

    @Test
    fun `PIT targets the project's packages, then its group, never everything`(
        @TempDir dir: File,
    ) {
        write(dir, "com/example/A.java", "package com.example;\nclass A {}\n")

        assertEquals(setOf("com.example.*"), MutationTestingConfigurator.targetClasses(setOf(dir), group = "ignored"))
        assertEquals(setOf("org.acme.*"), MutationTestingConfigurator.targetClasses(setOf(dir.resolve("missing")), group = "org.acme"))
        assertEquals(emptySet(), MutationTestingConfigurator.targetClasses(setOf(dir.resolve("missing")), group = ""))
    }

    private fun write(
        dir: File,
        path: String,
        content: String,
    ) {
        dir.resolve(path).apply { parentFile.mkdirs() }.writeText(content)
    }
}
