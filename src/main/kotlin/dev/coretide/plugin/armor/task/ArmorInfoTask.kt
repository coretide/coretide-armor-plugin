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
import dev.coretide.plugin.armor.configurator.DetektConfigurator
import dev.coretide.plugin.armor.configurator.DiffCoverageConfigurator
import dev.coretide.plugin.armor.configurator.KoverConfigurator
import dev.coretide.plugin.armor.configurator.OwaspConfigurator
import dev.coretide.plugin.armor.configurator.SonarqubeConfigurator
import dev.coretide.plugin.armor.git.GitHooksManager
import dev.coretide.plugin.armor.git.GitRepository
import dev.coretide.plugin.armor.util.PluginVersion
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import java.io.File
import java.util.Locale
import javax.inject.Inject

/**
 * Prints what CodeArmor does in a project: the check tiers, the tools switched on and off, and anything in the
 * setup that keeps a check from doing its job.
 *
 * The lines are rendered at configuration time, so the task body touches no `Project` state. The git hooks and
 * gitleaks are looked up when the task runs: they change outside the build.
 */
@UntrackedTask(because = "Prints the setup; it has no outputs and must run whenever asked")
abstract class ArmorInfoTask : DefaultTask() {
    @get:Input
    abstract val lines: ListProperty<String>

    @get:Input
    abstract val warnings: ListProperty<String>

    /** The Gradle root directory, whose git repository the hooks belong to. Unset where hooks are not checked. */
    @get:Internal
    abstract val gitRootDirectory: DirectoryProperty

    /** The hooks `armorInstallGitHooks` would install. */
    @get:Input
    abstract val expectedHooks: ListProperty<String>

