/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.util

import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.config.CoverageConfig

/**
 * Coverage exclusions, for the JaCoCo (or Kover) reports and verification, and for SonarQube.
 *
 * The defaults are narrow: entry points, framework configuration and generated code, matched as a whole
 * class-name suffix or a whole package name, so `AppConfig` is left out but `ConfigParser` and `ErrorHandler`
 * are not. The build's own [CoverageConfig.exclusions] match anywhere in a class name, or as a package.
 */
object ExclusionUtil {
    /** Entry points (`DemoApplication`, Kotlin's `DemoApplicationKt`), configuration, and generated code. */
    val DEFAULT_CLASS_SUFFIXES =
        listOf(
            "Application",
            "ApplicationKt",
            "Config",
            "Configuration",
            // MapStruct's generated mappers, and JPA's generated metamodel classes (User_).
            "MapperImpl",
            "_",
        )

    /** Packages of generated code. */
    val DEFAULT_PACKAGES = listOf("generated")

    /** The defaults, as `logExclusionInfo` shows them. */
    val DEFAULT_COVERAGE_EXCLUSIONS: List<String> = DEFAULT_CLASS_SUFFIXES.map { "*$it" } + DEFAULT_PACKAGES.map { "$it package" }

    /**
     * The defaults before 0.3.0, which matched anywhere in a name: `Error` left out `ErrorHandler`, and `model`
     * whole packages of domain code. A build that wants them back adds them to `coverage { exclusions }`.
     */
    val LEGACY_DEFAULT_EXCLUSIONS =
        listOf(
            "annotation", "model", "dto", "entity", "entities", "mapper", "util", "utils", "helper", "helpers",
            "config", "Application", "Config", "Configuration", "Repository", "generated", "Test", "Mock", "Stubs",
            "Dummy", "Fake", "Abstract", "Base", "Exception", "Error", "logging",
        )

    fun getCombinedExclusions(extension: CodeArmorExtension): List<String> =
        if (extension.coverage.defaultExclusions.get()) {
            DEFAULT_COVERAGE_EXCLUSIONS + extension.coverage.exclusions.get()
        } else {
            extension.coverage.exclusions.get()
        }

    /** Class file patterns, for the JaCoCo reports. A nested class goes with its outer class. */
    fun generateJacocoReportExclusions(extension: CodeArmorExtension): List<String> =
        defaults(
            extension,
            suffix = { listOf("**/*$it.class", "**/*$it\$*.class") },
            pkg = { listOf("**/$it/**/*.class") },
        ) + extension.coverage.exclusions.get().flatMap { listOf("**/*$it*.class", "**/$it/**/*.class") }

    /** Class name patterns, for JaCoCo's coverage verification and Kover's filters. */
    fun generateJacocoVerificationExclusions(extension: CodeArmorExtension): List<String> =
        defaults(
            extension,
            suffix = { listOf("*$it", "*$it\$*") },
            pkg = { listOf("*.$it.*") },
        ) + extension.coverage.exclusions.get().flatMap { listOf("*$it*", "*.${it.lowercase()}.*") }

    /** Source file patterns, for SonarQube. */
    fun generateSonarCoverageExclusions(extension: CodeArmorExtension): List<String> =
        defaults(
            extension,
            suffix = { listOf("**/*$it.*") },
            pkg = { listOf("**/$it/**") },
        ) + extension.coverage.exclusions.get().flatMap { listOf("**/*$it*", "**/$it/**") }

    private fun defaults(
        extension: CodeArmorExtension,
        suffix: (String) -> List<String>,
        pkg: (String) -> List<String>,
    ): List<String> =
        if (extension.coverage.defaultExclusions.get()) {
            DEFAULT_CLASS_SUFFIXES.flatMap(suffix) + DEFAULT_PACKAGES.flatMap(pkg)
        } else {
            emptyList()
        }
}
