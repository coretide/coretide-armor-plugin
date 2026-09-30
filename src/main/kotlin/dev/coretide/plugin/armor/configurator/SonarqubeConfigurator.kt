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
    /** What points the SonarScanner at a server, or authenticates it, from outside the build script. */
    private val SERVER_ENVIRONMENT = listOf("SONAR_HOST_URL", "SONAR_TOKEN")
    private val SERVER_SYSTEM_PROPERTIES = listOf("sonar.host.url", "sonar.token", "sonar.login")

    /** The SonarScanner's server when none is configured. */
    private const val DEFAULT_SERVER = "https://sonarcloud.io"

    /**
     * Whether the build names a SonarQube server, or a token for one; a token alone means SonarQube Cloud,
     * the SonarScanner's default. Without either, `sonar` has nowhere to send the analysis, so it leaves
     * the default CI tier.
     */
    fun isConfigured(
        project: Project,
        extension: CodeArmorExtension,
    ): Boolean =
        !extension.sonarqube.hostUrl.orNull.isNullOrBlank() ||
            !extension.sonarqube.token.orNull.isNullOrBlank() ||
            SERVER_ENVIRONMENT.any { !project.providers.environmentVariable(it).orNull.isNullOrBlank() } ||
            SERVER_SYSTEM_PROPERTIES.any { !project.providers.systemProperty(it).orNull.isNullOrBlank() }

    /** `SONAR_HOST_URL`, else the build's own setting; null leaves the choice to the SonarScanner. */
    fun hostUrl(extension: CodeArmorExtension): String? =
        System.getenv("SONAR_HOST_URL")?.takeIf { it.isNotBlank() } ?: extension.sonarqube.hostUrl.orNull?.takeIf { it.isNotBlank() }

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
                hostUrl(extension)?.let { sonarProperties.property("sonar.host.url", it) }
                sonarProperties.property("sonar.projectKey", projectKey(project, extension))
                sonarProperties.property(
                    "sonar.projectName",
                    extension.sonarqube.projectName.orNull?.takeIf { it.isNotEmpty() } ?: project.name,
                )
                // Only when configured: the SonarScanner reads SONAR_TOKEN from the environment, the usual way on CI,
                // only while sonar.token is unset. An empty value would shadow it.
                extension.sonarqube.token.orNull?.takeIf { it.isNotEmpty() }?.let { sonarProperties.property("sonar.token", it) }
                sonarProperties.property("sonar.projectVersion", "${project.version}")
                sonarProperties.property("sonar.sourceEncoding", "UTF-8")
                // Sources, tests and class directories come from the source sets: the SonarScanner reads
                // them itself, including Kotlin, generated and custom source sets.
                extension.sonarqube.javaVersion.orNull?.takeIf { it.isNotBlank() }?.let { javaVersion ->
                    sonarProperties.property("sonar.java.source", javaVersion)
                    sonarProperties.property("sonar.java.target", javaVersion)
                }
                // In a multi-module build, the combined report also counts tests in other modules.
                sonarProperties.property(
                    "sonar.coverage.jacoco.xmlReportPaths",
                    listOfNotNull(
                        if (usesKover) KoverConfigurator.XML_REPORT else "build/reports/jacoco/test/jacocoTestReport.xml",
                        aggregatedCoverage?.absolutePath,
                    ).joinToString(","),
                )
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
                // Quality gates, coverage and duplication thresholds, and ratings are set on the server; the
                // SonarScanner has no properties for them.
                sonarProperties.property("sonar.qualitygate.wait", extension.sonarqube.qualityGateWait.get().toString())
                if (extension.spotbugs.enabled.get() && extension.spotbugs.xmlReports.get()) {
                    sonarProperties.property(
                        "sonar.java.spotbugs.reportPaths",
                        SpotbugsConfigurator.reportFile(project, "spotbugsMain", "xml").absolutePath,
                    )
                }
                DetektConfigurator.checkstyleReport(project, extension)?.let { report ->
                    sonarProperties.property("sonar.kotlin.detekt.reportPaths", report.absolutePath)
                }
                if (extension.owasp.enabled.get()) {
                    // The Dependency-Check SonarQube plugin reads the JSON report, and no other.
                    sonarProperties.property(
                        "sonar.dependencyCheck.jsonReportPath",
                        OwaspConfigurator.jsonReport(project).absolutePath,
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
                } else if (extension.coverage.enabled.get()) {
                    task.dependsOn("jacocoTestCoverageVerification")
                    if (aggregatedCoverage != null) {
                        task.dependsOn(":${AggregatedReportsConfigurator.COVERAGE_REPORT}")
                    }
                }
                if (extension.spotbugs.enabled.get()) {
                    task.dependsOn("spotbugsMain")
                }
                if (extension.owasp.enabled.get()) {
                    task.dependsOn("dependencyCheckAnalyze")
                }
                val dashboard = "${hostUrl(extension) ?: DEFAULT_SERVER}/dashboard?id=${projectKey(project, extension)}"
                task.doLast {
                    LogUtil.verbose("✅ SonarQube analysis completed")
                    LogUtil.verbose("🔍 View results at: $dashboard")
                }
            }
        }
    }

    private fun projectKey(
        project: Project,
        extension: CodeArmorExtension,
    ): String = extension.sonarqube.projectKey.orNull?.takeIf { it.isNotEmpty() } ?: "${project.group}:${project.name}"
}
