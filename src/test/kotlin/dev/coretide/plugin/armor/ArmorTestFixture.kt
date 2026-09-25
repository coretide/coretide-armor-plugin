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
        extraScript: String = "",
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

            $extraScript
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

    /**
     * Runs with this process's environment, minus [unset], plus [set]. TestKit replaces the whole
     * environment when one is given, so the rest has to be passed through explicitly.
     *
     * Windows variable names ignore case, and Java reports PATH there as "Path". A variable is
     * therefore replaced whatever its case, or the build would receive both spellings and either
     * could win.
     */
    fun runWithEnvironment(
        dir: File,
        vararg args: String,
        set: Map<String, String> = emptyMap(),
        unset: Set<String> = emptySet(),
    ): BuildResult {
        val replaced = unset + set.keys
        val inherited = System.getenv().filterKeys { name -> replaced.none { it.equals(name, ignoreCase = isWindows) } }
        return runner(dir, *args)
            .withEnvironment(inherited + set)
            .build()
    }

    private val isWindows = System.getProperty("os.name").startsWith("Windows")

    /**
     * Makes [dir] a git repository with one commit, tagged [tag] if given. Signing is switched off
     * so a developer's global git configuration cannot make the commit or tag prompt or fail.
     */
    fun initGitRepository(
        dir: File,
        tag: String? = null,
    ) {
        git(dir, "init", "--quiet")
        git(dir, "add", "--all")
        git(
            dir,
            "-c", "user.name=Armor Test",
            "-c", "user.email=armor@example.test",
            "-c", "commit.gpgsign=false",
            "commit", "--quiet", "--message", "fixture",
        )
        if (tag != null) {
            git(dir, "-c", "tag.gpgsign=false", "tag", tag)
        }
    }

    /** An empty file standing in for the developer's global git configuration. */
    private val emptyGitConfig: File by lazy {
        File.createTempFile("armor-empty-gitconfig", "").apply { deleteOnExit() }
    }

    /**
     * Environment that hides the machine's global and system git configuration. Without it, a
     * developer's own global `core.hooksPath` or signing setup would change what these tests see.
     * Pass it to [runWithEnvironment] for builds whose tasks call git.
     */
    val isolatedGitEnvironment: Map<String, String>
        get() = mapOf("GIT_CONFIG_GLOBAL" to emptyGitConfig.absolutePath, "GIT_CONFIG_NOSYSTEM" to "1")

    class GitResult(
        val exitCode: Int,
        val output: String,
    )

    /** Runs git in [dir] with [environment] added, returning its exit code and output. */
    fun runGit(
        dir: File,
        vararg args: String,
        environment: Map<String, String> = isolatedGitEnvironment,
    ): GitResult {
        val builder =
            ProcessBuilder(listOf("git") + args)
                .directory(dir)
                .redirectErrorStream(true)
        builder.environment().putAll(environment)
        val process = builder.start()
        val output = process.inputStream.bufferedReader().readText()
        return GitResult(process.waitFor(), output)
    }

    /** Runs git in [dir] and fails the test when git does. */
    fun git(
        dir: File,
        vararg args: String,
        environment: Map<String, String> = isolatedGitEnvironment,
    ): String {
        val result = runGit(dir, *args, environment = environment)
        check(result.exitCode == 0) { "git ${args.joinToString(" ")} failed:\n${result.output}" }
        return result.output
    }

    /**
     * Writes a stand-in `gradlew` into [dir] that records its arguments in `gradlew-calls.txt` and
     * exits with [exitCode]. It lets tests drive a git hook without running a nested Gradle build.
     */
    fun writeFakeGradlew(
        dir: File,
        exitCode: Int,
    ) {
        dir.resolve("gradlew").writeText(
            """
            #!/bin/sh
            printf '%s\n' "${'$'}*" >> gradlew-calls.txt
            exit $exitCode
            """.trimIndent() + "\n",
        )
    }

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