    /** Whether the pre-commit hook scans for secrets, which needs gitleaks. */
    @get:Input
    abstract val secretScan: Property<Boolean>

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        group = "help"
        description = "🛡️ Prints what CodeArmor checks here: the check tiers, the tools, and what needs attention"
        expectedHooks.convention(emptyList())
        secretScan.convention(false)
    }

    @TaskAction
    fun print() {
        val found = warnings.get() + hookWarnings() + gitleaksWarnings()
        lines.get().forEach { logger.lifecycle(it) }
        logger.lifecycle("")
        if (found.isEmpty()) {
            logger.lifecycle("✅ Nothing needs attention")
        } else {
            logger.lifecycle("Needs attention")
            found.forEach { logger.lifecycle("  ⚠️  $it") }
        }
    }

    private fun hookWarnings(): List<String> {
        val hooks = expectedHooks.get()
        val root = gitRootDirectory.orNull?.asFile
        if (hooks.isEmpty() || root == null) return emptyList()
        val repository = GitRepository.locate(execOperations, root) ?: return emptyList()
        // A hook from CodeArmor 0.1.x is replaced on the next install, so it counts as missing.
        val missing =
            hooks.filterNot { name ->
                val hook = File(repository.hooksDir, name)
                GitHooksManager.isManagedByCodeArmor(hook) && !GitHooksManager.isLegacy(hook)
            }
        if (missing.isEmpty()) return emptyList()
        return listOf("Git hooks not installed: ${missing.joinToString()}. Run ./gradlew armorInstallGitHooks")
    }

    private fun gitleaksWarnings(): List<String> {
        if (!secretScan.get()) return emptyList()
        val command = System.getenv("CODEARMOR_GITLEAKS")?.takeIf { it.isNotBlank() } ?: "gitleaks"
        if (findExecutable(command, System.getenv("PATH").orEmpty()) != null) return emptyList()
        return listOf(
            "secretScan is on, but gitleaks is not installed, so the pre-commit hook lets commits through " +
                "unscanned, and armorSecretScan fails. Install it: https://github.com/gitleaks/gitleaks#installing",
        )
    }

    /** The check tiers a JVM project runs. */
    data class Tiers(
        val build: List<String>,
        val ci: List<String>,
    )

    companion object {
        const val TASK_NAME = "armorInfo"

        private const val KOTLIN_JVM_PLUGIN_ID = "org.jetbrains.kotlin.jvm"
        private const val SONAR_TASK = "sonar"
        private const val OWASP_TASK = "dependencyCheckAnalyze"

        private val isWindows = System.getProperty("os.name").lowercase(Locale.ROOT).startsWith("windows")

        /**
         * Registers `armorInfo` in [project]. A module shows its tiers and tools, given [tiers] (null where the
         * project has no Java plugin); the project CodeArmor is applied to, [repository], also shows its git hooks
         * and checks that they are installed. [kind] says what the project is.
         */
        fun register(
            project: Project,
            extension: CodeArmorExtension,
            kind: String,
            tiers: Tiers?,
            module: Boolean,
            repository: Boolean,
            modules: List<Project> = emptyList(),
        ) {
            project.tasks.register(TASK_NAME, ArmorInfoTask::class.java) { task ->
                // Rendered once, and only when armorInfo is about to run.
                val setup by lazy { render(project, extension, kind, tiers, module, repository, modules) }
                task.lines.set(project.provider { setup.first })
                task.warnings.set(project.provider { setup.second })
                if (repository && extension.enableGitHooks) {
                    task.gitRootDirectory.set(project.rootDir)
                    task.expectedHooks.set(project.provider { expectedHooks(extension) })
                }
                if (repository) task.secretScan.set(project.provider { extension.secretScan })
            }
        }

        /** The lines and the warnings `armorInfo` prints. */
        fun render(
            project: Project,
            extension: CodeArmorExtension,
            kind: String,
            tiers: Tiers?,
            module: Boolean,
            repository: Boolean,
            modules: List<Project> = emptyList(),
        ): Pair<List<String>, List<String>> {
            val lines = mutableListOf<String>()
            val warnings = mutableListOf<String>()
            val name = if (project.parent == null) project.name else project.path
            lines += "🛡️ CodeArmor ${PluginVersion.current} · $name ($kind)"
            if (modules.isNotEmpty()) {
                lines += ""
                lines += "Modules, each with its own armorInfo: ${modules.joinToString { it.path }}"
            }
            val checks = mutableListOf<String>()
            if (module) {
                if (tiers == null) {
                    checks += "  none: the check tiers need the Java or Kotlin JVM plugin"
                } else {
                    checks += "  build, through codeQuality: ${tiers.build.joinToString().ifEmpty { "nothing" }}"
                    checks += "  CI, through fullAnalysis: ${tiers.ci.joinToString().ifEmpty { "nothing" }}"
                }
            }
            if (repository && extension.enableGitHooks && extension.prePushEnabled) {
                checks += "  pre-push hook: ${extension.checks.prePush.get().joinToString().ifEmpty { "nothing" }}"
            }
            if (checks.isNotEmpty()) {
                lines += ""
                lines += "Checks"
                lines += checks
            }
            val on = mutableListOf<String>()
            val off = mutableListOf<String>()
            val plainOff = mutableListOf<String>()
            if (module && tiers != null) {
                tools(project, extension, tiers, on, off, plainOff, warnings)
            }
            if (repository) {
                if (extension.enableGitHooks) {
                    val hooks =
                        expectedHooks(extension).map {
                            when (it) {
                                "commit-msg" -> "commit-msg (Conventional Commits)"
                                "pre-commit" -> "pre-commit (secret scan)"
                                else -> it
                            }
                        }
                    if (hooks.isEmpty()) plainOff += "git hooks" else on += "Git hooks: ${hooks.joinToString()}"
                } else {
                    plainOff += "git hooks"
                }
                if (extension.secretScan) on += "Secret scan with gitleaks: the history, in fullAnalysis (armorSecretScan)"
            }
            if (on.isNotEmpty() || off.isNotEmpty() || plainOff.isNotEmpty()) {
                lines += ""
                lines += "Tools"
                on.forEach { lines += "  ✅ $it" }
                off.forEach { lines += "  ➖ $it" }
                if (plainOff.isNotEmpty()) {
                    lines += "  ➖ Off: ${plainOff.joinToString()}"
                }
            }
            return lines to warnings
        }

        private fun tools(
            project: Project,
            extension: CodeArmorExtension,
            tiers: Tiers,
            on: MutableList<String>,
            off: MutableList<String>,
            plainOff: MutableList<String>,
            warnings: MutableList<String>,
        ) {
            val kotlin = project.plugins.hasPlugin(KOTLIN_JVM_PLUGIN_ID)
            val minimum = percent(extension.coverageMinimum)
            val classMinimum = percent(extension.coverageClassMinimum)
            // JaCoCo's overall rule counts instructions, its default; the per-class rule and Kover's count lines.
            val versions = extension.toolVersions
            val jacoco = "JaCoCo ${versions.jacoco.get()}: at least $minimum of instructions, $classMinimum of lines per class"

            if (extension.spotbugs) {
                val baseline = project.file(extension.spotbugsConfig.baselineFile).takeIf { it.isFile }
                on += "SpotBugs ${extension.spotbugsConfig.toolVersion}" +
                    (baseline?.let { ", with a baseline of ${SpotbugsBaselineTask.count(it)} accepted findings" } ?: "")
            } else {
                plainOff += "SpotBugs"
            }
            when {
                KoverConfigurator.usesKover(project, extension) -> on += "Kover: at least $minimum of lines, $classMinimum per class"
                extension.kover -> on += "$jacoco (kover = true, but no Kotlin here)"
                extension.jacoco -> on += jacoco
                else -> plainOff += "coverage"
            }
            if (DiffCoverageConfigurator.enabled(extension)) {
                on += "Diff coverage: " +
                    (extension.diffCoverageMinimum?.let { "at least ${percent(it)} of changed lines" } ?: "reports only")
            } else {
                plainOff += "diff coverage"
            }
            when {
                !extension.detekt -> plainOff += "detekt"
                kotlin -> on += "detekt ${DetektConfigurator.DETEKT_VERSION}" + if (extension.detektTypeResolution) ", with type resolution (detektMain)" else ""
                else -> off += "detekt: no Kotlin here"
            }
            if (extension.errorProne || extension.nullAway) {
                on += "Error Prone ${versions.errorProne.get()}" +
                    if (extension.nullAway) ", with NullAway ${versions.nullAway.get()}" else ""
            } else {
                plainOff += "Error Prone"
            }
            if (extension.integrationTests) on += "Integration tests: src/integrationTest, in build" else plainOff += "integration tests"
            if (extension.strictCompilation) on += "Strict compilation: warnings fail the build" else plainOff += "strict compilation"
            if (extension.mutationTesting) {
                on += "Mutation testing with PIT ${versions.pitest.get()}" +
                    if (extension.mutationThreshold > 0) ": at least ${extension.mutationThreshold}% of mutations killed" else ""
            } else {
                plainOff += "mutation testing"
            }
            if (extension.architectureTests) {
                on += "Architecture tests with ArchUnit ${versions.archUnit.get()}"
            } else {
                plainOff += "architecture tests"
            }
            if (extension.kotlinAbiValidation) on += "Kotlin ABI validation" else plainOff += "Kotlin ABI validation"
            if (!extension.apiBaseline.isNullOrBlank()) on += "API check against ${extension.apiBaseline}" else plainOff += "API check"

            if (extension.owasp) {
                val nvdKey = OwaspConfigurator.nvdApiKey(project, extension) != null
                on += "OWASP Dependency-Check: fails at CVSS ${extension.owaspFailBuildOnCVSS}" + if (nvdKey) ", with an NVD API key" else ""
                if (!nvdKey && OWASP_TASK in tiers.ci) {
                    warnings +=
                        "OWASP has no NVD API key, so downloading the vulnerability database is slow: an hour or more " +
                        "the first time. Get a free key at https://nvd.nist.gov/developers/request-an-api-key " +
                        "and set NVD_API_KEY"
                }
            } else {
                plainOff += "OWASP"
            }
            if (extension.dependencyUpdates) on += "Dependency updates" else plainOff += "dependency updates"
            if (extension.sbom) on += "SBOM and licence report" else plainOff += "SBOM"
            if (extension.dependencyAnalysis) on += "Dependency analysis" else plainOff += "dependency analysis"

            val sonarConfigured = SonarqubeConfigurator.isConfigured(project, extension)
            when {
                !extension.sonarqube -> plainOff += "SonarQube"
                sonarConfigured -> on += "SonarQube: ${SonarqubeConfigurator.hostUrl(extension) ?: "SonarQube Cloud"}"
                SONAR_TASK in tiers.ci -> {
                    on += "SonarQube, listed in checks.ci"
                    warnings +=
                        "sonar is in checks.ci, but no SonarQube server or token is configured, so it would send the " +
                        "analysis to SonarQube Cloud without a token. Set sonarHostUrl or SONAR_HOST_URL, and SONAR_TOKEN"
                }
                else -> {
                    off += "SonarQube: no server configured"
                    warnings +=
                        "SonarQube is on, but no server or token is configured, so fullAnalysis leaves it out. Set " +
                        "sonarHostUrl or SONAR_HOST_URL, or SONAR_TOKEN alone for SonarQube Cloud; or sonarqube = false"
                }
            }
            if (extension.veracode) {
                on += "Veracode, through your Veracode plugin's veracodeUpload"
                if ("veracodeUpload" !in project.tasks.names) {
                    warnings += "veracode = true, but there is no veracodeUpload task: apply your Veracode Gradle plugin"
                }
                val credentials =
                    listOf("VERACODE_USERNAME", "VERACODE_PASSWORD").all {
                        project.providers.environmentVariable(it).isPresent
                    }
                if (!credentials) {
                    warnings += "VERACODE_USERNAME and VERACODE_PASSWORD are not both set, so fullAnalysis skips the Veracode upload"
                }
            } else {
                plainOff += "Veracode"
            }
        }

        /** The hooks `armorInstallGitHooks` installs with these settings. */
        fun expectedHooks(extension: CodeArmorExtension): List<String> =
            buildList {
                if (extension.prePushEnabled) add("pre-push")
                if (extension.conventionalCommits) add("commit-msg")
                if (extension.secretScan) add("pre-commit")
            }

        /**
         * [command] as a file: a path as it is, or a name looked up in [path], the way a shell would. On Windows
         * the usual executable extensions are tried too. Null when there is no such executable.
         */
        fun findExecutable(
            command: String,
            path: String,
            windows: Boolean = isWindows,
        ): File? {
            val names = if (windows) listOf(command, "$command.exe", "$command.cmd", "$command.bat") else listOf(command)
            val candidates =
                if ('/' in command || File.separatorChar in command) {
                    names.map(::File)
                } else {
                    path.split(File.pathSeparator).filter { it.isNotEmpty() }.flatMap { dir -> names.map { File(dir, it) } }
                }
            return candidates.firstOrNull { it.isFile && it.canExecute() }
        }

        private fun percent(fraction: Double): String {
            val value = fraction * 100
            return if (value == Math.rint(value)) "${value.toInt()}%" else String.format(Locale.ROOT, "%.1f%%", value)
        }
    }
}
