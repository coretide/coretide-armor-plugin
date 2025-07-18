/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.configurators

import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.utils.ExclusionUtils
import dev.coretide.plugin.armor.utils.ExclusionUtils.generateJacocoVerificationExclusions
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
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
            toolVersion = "0.8.12"
        }
        project.afterEvaluate {
            project.tasks.withType<Test>().configureEach { testTask ->
                testTask.useJUnitPlatform()
                testTask.testLogging {
                    it.events("passed", "skipped", "failed", "standardOut", "standardError")
                    it.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                    it.showExceptions = true
                    it.showCauses = true
                    it.showStackTraces = true
                    it.showStandardStreams = true
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
                val jacocoExclusions = ExclusionUtils.generateJacocoReportExclusions(extension)
                report.classDirectories.setFrom(
                    project.files(
                        report.classDirectories.files.map { file ->
                            project.fileTree(file).exclude(jacocoExclusions)
                        },
                    ),
                )
                report.doLast {
                    println("📊 JaCoCo coverage report generated")
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
                    println("✅ JaCoCo coverage verification completed")
                }
            }
        }
    }
}
