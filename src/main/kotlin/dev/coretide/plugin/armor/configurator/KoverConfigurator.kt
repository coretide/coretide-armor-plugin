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
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Project

/**
 * Kover instead of JaCoCo (`coverage { kover = true }`) in Kotlin projects: JetBrains' coverage tool, which understands
 * Kotlin's inline functions and coroutines. The same thresholds and exclusions apply. Kover only measures
 * projects that apply the Kotlin JVM plugin, so Java-only projects keep JaCoCo.
 */
object KoverConfigurator {
    const val PLUGIN_ID = "org.jetbrains.kotlinx.kover"
    val BUILD_TIER_TASKS = listOf("koverXmlReport", "koverHtmlReport", "koverVerify")

    /** Kover's XML report, in JaCoCo's format, which SonarQube reads. */
    const val XML_REPORT = "build/reports/kover/report.xml"

    private const val KOTLIN_JVM_PLUGIN_ID = "org.jetbrains.kotlin.jvm"

    /** Whether [project] measures coverage with Kover: asked for, and a Kotlin project. */
    fun usesKover(
        project: Project,
        extension: CodeArmorExtension,
    ): Boolean = extension.coverage.kover.get() && project.plugins.hasPlugin(KOTLIN_JVM_PLUGIN_ID)

    fun configure(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        project.pluginManager.apply(PLUGIN_ID)
        project.extensions.configure(KoverProjectExtension::class.java) { kover ->
            kover.reports { reports ->
                reports.filters { filters ->
                    // JaCoCo's class-name wildcards (*Config*, *.dto.*) are also Kover's.
                    filters.excludes { it.classes(ExclusionUtil.generateJacocoVerificationExclusions(extension)) }
                    if (extension.coverage.inclusions.get().isNotEmpty()) {
                        filters.includes { it.classes(extension.coverage.inclusions.get().map(::toClassPattern)) }
                    }
                }
                reports.verify { verify ->
                    verify.rule { rule -> rule.minBound(percent(extension.coverage.minimum.get())) }
                    verify.rule("classes") { rule ->
                        rule.groupBy.set(GroupingEntityType.CLASS)
                        rule.bound { bound ->
                            bound.minValue.set(percent(extension.coverage.classMinimum.get()))
                            bound.coverageUnits.set(CoverageUnit.LINE)
                        }
                    }
                }
            }
        }
        LogUtil.verbose("📊 Kover configured instead of JaCoCo")
    }

    private fun percent(fraction: Double): Int = Math.round(fraction * 100).toInt()

    /** A path-style pattern, as JaCoCo's report filters take (`com/example/` then `**`), as a class name pattern. */
    private fun toClassPattern(pattern: String): String = pattern.replace('/', '.').replace("**", "*").removeSuffix(".class")
}
