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
import dev.coretide.plugin.armor.task.ScaffoldArchitectureTestsTask
import dev.coretide.plugin.armor.util.LogUtil
import dev.coretide.plugin.armor.util.SourcePackages
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

/**
 * Architecture tests with ArchUnit (`tests { architectureTests = true }`), for Java and Kotlin alike: ArchUnit reads
 * bytecode. The rules are ordinary tests, written once by `armorScaffoldArchitectureTests` and then the project's.
 */
object ArchitectureTestsConfigurator {
    const val ARCHUNIT_VERSION = "1.5.1"
    const val SCAFFOLD_TASK = "armorScaffoldArchitectureTests"

    fun configure(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        if (!extension.tests.architectureTests.get()) return
        project.plugins.withType(JavaPlugin::class.java) {
            val version = extension.toolVersions.archUnit.get()
            project.dependencies.add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, "com.tngtech.archunit:archunit-junit5:$version")
            // Gradle 9 no longer supplies the JUnit Platform launcher; JUnit's BOM, which ArchUnit brings, sets its version.
            project.dependencies.add(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME, "org.junit.platform:junit-platform-launcher")
            val mainSources =
                project.extensions
                    .getByType(SourceSetContainer::class.java)
                    .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                    .allSource.srcDirs
            val group = project.group.toString()
            val kotlin = project.plugins.hasPlugin("org.jetbrains.kotlin.jvm")
            project.tasks.register(SCAFFOLD_TASK, ScaffoldArchitectureTestsTask::class.java) { task ->
                task.packages.set(project.provider { packages(mainSources, group) })
                task.kotlin.set(kotlin)
                task.testSourceDirectory.set(project.layout.projectDirectory.dir(if (kotlin) "src/test/kotlin" else "src/test/java"))
            }
            LogUtil.verbose("🏛️ ArchUnit $version added to the tests: ./gradlew $SCAFFOLD_TASK")
        }
    }

    /** The project's top-level packages, from the sources; the group when they declare none. */
    fun packages(
        mainSources: Set<java.io.File>,
        group: String,
    ): List<String> {
        val roots = SourcePackages.roots(SourcePackages.declaredIn(mainSources)).sorted()
        return roots.ifEmpty { listOfNotNull(group.takeIf { it.isNotBlank() }) }
    }
}
