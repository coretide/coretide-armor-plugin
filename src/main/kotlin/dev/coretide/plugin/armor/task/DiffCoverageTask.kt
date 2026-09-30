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

import dev.coretide.plugin.armor.git.GitCommand
import dev.coretide.plugin.armor.git.GitRepository
import dev.coretide.plugin.armor.util.DiffCoverage
import dev.coretide.plugin.armor.util.LogUtil
import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import java.util.Locale
import javax.inject.Inject

/**
 * Coverage of the lines changed since the base branch: what a pull request adds, measured by the coverage report
 * the build already writes. It reports; with [minimum] set, it also fails below it.
 *
 * Untracked: its input is the git history and working tree, which Gradle does not see.
 */
@UntrackedTask(because = "Reads the git history and working tree, which Gradle does not track")
abstract class DiffCoverageTask : DefaultTask() {
    /** JaCoCo's XML report, or Kover's in the same format. */
    @get:Internal
    abstract val coverageReport: RegularFileProperty

    /** The production source directories, to find a changed file's entry in the report. */
    @get:Internal
    abstract val sourceDirectories: ConfigurableFileCollection

    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    /** The branch to compare with; unset, the CI's target branch, then `origin/HEAD`, `main` or `master`. */
    @get:Input
    @get:Optional
    abstract val base: Property<String>

    /** The share of changed lines tests must cover, from 0 to 1. Unset, the task only reports. */
    @get:Input
    @get:Optional
    abstract val minimum: Property<Double>

    /** The result, as JSON, for `armorReport`. */
    @get:Internal
    abstract val result: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        group = "verification"
        description = "📐 Coverage of the lines changed since the base branch"
    }

    @TaskAction
    fun measure() {
        val output = result.get().asFile
        output.delete()
        val report = coverageReport.get().asFile
        if (!report.isFile) {
            LogUtil.essential(this, "📐 Diff coverage: no coverage report, so nothing to measure")
            return
        }
        val repository = GitRepository.locate(execOperations, projectDirectory.get().asFile)
        if (repository == null) {
            LogUtil.essential(this, "📐 Diff coverage skipped: not a git repository")
            return
        }
        val git = repository.git
        val base = resolveBase(git)
        if (base == null) {
            val configured = this.base.orNull?.let { "$it does not exist" } ?: "no base branch found"
            LogUtil.essential(
                this,
                "📐 Diff coverage skipped: $configured. Set diffCoverage { base }, or fetch the base branch " +
                    "(on GitHub Actions, actions/checkout with fetch-depth: 0)",
            )
            return
        }
        val mergeBase = git.output("merge-base", base, "HEAD")
        if (mergeBase == null) {
            LogUtil.essential(this, "📐 Diff coverage skipped: HEAD shares no history with $base. Fetch the full history (fetch-depth: 0)")
            return
        }
        // Against the working tree, so uncommitted changes count too; renames keep their changed lines only.
        val diff = git.output("-c", "core.quotePath=false", "diff", "--unified=0", "--no-color", "--no-ext-diff", "-M", mergeBase).orEmpty()
        val files =
            DiffCoverage.measure(
                DiffCoverage.changedLines(diff),
                repository.topLevel,
                sourceDirectories.files,
                DiffCoverage.lineCoverage(report),
            )
        val lines = files.sumOf { it.lines }
        val missed = files.sumOf { it.missed.size }
        output.parentFile.mkdirs()
        output.writeText(
            JsonOutput.prettyPrint(
                JsonOutput.toJson(
                    mapOf(
                        "base" to base,
                        "lines" to lines,
                        "covered" to lines - missed,
                        "minimum" to minimum.orNull,
                        "files" to files.map { mapOf("path" to it.path, "lines" to it.lines, "missed" to it.missed) },
                    ),
                ),
            ),
        )
        if (lines == 0) {
            LogUtil.essential(this, "📐 Diff coverage: no changed lines with code since $base")
            return
        }
        val share = (lines - missed).toDouble() / lines
        LogUtil.essential(this, "📐 Diff coverage: ${percent(share)} of $lines changed lines since $base")
        files.filter { it.missed.isNotEmpty() }.forEach {
            LogUtil.essential(this, "   ${it.path}: untested lines ${it.missed.joinToString(", ")}")
        }
        val required = minimum.orNull ?: return
        if (share < required) {
            throw GradleException(
                "Diff coverage ${percent(share)} is below its minimum, ${percent(required)}: test the changed lines " +
                    "listed above, or lower diffCoverage { minimum }",
            )
        }
    }

    /** The first of the candidates that names a commit. */
    private fun resolveBase(git: GitCommand): String? {
        val configured = base.orNull?.takeIf { it.isNotBlank() }
        val candidates =
            if (configured != null) {
                listOf(configured)
            } else {
                buildList {
                    ciTargetBranch()?.let {
                        add("origin/$it")
                        add(it)
                    }
                    git.output("symbolic-ref", "--quiet", "--short", "refs/remotes/origin/HEAD")?.let { add(it) }
                    addAll(listOf("origin/main", "origin/master", "main", "master"))
                }
            }
        return candidates.firstOrNull { git.run("rev-parse", "--verify", "--quiet", "$it^{commit}") }
    }

    companion object {
        const val TASK_NAME = "armorDiffCoverage"

        /** The branch a pull or merge request targets, as CI services name it. */
        private val CI_TARGET_BRANCH =
            listOf(
                "GITHUB_BASE_REF",
                "CI_MERGE_REQUEST_TARGET_BRANCH_NAME",
                "CHANGE_TARGET",
                "SYSTEM_PULLREQUEST_TARGETBRANCH",
                "BITBUCKET_PR_DESTINATION_BRANCH",
            )

        fun ciTargetBranch(environment: Map<String, String> = System.getenv()): String? =
            CI_TARGET_BRANCH.firstNotNullOfOrNull { environment[it]?.takeIf { value -> value.isNotBlank() } }?.removePrefix("refs/heads/")

        fun percent(share: Double): String = String.format(Locale.ROOT, "%.1f%%", share * 100)
    }
}
