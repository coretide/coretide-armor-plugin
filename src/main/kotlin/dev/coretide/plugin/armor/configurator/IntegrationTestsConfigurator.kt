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
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.gradle.api.tasks.testing.testng.TestNGOptions

/**
 * An `integrationTest` source set and test task, in `src/integrationTest/java` or `src/integrationTest/kotlin`:
 * tests that run against the production code with the unit tests' dependencies, after the unit tests. Part of the
 * build tier, and of coverage.
 *
 * A plain source set rather than a JVM test suite: a suite adds Gradle's own JUnit version, where these tests
 * should use exactly the libraries, and the runner, the unit tests do.
 */
object IntegrationTestsConfigurator {
    const val TASK_NAME = "integrationTest"

    fun configure(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        if (!extension.integrationTests) return
        project.plugins.withType(JavaPlugin::class.java) {
            val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
            val main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            val integrationTest =
                sourceSets.maybeCreate(TASK_NAME).apply {
                    compileClasspath += main.output
                    runtimeClasspath += main.output
                }
            // testImplementation already extends implementation, so this brings the production dependencies too.
            project.configurations.named(integrationTest.implementationConfigurationName) {
                it.extendsFrom(project.configurations.getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME))
            }
            project.configurations.named(integrationTest.runtimeOnlyConfigurationName) {
                it.extendsFrom(project.configurations.getByName(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME))
            }
            project.tasks.register(TASK_NAME, Test::class.java) { task ->
                task.group = "verification"
                task.description = "🧪 Runs the integration tests, in src/$TASK_NAME"
                task.testClassesDirs = integrationTest.output.classesDirs
                task.classpath = integrationTest.runtimeClasspath
                task.shouldRunAfter(JavaPlugin.TEST_TASK_NAME)
                // The unit tests' runner. A default JUnit 4 runner is switched like theirs, by the test conventions.
                when (project.tasks.named(JavaPlugin.TEST_TASK_NAME, Test::class.java).get().options) {
                    is JUnitPlatformOptions -> task.useJUnitPlatform()
                    is TestNGOptions -> task.useTestNG()
                    else -> Unit
                }
            }
            LogUtil.verbose("🧪 Integration tests: src/$TASK_NAME, run by ./gradlew $TASK_NAME and build")
        }
    }
}
