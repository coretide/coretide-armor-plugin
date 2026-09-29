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
import dev.coretide.plugin.armor.util.ExclusionUtil
import dev.coretide.plugin.armor.util.ExclusionUtil.generateJacocoVerificationExclusions
import dev.coretide.plugin.armor.util.LogUtil
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junit.JUnitOptions
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

object JacocoConfigurator {
    fun configureJacoco(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        project.pluginManager.apply("jacoco")
        project.configure<JacocoPluginExtension> {
            toolVersion = "0.8.15"
        }
        project.afterEvaluate {
            // The JaCoCo plugin only creates its report tasks alongside the Java plugin; looking them
            // up in, say, a docs or aggregator project failed the whole configuration.
            if (!project.plugins.hasPlugin(JavaPlugin::class.java)) return@afterEvaluate
            project.tasks.withType<Test>().configureEach { testTask ->
                if (extension.junitPlatform && usesDefaultRunner(testTask)) {
                    testTask.useJUnitPlatform()
                }
                // Failures in full; passing tests and their output stay quiet.
                testTask.testLogging { logging ->
                    logging.events = logging.events + TestLogEvent.FAILED
                    logging.exceptionFormat = TestExceptionFormat.FULL
                }
                testTask.finalizedBy("jacocoTestReport")
            }
            project.tasks.named("jacocoTestReport", JacocoReport::class.java) { report ->
                report.dependsOn("test")
                report.reports { reports ->
                    reports.xml.required.set(true)
                    reports.html.required.set(true)
                    reports.csv.required.set(false)
                }
                val jacocoExclusions = ExclusionUtil.generateJacocoReportExclusions(extension)
                report.classDirectories.setFrom(
                    project.files(
                        report.classDirectories.files.map { file ->
                            project.fileTree(file).exclude(jacocoExclusions)
                        },
                    ),
                )
                report.doLast {
                    LogUtil.verbose("📊 JaCoCo coverage report generated")
                }
            }
            project.tasks.named("jacocoTestCoverageVerification", JacocoCoverageVerification::class.java) { verification ->
                verification.dependsOn("jacocoTestReport")
                verification.violationRules { rules ->
                    rules.rule { rule ->
                        rule.limit { limit ->
                            limit.minimum = extension.coverageMinimum.toBigDecimal()
                        }
                    }
                    rules.rule { rule ->
                        rule.element = "CLASS"
                        rule.excludes = generateJacocoVerificationExclusions(extension)
                        rule.includes = extension.coverageInclusions
                        rule.limit { limit ->
                            limit.counter = "LINE"
                            limit.minimum = extension.coverageClassMinimum.toBigDecimal()
                        }
                    }
                }
                verification.doLast {
                    LogUtil.verbose("✅ JaCoCo coverage verification completed")
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
