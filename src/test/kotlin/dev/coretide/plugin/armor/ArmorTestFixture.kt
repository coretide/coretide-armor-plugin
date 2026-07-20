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
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

/**
 * Builds throwaway Gradle projects on disk and runs the armor plugin against them via TestKit.
 *
 * Every fixture disables git hooks and git-derived versioning by default: the temp directories
 * are not git repositories, and leaving those on makes assertions depend on the host's git state.
 */
object ArmorTestFixture {
    /** Marker so tests can assert on which language plugin a fixture applied. */
    enum class Language(val pluginId: String) {
        JAVA("java"),
        KOTLIN("org.jetbrains.kotlin.jvm"),
    }

    fun writeProject(
        dir: File,
        language: Language = Language.JAVA,
        armorConfig: String = "",
        extraPlugins: List<String> = emptyList(),
        withSource: Boolean = true,
    ) {
        dir.mkdirs()
        writeSettings(dir, "armor-fixture")

        val pluginLines =
            buildList {
                add("""    id("${language.pluginId}")""")
                extraPlugins.forEach { add("""    id("$it")""") }
                add("""    id("dev.coretide.plugin.armor")""")
            }.joinToString("\n")

        dir.resolve("build.gradle.kts").writeText(
            """
            plugins {
            $pluginLines
            }

            repositories {
                mavenCentral()
            }

            codeArmor {
                // Fixtures are not git repositories; these would otherwise shell out to git.
                enableGitHooks = false
                enableVersionFromGit = false
            $armorConfig
            }
            """.trimIndent(),
        )

        if (withSource) {
            writeJavaSource(dir)
        }
    }

    fun writeMultiModuleProject(
        dir: File,
        modules: List<String> = listOf("module-a", "module-b"),
        armorConfig: String = "",
    ) {
        dir.mkdirs()
        writeSettings(dir, "armor-multi-fixture", modules)

        dir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("dev.coretide.plugin.armor")
            }

            codeArmor {
                enableGitHooks = false
                enableVersionFromGit = false
            $armorConfig
            }
            """.trimIndent(),
        )

        modules.forEach { module ->
            val moduleDir = dir.resolve(module)
            moduleDir.mkdirs()
            moduleDir.resolve("build.gradle.kts").writeText(
                """
                plugins {
                    id("java")
                }

                repositories {
                    mavenCentral()
                }
                """.trimIndent(),
            )
            writeJavaSource(moduleDir)
        }
    }

    /**
     * A fixture that looks like a Spring Boot project to [dev.coretide.plugin.armor.util.ProjectDetector]
     * without actually resolving the Boot plugin, which would need network access and a pinned Boot version.
     */
    fun writeSpringBootLikeProject(dir: File) {
        dir.mkdirs()
        writeSettings(dir, "armor-boot-fixture")
        dir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("java")
                id("dev.coretide.plugin.armor")
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                compileOnly("org.springframework.boot:spring-boot-starter-web:3.4.1")
            }

            codeArmor {
                enableGitHooks = false
                enableVersionFromGit = false
            }
            """.trimIndent(),
        )
        writeJavaSource(dir)
    }

    fun run(
        dir: File,
        vararg args: String,
    ): BuildResult = runner(dir, *args).build()

    fun runAndFail(
        dir: File,
        vararg args: String,
    ): BuildResult = runner(dir, *args).buildAndFail()

    private fun runner(
        dir: File,
        vararg args: String,
    ): GradleRunner =
        GradleRunner
            .create()
            .withProjectDir(dir)
            .withPluginClasspath()
            .withArguments(*args, "--stacktrace")
            .forwardOutput()

    private fun writeSettings(
        dir: File,
        name: String,
        modules: List<String> = emptyList(),
    ) {
        val includes = modules.joinToString("\n") { """include("$it")""" }
        dir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "$name"
            $includes
            """.trimIndent(),
        )
    }

    private fun writeJavaSource(dir: File) {
        val src = dir.resolve("src/main/java/com/example")
        src.mkdirs()
        src.resolve("Sample.java").writeText(
            """
            package com.example;

            public class Sample {
                public int add(int a, int b) {
                    return a + b;
                }
            }
            """.trimIndent(),
        )
    }
}
