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
import dev.coretide.plugin.armor.util.LogUtil
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.sonarqube.gradle.SonarExtension
import java.io.File

object SonarqubeConfigurator {
    fun configureSonarqube(
        project: Project,
        extension: CodeArmorExtension,
        aggregatedCoverage: File? = null,
    ) {
        project.pluginManager.apply("org.sonarqube")
        val usesKover = KoverConfigurator.usesKover(project, extension)
        project.configure<SonarExtension> {
            properties { sonarProperties ->
                sonarProperties.property("sonar.scm.provider", "git")
                sonarProperties.property("sonar.host.url", System.getenv("SONAR_HOST_URL") ?: extension.sonarHostUrl)
                sonarProperties.property(
                    "sonar.projectKey",
                    extension.sonarProjectKey?.takeIf { it.isNotEmpty() } ?: "${project.group}:${project.name}",
                )
                sonarProperties.property(
                    "sonar.projectName",
                    extension.sonarProjectName?.takeIf { it.isNotEmpty() } ?: project.name,
                )
                // Only when configured: the SonarScanner reads SONAR_TOKEN from the environment, the usual way on CI,
                // only while sonar.token is unset. An empty value would shadow it.
                extension.sonarToken?.takeIf { it.isNotEmpty() }?.let { sonarProperties.property("sonar.token", it) }
                sonarProperties.property("sonar.projectVersion", "${project.version}")
                sonarProperties.property("sonar.sourceEncoding", "UTF-8")
                // Sources, tests and class directories come from the source sets: the SonarScanner reads
                // them itself, including Kotlin, generated and custom source sets.
                extension.sonarJavaVersion?.takeIf { it.isNotBlank() }?.let { javaVersion ->
                    sonarProperties.property("sonar.java.source", javaVersion)
                    sonarProperties.property("sonar.java.target", javaVersion)
                }
                sonarProperties.property("sonar.java.coveragePlugin", "jacoco")
                // In a multi-module build, the combined report also counts tests in other modules.
                sonarProperties.property(
                    "sonar.coverage.jacoco.xmlReportPaths",
                    listOfNotNull(
                        if (usesKover) KoverConfigurator.XML_REPORT else "build/reports/jacoco/test/jacocoTestReport.xml",
                        aggregatedCoverage?.absolutePath,
                    ).joinToString(","),
                )
                sonarProperties.property("sonar.coverage.minimum", "${(extension.coverageMinimum * 100).toInt()}")
                val sonarCoverageExclusions = ExclusionUtil.generateSonarCoverageExclusions(extension)
                sonarProperties.property("sonar.coverage.exclusions", sonarCoverageExclusions.joinToString(","))
                sonarProperties.property(
                    "sonar.exclusions",
                    listOf(
                        "build/**",
                        "target/**",
                        "**/*.proto",
                        "src/main/resources/**",
                        "**/generated/**",
                    ).joinToString(","),
                )
                sonarProperties.property("sonar.qualitygate.wait", extension.sonarQualityGateWait.toString())
                sonarProperties.property("sonar.duplicated_lines_density", "15")
                sonarProperties.property("sonar.maintainability_rating", "C")
                sonarProperties.property("sonar.reliability_rating", "C")
                sonarProperties.property("sonar.security_rating", "C")
                if (extension.spotbugs && extension.spotbugsConfig.xmlReports) {
                    sonarProperties.property(
                        "sonar.java.spotbugs.reportPaths",
                        SpotbugsConfigurator.reportFile(project, "spotbugsMain", "xml").absolutePath,
                    )
                }
                DetektConfigurator.checkstyleReport(project)?.let { report ->
                    sonarProperties.property("sonar.kotlin.detekt.reportPaths", report.absolutePath)
                }
                if (extension.owasp) {
                    sonarProperties.property(
                        "sonar.dependencyCheck.reportPath",
                        "build/reports/dependency-check/dependency-check-report.xml",
                    )
                    sonarProperties.property(
                        "sonar.dependencyCheck.htmlReportPath",
                        "build/reports/dependency-check/dependency-check-report.html",
                    )
                }
            }
        }
        project.afterEvaluate {
            project.tasks.named("sonar") { task ->
                task.group = "verification"
                task.description = "Runs SonarQube analysis"
                task.dependsOn("build")
                if (usesKover) {
                    task.dependsOn("koverXmlReport")
                } else if (extension.jacoco) {
                    task.dependsOn("jacocoTestCoverageVerification")
                    if (aggregatedCoverage != null) {
                        task.dependsOn(":${AggregatedReportsConfigurator.COVERAGE_REPORT}")
                    }
                }
                if (extension.spotbugs) {
                    task.dependsOn("spotbugsMain")
                }
                if (extension.owasp) {
                    task.dependsOn("dependencyCheckAnalyze")
                }
                task.doLast {
                    LogUtil.verbose("✅ SonarQube analysis completed")
                    LogUtil.verbose(
                        "🔍 View results at: ${extension.sonarHostUrl}/dashboard?id=${
                            extension.sonarProjectKey?.ifEmpty {
                                "${project.group}:${project.name}"
                            }
                        }",
                    )
                }
            }
        }
    }
}
