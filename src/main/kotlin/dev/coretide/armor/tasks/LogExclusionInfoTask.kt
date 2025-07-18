/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.armor.tasks

import dev.coretide.armor.CodeArmorExtension
import dev.coretide.armor.utils.ExclusionUtils
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
        println("🛡️ CodeArmor Exclusion Information")
        println("=".repeat(60))
        val combined = ExclusionUtils.getCombinedExclusions(extension)
        val defaultExclusions = ExclusionUtils.DEFAULT_COVERAGE_EXCLUSIONS
        println("📊 Summary:")
        if (extension.coverageIncludeDefaultExclusions) {
            println("  • Default exclusions: ${defaultExclusions.size}")
        } else {
            println("  • Default exclusions: DISABLED")
        }
        println("  • User exclusions: ${extension.coverageExclusions.size}")
        println("  • Total patterns: ${combined.size}")
        if (verbose || project.logger.isDebugEnabled) {
            println("\n📋 Detailed Patterns:")
            if (extension.coverageIncludeDefaultExclusions) {
                println("  Default patterns:")
                defaultExclusions.forEach { pattern ->
                    println("    - $pattern")
                }
            }
            if (extension.coverageExclusions.isNotEmpty()) {
                println("  User patterns:")
                extension.coverageExclusions.forEach { pattern ->
                    println("    - $pattern")
                }
            }
            println("\n🔧 Generated Exclusions:")
            println("  JaCoCo Report exclusions:")
            ExclusionUtils.generateJacocoReportExclusions(extension).forEach { pattern ->
                println("    - $pattern")
            }
            println("  JaCoCo Verification exclusions:")
            ExclusionUtils.generateJacocoVerificationExclusions(extension).forEach { pattern ->
                println("    - $pattern")
            }
            println("  SonarQube exclusions:")
            ExclusionUtils.generateSonarCoverageExclusions(extension).forEach { pattern ->
                println("    - $pattern")
            }
        } else {
            println("\n💡 Use --verbose or enable debug logging to see detailed patterns")
        }
        println("\n📈 Coverage Settings:")
        println("  • Minimum coverage: ${(extension.coverageMinimum * 100).toInt()}%")
        println("  • Class minimum coverage: ${(extension.coverageClassMinimum * 100).toInt()}%")
        if (extension.coverageInclusions.isNotEmpty()) {
            println("  • Coverage inclusions: ${extension.coverageInclusions.joinToString(", ")}")
        }
        println("=".repeat(60))
    }
}
