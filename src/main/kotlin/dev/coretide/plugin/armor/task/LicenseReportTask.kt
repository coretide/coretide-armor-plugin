/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.task

import dev.coretide.plugin.armor.util.LogUtil
import dev.coretide.plugin.armor.util.SbomLicenses
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * The licences of the dependencies in the project's CycloneDX SBOM, grouped by licence. Fails when a dependency
 * can only be used under one of [forbiddenLicenses].
 */
@CacheableTask
abstract class LicenseReportTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val sbom: RegularFileProperty

    @get:Input
    abstract val forbiddenLicenses: ListProperty<String>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun write() {
        val components = SbomLicenses.read(sbom.get().asFile)
        val byLicense = SbomLicenses.byLicense(components)
        val reportFile = report.get().asFile
        reportFile.writeText(
            buildString {
                appendLine("Licences of the ${components.size} dependencies in ${sbom.get().asFile.name}")
                byLicense.forEach { (license, coordinates) ->
                    appendLine()
                    appendLine("$license (${coordinates.size})")
                    coordinates.forEach { appendLine("  $it") }
                }
            },
        )
        val unknown = byLicense[SbomLicenses.UNKNOWN]?.size ?: 0
        val licenses = byLicense.keys.count { it != SbomLicenses.UNKNOWN }
        LogUtil.essential(
            this,
            "📜 CodeArmor: ${components.size} dependencies under $licenses licence(s)" +
                (if (unknown > 0) ", $unknown without licence information" else "") +
                ": ${reportFile.path}",
        )

        val forbidden = SbomLicenses.forbidden(components, forbiddenLicenses.get())
        if (forbidden.isNotEmpty()) {
            throw GradleException(
                "${forbidden.size} dependencies can only be used under a forbidden licence:\n" +
                    forbidden.joinToString("\n") { "  ${it.coordinates}: ${it.licenses.joinToString(", ")}" },
            )
        }
    }
}
