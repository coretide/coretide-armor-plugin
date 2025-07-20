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
import dev.coretide.plugin.armor.util.LogUtil
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

open class LogExclusionInfoTask : DefaultTask() {
    @get:Internal
    @Option(option = "verbose", description = "Show detailed exclusion patterns")
    var verbose: Boolean = false

    init {
        group = "verification"
        description = "Logs coverage exclusion information for debugging"
    }

    @TaskAction
    fun logExclusionInfo() {
        val extension = project.extensions.getByType(CodeArmorExtension::class.java)
        LogUtil.essential("🛡️ CodeArmor Exclusion Information")
        LogUtil.essential("=".repeat(60))
        val combined = ExclusionUtil.getCombinedExclusions(extension)
        val defaultExclusions = ExclusionUtil.DEFAULT_COVERAGE_EXCLUSIONS
        LogUtil.essential("📊 Summary:")
        if (extension.coverageIncludeDefaultExclusions) {
            LogUtil.essential("  • Default exclusions: ${defaultExclusions.size}")
        } else {
            LogUtil.essential("  • Default exclusions: DISABLED")
        }
        LogUtil.essential("  • User exclusions: ${extension.coverageExclusions.size}")
        LogUtil.essential("  • Total patterns: ${combined.size}")
        if (extension.logLevel == ArmorLogLevel.VERBOSE) {
            LogUtil.verbose("\n📋 Detailed Patterns:")
            if (extension.coverageIncludeDefaultExclusions) {
                LogUtil.verbose("  Default patterns:")
                defaultExclusions.forEach { pattern ->
                    LogUtil.verbose("    - $pattern")
                }
            }
            if (extension.coverageExclusions.isNotEmpty()) {
                LogUtil.verbose("  User patterns:")
                extension.coverageExclusions.forEach { pattern ->
                    LogUtil.verbose("    - $pattern")
                }
            }
            LogUtil.verbose("\n🔧 Generated Exclusions:")
            LogUtil.verbose("  JaCoCo Report exclusions:")
            ExclusionUtil.generateJacocoReportExclusions(extension).forEach { pattern ->
                LogUtil.verbose("    - $pattern")
            }
            LogUtil.verbose("  JaCoCo Verification exclusions:")
            ExclusionUtil.generateJacocoVerificationExclusions(extension).forEach { pattern ->
                LogUtil.verbose("    - $pattern")
            }
            LogUtil.verbose("  SonarQube exclusions:")
            ExclusionUtil.generateSonarCoverageExclusions(extension).forEach { pattern ->
                LogUtil.verbose("    - $pattern")
            }
        } else {
            LogUtil.essential("\n💡 Use --verbose or enable debug logging to see detailed patterns")
        }
        LogUtil.essential("\n📈 Coverage Settings:")
        LogUtil.essential("  • Minimum coverage: ${(extension.coverageMinimum * 100).toInt()}%")
        LogUtil.essential("  • Class minimum coverage: ${(extension.coverageClassMinimum * 100).toInt()}%")
        if (extension.coverageInclusions.isNotEmpty()) {
            LogUtil.essential("  • Coverage inclusions: ${extension.coverageInclusions.joinToString(", ")}")
        }
        LogUtil.essential("=".repeat(60))
    }
}
