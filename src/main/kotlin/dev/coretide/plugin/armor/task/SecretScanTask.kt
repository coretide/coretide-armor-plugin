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

import dev.coretide.plugin.armor.git.GitRepository
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Scans the repository's history for secrets with gitleaks, and fails when it finds one. Its SARIF report goes to
 * code scanning with the other tools' findings. The CI counterpart of the pre-commit hook `secretScan` installs,
 * which only sees new commits on machines that have it.
 */
@UntrackedTask(because = "Scans the git history, which Gradle does not track")
abstract class SecretScanTask : DefaultTask() {
    @get:Internal
    abstract val gradleRootDirectory: DirectoryProperty

    @get:Internal
    abstract val report: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        group = "verification"
        description = "🔑 Scans the git history for secrets with gitleaks"
    }

    @TaskAction
    fun scan() {
        val output = report.get().asFile
        output.delete()
        val command = System.getenv("CODEARMOR_GITLEAKS")?.takeIf { it.isNotBlank() } ?: "gitleaks"
        if (ArmorInfoTask.findExecutable(command, System.getenv("PATH").orEmpty()) == null) {
            throw GradleException(
                "gitleaks is not installed, so the history was not scanned for secrets. Install it " +
                    "(https://github.com/gitleaks/gitleaks#installing), or leave armorSecretScan out of checks.ci",
            )
        }
        val repository =
            GitRepository.locate(execOperations, gradleRootDirectory.get().asFile)
                ?: throw GradleException("armorSecretScan needs a git repository")
        output.parentFile.mkdirs()
        val arguments = listOf("--report-format", "sarif", "--report-path", output.absolutePath, "--redact", "--no-banner", "--exit-code", "1")
        // gitleaks 8.19 replaced `detect` with `git`.
        val subcommand = if (succeeds(command, "git", "--help")) listOf("git") else listOf("detect", "--source")
        val errors = ByteArrayOutputStream()
        val result =
            execOperations.exec {
                it.workingDir = repository.topLevel
                it.commandLine(listOf(command) + subcommand + repository.topLevel.absolutePath + arguments)
                it.errorOutput = errors
                it.isIgnoreExitValue = true
            }
        when (result.exitValue) {
            0 -> logger.lifecycle("🔑 gitleaks: no secrets in the history")
            1 -> throw GradleException("gitleaks found secrets in the history; see $output. Rotate them, then remove them or list false positives in .gitleaksignore")
            else -> throw GradleException("gitleaks failed (exit ${result.exitValue}): ${errors.toString().trim()}")
        }
    }

    private fun succeeds(vararg command: String): Boolean =
        execOperations.exec {
            it.commandLine(command.toList())
            it.standardOutput = ByteArrayOutputStream()
            it.errorOutput = ByteArrayOutputStream()
            it.isIgnoreExitValue = true
        }.exitValue == 0

    companion object {
        const val TASK_NAME = "armorSecretScan"

        /** Where the SARIF report goes, under the build directory; `armorSarifReport` gathers it. */
        const val REPORT = "reports/gitleaks/gitleaks.sarif"

        /** Registers the scan in [project], the one CodeArmor is applied to: it covers the whole repository. */
        fun register(project: Project): TaskProvider<SecretScanTask> =
            project.tasks.register(TASK_NAME, SecretScanTask::class.java) { task ->
                task.gradleRootDirectory.set(project.rootDir)
                task.report.set(project.layout.buildDirectory.file(REPORT))
            }
    }
}
