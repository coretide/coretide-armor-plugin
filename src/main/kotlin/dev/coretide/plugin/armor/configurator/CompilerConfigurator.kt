/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.configurator

import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.ProjectType
import dev.coretide.plugin.armor.util.LogUtil
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.compile.JavaCompile

/**
 * `strictCompilation`: compiler warnings in production code fail the build.
 *
 * Only the main source set: tests often use deprecated APIs on purpose, to test them.
 */
object CompilerConfigurator {
    /**
     * Every lint category except those that flag the build setup rather than the code: annotation
     * processors that claim nothing, missing serialVersionUIDs, bad classpath entries (a dependency's
     * manifest), and obsolete -source/-target values.
     */
    val JAVA_ARGS = listOf("-Xlint:all", "-Xlint:-processing", "-Xlint:-serial", "-Xlint:-path", "-Xlint:-options", "-Werror")

    /** Nullability annotations on Java APIs (JSR-305, Spring's) become Kotlin types instead of platform types. */
    const val KOTLIN_JSR305 = "-Xjsr305=strict"

    /** Libraries must declare the visibility and return type of their public API. */
    const val KOTLIN_EXPLICIT_API = "-Xexplicit-api=strict"

    fun configure(
        project: Project,
        extension: CodeArmorExtension,
        projectType: ProjectType,
    ) {
        if (!extension.strictCompilation) return
        project.plugins.withType(JavaPlugin::class.java) {
            project.tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME, JavaCompile::class.java).configure { task ->
                val args = task.options.compilerArgs
                JAVA_ARGS.filterNot { it in args }.forEach { args.add(it) }
            }
        }
        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            val explicitApi = !projectType.isApplication && !declaresExplicitApi(project)
            project.tasks.named("compileKotlin").configure { task ->
                configureKotlin(task, if (explicitApi) listOf(KOTLIN_JSR305, KOTLIN_EXPLICIT_API) else listOf(KOTLIN_JSR305))
            }
        }
    }

    /**
     * Through the Kotlin Gradle plugin's public interfaces, looked up by name: CodeArmor has no compile-time
     * dependency on it, and its classes are not visible from CodeArmor's class loader when the two plugins
     * are applied in different build scripts.
     */
    private fun configureKotlin(
        task: Task,
        freeArgs: List<String>,
    ) {
        try {
            val loader = task.javaClass.classLoader
            val taskType = Class.forName("org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask", false, loader)
            val optionsType = Class.forName("org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerToolOptions", false, loader)
            val options = taskType.getMethod("getCompilerOptions").invoke(task)

            @Suppress("UNCHECKED_CAST")
            val warningsAsErrors = optionsType.getMethod("getAllWarningsAsErrors").invoke(options) as Property<Boolean>

            @Suppress("UNCHECKED_CAST")
            val args = optionsType.getMethod("getFreeCompilerArgs").invoke(options) as ListProperty<String>
            warningsAsErrors.set(true)
            // A repeated -X argument is itself a compiler warning, which would now fail the build.
            val present = args.get().map { it.substringBefore('=') }
            args.addAll(freeArgs.filterNot { it.substringBefore('=') in present })
        } catch (e: ReflectiveOperationException) {
            warnKotlinUnsupported(task, e)
        } catch (e: LinkageError) {
            warnKotlinUnsupported(task, e)
        }
    }

    /** `kotlin { explicitApi() }` already passes -Xexplicit-api; passing it twice is a warning. */
    private fun declaresExplicitApi(project: Project): Boolean {
        val kotlin = project.extensions.findByName("kotlin") ?: return false
        return try {
            val configType = Class.forName("org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtensionConfig", false, kotlin.javaClass.classLoader)
            val mode = configType.getMethod("getExplicitApi").invoke(kotlin)
            mode != null && mode.toString() != "Disabled"
        } catch (e: ReflectiveOperationException) {
            false
        } catch (e: LinkageError) {
            false
        }
    }

    private fun warnKotlinUnsupported(
        task: Task,
        cause: Throwable,
    ) {
        LogUtil.essential(
            "⚠️ CodeArmor: strictCompilation could not configure ${task.path} " +
                "(${cause.javaClass.simpleName}: ${cause.message}); this Kotlin Gradle plugin version is not supported.",
        )
    }
}
