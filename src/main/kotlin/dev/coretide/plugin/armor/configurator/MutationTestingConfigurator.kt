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
import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import java.io.File

/**
 * Mutation testing with PIT (`mutationTesting = true`): `./gradlew pitest` changes the production code in
 * small ways and reports which changes no test notices. It runs on demand, not in a check tier: it is slow.
 */
object MutationTestingConfigurator {
    const val PLUGIN_ID = "info.solidsoft.pitest"
    const val TASK_NAME = "pitest"
    const val PITEST_VERSION = "1.30.0"
    const val JUNIT5_PLUGIN_VERSION = "1.2.3"

    fun configure(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        if (!extension.mutationTesting || !project.plugins.hasPlugin(JavaPlugin::class.java)) return
        project.pluginManager.apply(PLUGIN_ID)
        // The JUnit 5 plugin only works when the tests run on the JUnit Platform. Looked up lazily: CodeArmor's
        // own switch to the JUnit Platform happens later in configuration.
        val testTask = project.tasks.named(JavaPlugin.TEST_TASK_NAME, Test::class.java)
        val junit5Plugin = testTask.map { if (it.options is JUnitPlatformOptions) JUNIT5_PLUGIN_VERSION else null }
        val group = project.group.toString()
        val mainSources =
            project.extensions
                .getByType(SourceSetContainer::class.java)
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                .allSource.srcDirs
        project.extensions.configure(PitestPluginExtension::class.java) { pitest ->
            pitest.pitestVersion.set(PITEST_VERSION)
            pitest.junit5PluginVersion.set(junit5Plugin)
            pitest.targetClasses.set(project.provider { targetClasses(mainSources, group) })
            pitest.threads.set(maxOf(1, Runtime.getRuntime().availableProcessors() / 2))
            pitest.outputFormats.set(setOf("XML", "HTML"))
            pitest.timestampedReports.set(false)
            pitest.failWhenNoMutations.set(false)
            pitest.mutationThreshold.set(extension.mutationThreshold)
        }
        LogUtil.verbose("🧬 PIT mutation testing configured: ./gradlew pitest")
    }

    /**
     * The project's own packages, as PIT class patterns. PIT refuses a pattern that matches everything, since it
     * would mutate PIT itself. Without any package declarations, the group stands in.
     */
    fun targetClasses(
        mainSources: Set<File>,
        group: String,
    ): Set<String> {
        val packages = SourcePackages.roots(SourcePackages.declaredIn(mainSources))
        return when {
            packages.isNotEmpty() -> packages.map { "$it.*" }.toSet()
            group.isNotBlank() -> setOf("$group.*")
            else -> emptySet()
        }
    }
}
