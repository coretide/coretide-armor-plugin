/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.configurator

import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.ProjectType
import dev.coretide.plugin.armor.util.ExclusionUtil
import dev.coretide.plugin.armor.util.LogUtil
import dev.coretide.plugin.armor.util.ProjectDetector
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.sonarqube.gradle.SonarExtension

object SonarqubeConfigurator {
    fun configureSonarqube(
        project: Project,
        extension: CodeArmorExtension,
        projectType: ProjectType,
    ) {
        project.pluginManager.apply("org.sonarqube")
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
                sonarProperties.property(
                    "sonar.token",
                    extension.sonarToken?.takeIf { it.isNotEmpty() } ?: "",
                )
                sonarProperties.property("sonar.projectVersion", "${project.version}")
                sonarProperties.property("sonar.sourceEncoding", "UTF-8")
                when (projectType) {
                    ProjectType.JAVA_APPLICATION, ProjectType.JAVA_LIBRARY -> {
                        sonarProperties.property("sonar.sources", "src/main/java")
                        sonarProperties.property("sonar.tests", "src/test/java")
                        sonarProperties.property("sonar.java.source", extension.sonarJavaVersion)
                        sonarProperties.property("sonar.java.target", extension.sonarJavaVersion)
                        sonarProperties.property("sonar.java.binaries", "build/classes/java/main")
                        sonarProperties.property("sonar.java.test.binaries", "build/classes/java/test")
                    }

                    ProjectType.KOTLIN_APPLICATION, ProjectType.KOTLIN_LIBRARY -> {
                        sonarProperties.property("sonar.sources", "src/main/kotlin")
                        sonarProperties.property("sonar.tests", "src/test/kotlin")
                        sonarProperties.property("sonar.kotlin.source", extension.sonarJavaVersion)
                        sonarProperties.property("sonar.kotlin.target", extension.sonarJavaVersion)
                        sonarProperties.property("sonar.kotlin.binaries", "build/classes/kotlin/main")
                        sonarProperties.property("sonar.kotlin.test.binaries", "build/classes/kotlin/test")
                    }

                    ProjectType.MIXED_APPLICATION, ProjectType.MIXED_LIBRARY -> {
                        sonarProperties.property("sonar.sources", "src/main/java,src/main/kotlin")
                        sonarProperties.property("sonar.tests", "src/test/java,src/test/kotlin")
                        sonarProperties.property("sonar.java.source", extension.sonarJavaVersion)
                        sonarProperties.property("sonar.java.target", extension.sonarJavaVersion)
                        sonarProperties.property("sonar.kotlin.source", extension.sonarJavaVersion)
                        sonarProperties.property("sonar.kotlin.target", extension.sonarJavaVersion)
                        sonarProperties.property("sonar.java.binaries", "build/classes/java/main,build/classes/kotlin/main")
                        sonarProperties.property("sonar.java.test.binaries", "build/classes/java/test,build/classes/kotlin/test")
                    }
                }
                sonarProperties.property("sonar.java.coveragePlugin", "jacoco")
                sonarProperties.property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
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
                if (extension.spotbugs) {
                    sonarProperties.property("sonar.java.spotbugs.reportPaths", "build/reports/spotbugs/main.xml")
                }
                if (extension.checkstyle && ProjectDetector.needsCheckstyle(projectType)) {
                    sonarProperties.property("sonar.java.checkstyle.reportPaths", "build/reports/checkstyle/main.xml")
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
                if (extension.jacoco) {
                    task.dependsOn("jacocoTestCoverageVerification")
                }
                if (extension.checkstyle && ProjectDetector.needsCheckstyle(projectType)) {
                    task.dependsOn("checkstyleMain")
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
