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
import dev.coretide.plugin.armor.util.FileUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * Writes a starting point for the project's setup: an `.editorconfig` in the Gradle root, and a GitHub Actions
 * workflow at the top of the repository that runs the checks and uploads their findings to code scanning.
 * Existing files are never overwritten.
 */
@UntrackedTask(because = "Scaffolds into the project tree on demand; the files are the project's once written")
abstract class ScaffoldProjectTask : DefaultTask() {
    @get:Internal
    abstract val gradleRootDirectory: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        group = "build setup"
        description = "📝 Writes an .editorconfig and a GitHub Actions workflow that uploads findings to code scanning"
    }

    @TaskAction
    fun scaffold() {
        val gradleRoot = gradleRootDirectory.get().asFile.canonicalFile
        // Workflows only run from the top of the repository, which may be above the Gradle root.
        val repositoryRoot = GitRepository.locate(execOperations, gradleRoot)?.topLevel?.canonicalFile ?: gradleRoot
        val gradleRootFromTop = gradleRoot.relativeTo(repositoryRoot).invariantSeparatorsPath
        listOf(
            File(gradleRoot, ".editorconfig") to EDITORCONFIG,
            File(repositoryRoot, ".github/workflows/codearmor.yml") to workflow(gradleRootFromTop),
        ).forEach { (file, content) ->
            if (FileUtil.scaffoldIfAbsent(file, content)) {
                logger.lifecycle("📝 Created ${file.absolutePath}")
            } else {
                logger.lifecycle("↩️  Kept existing ${file.absolutePath}")
            }
        }
    }

    companion object {
        val EDITORCONFIG =
            """
            # Written by `./gradlew armorScaffoldProject`. IntelliJ IDEA, ktlint and most editors read it.
            root = true

            [*]
            charset = utf-8
            end_of_line = lf
            insert_final_newline = true
            trim_trailing_whitespace = true
            indent_style = space
            indent_size = 4

            [*.{kt,kts}]
            max_line_length = 120
            ij_kotlin_allow_trailing_comma = true
            ij_kotlin_allow_trailing_comma_on_call_site = true

            [*.java]
            max_line_length = 120

            [*.{yml,yaml,json,xml}]
            indent_size = 2

            [*.md]
            trim_trailing_whitespace = false

            [*.{bat,cmd}]
            end_of_line = crlf

            """.trimIndent()

        /** [gradleRoot]: the Gradle root, relative to the top of the repository; empty when they are the same. */
        fun workflow(gradleRoot: String): String {
            val workingDirectory =
                if (gradleRoot.isEmpty()) {
                    ""
                } else {
                    "    defaults:\n      run:\n        working-directory: $gradleRoot\n"
                }
            val sarif = if (gradleRoot.isEmpty()) "build/reports/sarif" else "$gradleRoot/build/reports/sarif"
            return """
                |# Written by `./gradlew armorScaffoldProject`; CodeArmor never overwrites it.
                |# Runs CodeArmor's checks, and uploads SpotBugs and detekt findings to GitHub code scanning:
                |# the repository's Security tab, and annotations on pull requests.
                |name: codearmor
                |
                |on:
                |  push:
                |    branches: [main]
                |  pull_request:
                |
                |permissions:
                |  contents: read
                |
                |jobs:
                |  checks:
                |    runs-on: ubuntu-latest
                |    permissions:
                |      contents: read
                |      security-events: write
                |$workingDirectory    steps:
                |      - uses: actions/checkout@v7
                |        with:
                |          # CodeArmor derives the project version from git tags.
                |          fetch-depth: 0
                |
                |      - uses: actions/setup-java@v6
                |        with:
                |          distribution: temurin
                |          java-version: '21'
                |
                |      - uses: gradle/actions/setup-gradle@v6
                |
                |      # --continue runs every check, so each tool's findings reach code scanning.
                |      - name: Build and check
                |        run: ./gradlew build --continue
                |
                |      # fullAnalysis adds the CI tier: OWASP Dependency Check (whose findings are uploaded too),
                |      # dependency updates, the SBOM and licence report, and SonarQube. SonarQube needs a server
                |      # (sonarHostUrl and sonarToken, or sonarqube = false); OWASP is much faster with an NVD API key.
                |      # - name: Full analysis
                |      #   run: ./gradlew fullAnalysis --continue
                |      #   env:
                |      #     NVD_API_KEY: ${'$'}{{ secrets.NVD_API_KEY }}
                |
                |      - name: Gather SARIF reports
                |        if: always()
                |        run: ./gradlew armorSarifReport
                |
                |      - name: Upload to code scanning
                |        if: always()
                |        uses: github/codeql-action/upload-sarif@v4
                |        with:
                |          sarif_file: $sarif
                |
            """.trimMargin()
        }
    }
}
