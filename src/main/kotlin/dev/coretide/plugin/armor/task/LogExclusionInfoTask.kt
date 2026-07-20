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

import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.enumeration.ArmorLogLevel
import dev.coretide.plugin.armor.util.ExclusionUtil
import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

/**
 * Diagnostic task that prints the effective coverage-exclusion configuration.
 *
 * The report is rendered at configuration time into [reportLines] so the task body touches no
 * `Project` state — required for configuration-cache compatibility. It is untracked because it
 * has no outputs and must re-run on demand.
 */
@UntrackedTask(because = "Diagnostic reporting task with no outputs; must always run when invoked")
abstract class LogExclusionInfoTask : DefaultTask() {
    @get:Input
    abstract val reportLines: ListProperty<String>

    init {
        group = "verification"
        description = "Logs coverage exclusion information for debugging"
    }

    @TaskAction
    fun logExclusionInfo() {
        reportLines.get().forEach { logger.lifecycle(it) }
    }

    companion object {
        private const val SEPARATOR_WIDTH = 60

        /**
         * Renders the exclusion report from [extension]. Called at configuration time so the
         * resulting strings can be captured by the configuration cache.
         */
        fun renderReport(extension: CodeArmorExtension): List<String> {
            val lines = mutableListOf<String>()
            val combined = ExclusionUtil.getCombinedExclusions(extension)
            val defaultExclusions = ExclusionUtil.DEFAULT_COVERAGE_EXCLUSIONS

            lines += "🛡️ CodeArmor Exclusion Information"
            lines += "=".repeat(SEPARATOR_WIDTH)
            lines += "📊 Summary:"
            if (extension.coverageIncludeDefaultExclusions) {
                lines += "  • Default exclusions: ${defaultExclusions.size}"
            } else {
                lines += "  • Default exclusions: DISABLED"
            }
            lines += "  • User exclusions: ${extension.coverageExclusions.size}"
            lines += "  • Total patterns: ${combined.size}"

            if (extension.logLevel == ArmorLogLevel.VERBOSE) {
                lines += "\n📋 Detailed Patterns:"
                if (extension.coverageIncludeDefaultExclusions) {
                    lines += "  Default patterns:"
                    defaultExclusions.forEach { lines += "    - $it" }
                }
                if (extension.coverageExclusions.isNotEmpty()) {
                    lines += "  User patterns:"
                    extension.coverageExclusions.forEach { lines += "    - $it" }
                }
                lines += "\n🔧 Generated Exclusions:"
                lines += "  JaCoCo Report exclusions:"
                ExclusionUtil.generateJacocoReportExclusions(extension).forEach { lines += "    - $it" }
                lines += "  JaCoCo Verification exclusions:"
                ExclusionUtil.generateJacocoVerificationExclusions(extension).forEach { lines += "    - $it" }
                lines += "  SonarQube exclusions:"
                ExclusionUtil.generateSonarCoverageExclusions(extension).forEach { lines += "    - $it" }
            } else {
                lines += "\n💡 Set codeArmor.logLevel = ArmorLogLevel.VERBOSE to see detailed patterns"
            }

            lines += "\n📈 Coverage Settings:"
            lines += "  • Minimum coverage: ${(extension.coverageMinimum * 100).toInt()}%"
            lines += "  • Class minimum coverage: ${(extension.coverageClassMinimum * 100).toInt()}%"
            if (extension.coverageInclusions.isNotEmpty()) {
                lines += "  • Coverage inclusions: ${extension.coverageInclusions.joinToString(", ")}"
            }
            lines += "=".repeat(SEPARATOR_WIDTH)
            return lines
        }
    }
}
