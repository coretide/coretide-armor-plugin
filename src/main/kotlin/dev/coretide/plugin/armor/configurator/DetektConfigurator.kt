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
import dev.coretide.plugin.armor.util.LogUtil
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import java.io.File

/**
 * detekt, the Kotlin static analyser, for projects that apply the Kotlin JVM plugin.
 *
 * The plain `detekt` task, without type resolution, joins the build tier: it needs no compiled classes and
 * runs on the Kotlin compiler detekt ships with, not the project's.
 */
object DetektConfigurator {
    const val PLUGIN_ID = "dev.detekt"
    const val TASK_NAME = "detekt"

    /** detekt with type resolution, for the main source set. */
    const val TYPE_RESOLUTION_TASK = "detektMain"

    /** The detekt task the build tier runs. */
    fun taskName(extension: CodeArmorExtension): String = if (extension.detektTypeResolution) TYPE_RESOLUTION_TASK else TASK_NAME

    /** detekt 1.x. A project that applies it keeps it, and its `detekt` task joins the build tier instead. */
    const val LEGACY_PLUGIN_ID = "io.gitlab.arturbosch.detekt"

    private const val KOTLIN_JVM_PLUGIN_ID = "org.jetbrains.kotlin.jvm"

    /** The detekt release the bundled Gradle plugin runs; DetektVersionTest keeps these in step with it. */
    const val DETEKT_VERSION = "2.0.0-alpha.6"

    /** The Kotlin compiler that detekt release is built on, and so the newest Kotlin it can parse. */
    const val DETEKT_KOTLIN_VERSION = "2.4.10"

    fun configure(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        if (!extension.detekt || !project.plugins.hasPlugin(KOTLIN_JVM_PLUGIN_ID)) return
        if (project.plugins.hasPlugin(LEGACY_PLUGIN_ID)) {
            LogUtil.verbose("🔍 CodeArmor: this project applies detekt 1.x itself; its detekt task joins the build tier")
            return
        }
        val kotlinVersion = kotlinPluginVersion(project)
        if (kotlinVersion != null && !supports(kotlinVersion, DETEKT_KOTLIN_VERSION)) {
            LogUtil.essential(
                "⚠️ CodeArmor: detekt ${DETEKT_VERSION} supports Kotlin up to " +
                    "${majorMinor(DETEKT_KOTLIN_VERSION)}, and this project uses Kotlin $kotlinVersion, " +
                    "so detekt is off. A later CodeArmor release will update it; set detekt = false to hide this.",
            )
            return
        }
        try {
            project.pluginManager.apply(PLUGIN_ID)
        } catch (e: Exception) {
            val missing = missingClass(e) ?: throw e
            // detekt uses the Kotlin plugin's classes, which it can only see when the Kotlin plugin is on the
            // classpath of the build script that applies CodeArmor, or of a parent one.
            LogUtil.essential(
                "⚠️ CodeArmor: detekt cannot see the Kotlin Gradle plugin's classes (${missing.message}), so it is " +
                    "off for ${project.path}. Declare the Kotlin plugin in the build script that applies CodeArmor, " +
                    "for example kotlin(\"jvm\") version \"…\" apply false in the root build script.",
            )
            return
        }
        // A detekt.yml overrides the rules it names, on top of detekt's defaults, instead of replacing them.
        project.extensions.configure(DetektExtension::class.java) { detekt ->
            detekt.buildUponDefaultConfig.convention(true)
        }
        project.tasks.withType(Detekt::class.java).configureEach { task ->
            task.reports { reports ->
                reports.checkstyle.required.set(true)
                reports.html.required.set(true)
                reports.sarif.required.set(true)
                reports.markdown.required.set(false)
            }
        }
        LogUtil.verbose("🔍 detekt ${DETEKT_VERSION} configured")
    }

    /**
     * The class-loading failure behind [failure], if that is what it is: Gradle wraps whatever a plugin
     * throws while it is applied.
     */
    fun missingClass(failure: Throwable): LinkageError? =
        generateSequence(failure) { it.cause.takeIf { cause -> cause !== it } }.filterIsInstance<LinkageError>().firstOrNull()

    /** The checkstyle-format XML report of the `detekt` task, which SonarQube imports. */
    fun checkstyleReport(
        project: Project,
        extension: CodeArmorExtension,
    ): File? {
        if (!project.plugins.hasPlugin(PLUGIN_ID)) return null
        val task = project.tasks.named(taskName(extension), Detekt::class.java).get()
        return task.reports.checkstyle.outputLocation.orNull?.asFile
    }

    /**
     * Whether detekt, built with [detektKotlin], can analyse code for [projectKotlin]: detekt parses with its
     * own Kotlin compiler, which cannot read syntax a newer Kotlin release added.
     */
    fun supports(
        projectKotlin: String,
        detektKotlin: String,
    ): Boolean {
        val project = majorMinorParts(projectKotlin) ?: return true
        val detekt = majorMinorParts(detektKotlin) ?: return true
        return compareValuesBy(project, detekt, { it.first }, { it.second }) <= 0
    }

    /** Through the Kotlin plugin's public interface, looked up by name: CodeArmor does not depend on it. */
    private fun kotlinPluginVersion(project: Project): String? {
        val plugin = project.plugins.findPlugin(KOTLIN_JVM_PLUGIN_ID) ?: return null
        return try {
            val basePlugin = Class.forName("org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin", false, plugin.javaClass.classLoader)
            basePlugin.getMethod("getPluginVersion").invoke(plugin) as? String
        } catch (e: ReflectiveOperationException) {
            null
        } catch (e: LinkageError) {
            null
        }
    }

    private fun majorMinor(version: String): String = majorMinorParts(version)?.let { "${it.first}.${it.second}" } ?: version

    private fun majorMinorParts(version: String): Pair<Int, Int>? {
        val match = Regex("""^(\d+)\.(\d+)""").find(version) ?: return null
        return match.groupValues[1].toInt() to match.groupValues[2].toInt()
    }
}
