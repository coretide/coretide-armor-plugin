/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.armor.utils

import dev.coretide.armor.CodeArmorExtension

object ExclusionUtils {
    val DEFAULT_COVERAGE_EXCLUSIONS =
        listOf(
            "annotation",
            "model",
            "dto",
            "entity",
            "entities",
            "mapper",
            "util",
            "utils",
            "helper",
            "helpers",
            "config",
            "Application",
            "Config",
            "Configuration",
            "Repository",
            "generated",
            "Test",
            "Mock",
            "Stubs",
            "Dummy",
            "Fake",
            "Abstract",
            "Base",
            "Exception",
            "Error",
            "logging",
        )

    fun getCombinedExclusions(extension: CodeArmorExtension): List<String> =
        if (extension.coverageIncludeDefaultExclusions) {
            DEFAULT_COVERAGE_EXCLUSIONS + extension.coverageExclusions
        } else {
            extension.coverageExclusions
        }

    fun generateJacocoReportExclusions(extension: CodeArmorExtension): List<String> {
        val combinedExclusions = getCombinedExclusions(extension)
        return combinedExclusions.flatMap { pattern ->
            listOf(
                "**/*$pattern*.class",
                "**/$pattern/**/*.class",
            )
        }
    }

    fun generateJacocoVerificationExclusions(extension: CodeArmorExtension): List<String> {
        val combinedExclusions = getCombinedExclusions(extension)
        return combinedExclusions.flatMap { pattern ->
            listOf(
                "*$pattern*",
                "*.${pattern.lowercase()}.*",
            )
        }
    }

    fun generateSonarCoverageExclusions(extension: CodeArmorExtension): List<String> {
        val combinedExclusions = getCombinedExclusions(extension)
        return combinedExclusions.flatMap { pattern ->
            listOf(
                "**/*$pattern*",
                "**/$pattern/**",
            )
        }
    }
}
