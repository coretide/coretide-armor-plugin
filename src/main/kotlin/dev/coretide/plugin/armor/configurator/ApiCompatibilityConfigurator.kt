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
import me.champeau.gradle.japicmp.JapicmpTask
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar

/**
 * Library API checks, both opt-in and for libraries only: an application has no API to keep.
 *
 * - `apiBaseline`: japicmp compares the jar with a released version and fails on binary incompatible changes.
 *   It works on bytecode, so for Java and Kotlin alike.
 * - `kotlinAbiValidation`: the Kotlin Gradle plugin's own ABI validation compares a Kotlin library's public
 *   API with a dump committed under `api/`.
 */
object ApiCompatibilityConfigurator {
    const val API_CHECK_TASK = "armorApiCheck"

    /** The Kotlin Gradle plugin's ABI check and update tasks, Kotlin 2.2 to 2.4. */
    const val KOTLIN_ABI_CHECK_TASK = "checkLegacyAbi"
    const val KOTLIN_ABI_UPDATE_TASK = "updateLegacyAbi"

    fun configure(
        project: Project,
        extension: CodeArmorExtension,
        projectType: ProjectType,
    ) {
        if (projectType.isApplication) return
        extension.apiBaseline?.takeIf { it.isNotBlank() }?.let { configureJapicmp(project, it) }
        if (extension.kotlinAbiValidation) configureKotlinAbi(project)
    }

    /** `1.4.0`, or `group:name:1.4.0` when the published coordinates differ from the project's. */
    fun baselineCoordinates(
        baseline: String,
        group: String,
        name: String,
    ): String = if (baseline.count { it == ':' } == 2) baseline else "$group:$name:$baseline"

    private fun configureJapicmp(
        project: Project,
        baseline: String,
    ) {
        project.plugins.withType(JavaPlugin::class.java) {
            val coordinates = baselineCoordinates(baseline, project.group.toString(), project.name)
            val (group, module) = coordinates.split(":")
            val objects = project.objects
            // Detached: in one of the project's own configurations, its own coordinates would resolve to the project.
            val baselineClasspath = project.configurations.detachedConfiguration(project.dependencies.create(coordinates))
            baselineClasspath.description = "The released version the API check compares with"
            baselineClasspath.attributes { attributes ->
                attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.LIBRARY))
                attributes.attribute(
                    LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                    objects.named(LibraryElements::class.java, LibraryElements.JAR),
                )
                attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class.java, Bundling.EXTERNAL))
            }
            val baselineJar =
                baselineClasspath.incoming
                    .artifactView { view ->
                        view.componentFilter { id -> id is ModuleComponentIdentifier && id.group == group && id.module == module }
                    }.files
            val jar = project.tasks.named(JavaPlugin.JAR_TASK_NAME, Jar::class.java)
            project.tasks.register(API_CHECK_TASK, JapicmpTask::class.java) { task ->
                task.group = "verification"
                task.description = "🔌 Fails on binary incompatible changes since $coordinates"
                task.oldClasspath.from(baselineClasspath)
                task.oldArchives.from(baselineJar)
                task.newClasspath.from(jar, project.configurations.named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME))
                task.newArchives.from(jar)
                task.onlyBinaryIncompatibleModified.set(true)
                task.failOnModification.set(true)
                task.ignoreMissingClasses.set(true)
                task.htmlOutputFile.set(project.layout.buildDirectory.file("reports/japicmp/api.html"))
                task.txtOutputFile.set(project.layout.buildDirectory.file("reports/japicmp/api.txt"))
            }
            LogUtil.verbose("🔌 API check against $coordinates configured: ./gradlew $API_CHECK_TASK")
        }
    }

    /**
     * Switches on the Kotlin Gradle plugin's ABI validation (Kotlin 2.2 and later), through its public interfaces
     * looked up by name: CodeArmor has no compile-time dependency on the Kotlin Gradle plugin, and its classes are not
     * visible from CodeArmor's class loader when the two are applied in different build scripts.
     */
    private fun configureKotlinAbi(project: Project) {
        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            val abiValidation = (project.extensions.findByName("kotlin") as? ExtensionAware)?.extensions?.findByName("abiValidation")
            if (abiValidation == null) {
                LogUtil.essential("⚠️ CodeArmor: kotlinAbiValidation needs Kotlin 2.2 or later; ${project.path} is not checked")
                return@withId
            }
            try {
                val loader = abiValidation.javaClass.classLoader
                val extensionType = Class.forName("org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension", false, loader)
                val legacyDumpType = Class.forName("org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationLegacyDumpExtension", false, loader)

                @Suppress("UNCHECKED_CAST")
                val enabled = extensionType.getMethod("getEnabled").invoke(abiValidation) as Property<Boolean>
                enabled.set(true)
                // The build tier names the check task; say so if a Kotlin release renames it.
                val legacyDump = extensionType.getMethod("getLegacyDump").invoke(abiValidation)
                val check = (legacyDumpType.getMethod("getLegacyCheckTaskProvider").invoke(legacyDump) as TaskProvider<*>).name
                if (check != KOTLIN_ABI_CHECK_TASK) {
                    LogUtil.essential("⚠️ CodeArmor: this Kotlin's ABI check is $check; add it to checks.build in ${project.path}")
                }
                LogUtil.verbose("🔌 Kotlin ABI validation switched on for ${project.path}: ./gradlew $KOTLIN_ABI_UPDATE_TASK")
            } catch (e: ReflectiveOperationException) {
                warnKotlinAbiUnsupported(project, e)
            } catch (e: LinkageError) {
                warnKotlinAbiUnsupported(project, e)
            }
        }
    }

    private fun warnKotlinAbiUnsupported(
        project: Project,
        e: Throwable,
    ) {
        LogUtil.essential("⚠️ CodeArmor: kotlinAbiValidation could not switch on this Kotlin's ABI validation in ${project.path}: $e")
    }
}
