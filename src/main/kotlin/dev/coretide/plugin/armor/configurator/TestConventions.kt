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
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junit.JUnitOptions
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

/** The test runner and test logging, set up alongside coverage (JaCoCo or Kover). */
object TestConventions {
    fun configure(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        project.plugins.withType(JavaPlugin::class.java) {
            project.tasks.withType(Test::class.java).configureEach { testTask ->
                if (extension.tests.junitPlatform.get() && usesDefaultRunner(testTask)) {
                    testTask.useJUnitPlatform()
                }
                // Failures in full; passing tests and their output stay quiet.
                testTask.testLogging { logging ->
                    logging.events = logging.events + TestLogEvent.FAILED
                    logging.exceptionFormat = TestExceptionFormat.FULL
                }
            }
        }
    }

    /**
     * Gradle's default runner is JUnit 4. A test task that chose TestNG, or already configured the JUnit
     * Platform (tags, engines), is left as it is.
     */
    private fun usesDefaultRunner(testTask: Test): Boolean = testTask.options is JUnitOptions
}
