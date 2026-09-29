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
import dev.coretide.plugin.armor.util.SourcePackages
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.ErrorProneOptions
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaToolchainService

/**
 * Error Prone (`errorProne = true`) checks the Java sources as they compile, and NullAway (`nullAway = true`),
 * one of its checks, fails the build where production code may dereference null. Kotlin sources are not
 * checked: Error Prone is a javac plugin.
 */
object ErrorProneConfigurator {
    const val PLUGIN_ID = "net.ltgt.errorprone"
    const val ERROR_PRONE_VERSION = "2.50.0"
    const val NULLAWAY_VERSION = "0.14.2"

    /** Error Prone 2.43 and later only run in a JDK 21 or newer compiler. */
    const val MINIMUM_JDK = 21

    private const val CONFIGURATION = "errorprone"

    fun configure(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        if (!extension.errorProne && !extension.nullAway) return
        project.plugins.withType(JavaPlugin::class.java) {
            project.pluginManager.apply(PLUGIN_ID)
            addDependencies(project, extension.nullAway)
            val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
            val mainSources = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).java.srcDirs
            val nullAwayOptions = project.provider { nullAwayOptions(SourcePackages.roots(SourcePackages.declaredIn(mainSources))) }
            // Only the tasks the Error Prone plugin itself enables: those of source sets.
            sourceSets.configureEach { sourceSet ->
                val main = sourceSet.name == SourceSet.MAIN_SOURCE_SET_NAME
                project.tasks.named(sourceSet.compileJavaTaskName, JavaCompile::class.java).configure { task ->
                    configureTask(task, if (extension.nullAway && main) nullAwayOptions else null, extension.nullAway)
                }
            }
            LogUtil.verbose(
                "🐞 Error Prone $ERROR_PRONE_VERSION configured" + if (extension.nullAway) " with NullAway $NULLAWAY_VERSION" else "",
            )
        }
    }

    /**
     * Error Prone and NullAway go on the processor path only when the project's compiler can load them: javac
     * loads every plugin on that path, even with Error Prone switched off, and fails on classes built for a newer JDK.
     */
    private fun addDependencies(
        project: Project,
        nullAway: Boolean,
    ) {
        val toolchain = project.extensions.getByType(JavaPluginExtension::class.java).toolchain
        val supported =
            project.extensions
                .getByType(JavaToolchainService::class.java)
                .compilerFor(toolchain)
                .map { it.metadata.languageVersion.canCompileOrRun(MINIMUM_JDK) }
                .orElse(true)
        val dependencies =
            listOfNotNull(
                "com.google.errorprone:error_prone_core:$ERROR_PRONE_VERSION",
                "com.uber.nullaway:nullaway:$NULLAWAY_VERSION".takeIf { nullAway },
            ).map { project.dependencies.create(it) }
        project.configurations.named(CONFIGURATION) { configuration ->
            configuration.dependencies.addAllLater(supported.map { if (it) dependencies else emptyList() })
        }
    }

    /**
     * [nullAwayOptions] is set for the task NullAway checks, the main one; tests may pass null on purpose.
     * [nullAwayOn] says whether NullAway is on the processor path at all, so it can be switched off elsewhere.
     */
    private fun configureTask(
        task: JavaCompile,
        nullAwayOptions: Provider<Map<String, String>>?,
        nullAwayOn: Boolean,
    ) {
        val options = (task.options as ExtensionAware).extensions.getByType(ErrorProneOptions::class.java)
        // The Error Prone plugin only switches itself off below JDK 11; this Error Prone needs 21.
        val supported = task.javaCompiler.map { it.metadata.languageVersion.canCompileOrRun(MINIMUM_JDK) }.orElse(true)
        options.enabled.convention(supported)
        options.disableWarningsInGeneratedCode.set(true)
        options.excludedPaths.convention(".*/build/generated/.*")
        if (nullAwayOptions != null) {
            options.check("NullAway", CheckSeverity.ERROR)
            options.checkOptions.putAll(nullAwayOptions)
        } else if (nullAwayOn) {
            options.disable("NullAway")
        }
        val compiler = task.javaCompiler.map { it.metadata.languageVersion.toString() }.orElse("?")
        task.doFirst {
            if (!supported.get()) {
                it.logger.warn(
                    "⚠️ CodeArmor: Error Prone needs a JDK $MINIMUM_JDK compiler, and ${it.path} uses JDK ${compiler.get()}. " +
                        "It compiled without Error Prone.",
                )
            }
        }
    }

    /**
     * NullAway checks the project's own packages. Without any package declarations to go by, it checks only
     * code annotated `@NullMarked`. Generated code is treated as unannotated.
     */
    fun nullAwayOptions(packages: Set<String>): Map<String, String> =
        buildMap {
            if (packages.isEmpty()) {
                put("NullAway:OnlyNullMarked", "true")
            } else {
                put("NullAway:AnnotatedPackages", packages.sorted().joinToString(","))
            }
            put("NullAway:TreatGeneratedAsUnannotated", "true")
        }
}
