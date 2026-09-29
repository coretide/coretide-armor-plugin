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
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Gathers the SARIF reports of SpotBugs, detekt and OWASP Dependency Check, from every project, into one
 * directory for GitHub code scanning or another SARIF viewer.
 *
 * Each run gets a category of its own, from where its report lies (`codearmor/module-a/spotbugs/spotbugsMain`):
 * code scanning rejects an upload with two runs of the same tool in the same category, such as the SpotBugs runs
 * of two modules.
 */
@DisableCachingByDefault(because = "Rewrites a few reports; not worth caching")
abstract class SarifReportTask : DefaultTask() {
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val reports: ConfigurableFileCollection

    /** The reports' categories are their paths relative to this directory. */
    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun gather() {
        val output = outputDirectory.get().asFile
        output.listFiles()?.forEach { it.deleteRecursively() }
        val root = rootDirectory.get().asFile
        val gathered =
            reports.files.filter { it.isFile }.sortedBy { it.invariantSeparatorsPath }.map { report ->
                val category = category(report.relativeToOrSelf(root).invariantSeparatorsPath)
                File(output, category.removePrefix("codearmor/").replace('/', '-') + ".sarif").also {
                    it.writeText(withCategory(report.readText(), category))
                }
            }
        if (gathered.isEmpty()) {
            LogUtil.essential(this, "ℹ️ CodeArmor: no SARIF reports to gather; run the checks first, for example ./gradlew build")
        } else {
            LogUtil.essential(this, "🧾 CodeArmor: gathered ${gathered.size} SARIF report(s) in $output")
        }
    }

    companion object {
        /** `module-a/build/reports/spotbugs/spotbugsMain.sarif` becomes `codearmor/module-a/spotbugs/spotbugsMain`. */
        fun category(relativePath: String): String {
            val withoutReports = ("/" + relativePath.removeSuffix(".sarif")).replace("/build/reports/", "/").removePrefix("/")
            return "codearmor/$withoutReports"
        }

        /** The report with each run in [category]; several runs in one report are numbered. */
        fun withCategory(
            sarif: String,
            category: String,
        ): String {
            @Suppress("UNCHECKED_CAST")
            val document = JsonSlurper().parseText(sarif) as MutableMap<String, Any?>
            val runs = document["runs"] as? List<*> ?: emptyList<Any?>()
            runs.forEachIndexed { index, run ->
                @Suppress("UNCHECKED_CAST")
                val details = mutableMapOf<String, Any?>("id" to "$category/${if (runs.size > 1) index else ""}")
                (run as? MutableMap<String, Any?>)?.put("automationDetails", details)
            }
            return JsonOutput.toJson(document)
        }
    }
}
