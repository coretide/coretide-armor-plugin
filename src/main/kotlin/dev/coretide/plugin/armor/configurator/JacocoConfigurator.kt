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
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoReportBase

object JacocoConfigurator {
    const val TOOL_VERSION = "0.8.15"

    /**
     * The integration tests' coverage too, when they run in the same build; a report or check run on its own
     * reads whatever execution data exists.
     */
    private fun includeIntegrationTests(
        project: Project,
        extension: CodeArmorExtension,
        task: JacocoReportBase,
    ) {
        if (!extension.integrationTests) return
        task.executionData.from(project.layout.buildDirectory.file("jacoco/${IntegrationTestsConfigurator.TASK_NAME}.exec"))
        task.mustRunAfter(project.tasks.named { it == IntegrationTestsConfigurator.TASK_NAME })
    }

    fun configureJacoco(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        project.pluginManager.apply("jacoco")
        project.configure<JacocoPluginExtension> {
            toolVersion = TOOL_VERSION
        }
        project.afterEvaluate {
            // The JaCoCo plugin only creates its report tasks alongside the Java plugin; looking them
            // up in, say, a docs or aggregator project failed the whole configuration.
            if (!project.plugins.hasPlugin(JavaPlugin::class.java)) return@afterEvaluate
            project.tasks.withType<Test>().configureEach { testTask ->
                testTask.finalizedBy("jacocoTestReport")
            }
            project.tasks.named("jacocoTestReport", JacocoReport::class.java) { report ->
                report.dependsOn("test")
                includeIntegrationTests(project, extension, report)
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
                includeIntegrationTests(project, extension, verification)
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
}
