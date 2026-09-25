/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.codestats

import dev.coretide.plugin.armor.codestats.CodeStatsFiles.RepositoryRecords
import dev.coretide.plugin.armor.git.GitCommand
import dev.coretide.plugin.armor.git.GitRepository
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File

/**
 * Installs and removes code stats reporting.
 *
 * Installing takes over `core.hooksPath` at one scope and records what it replaced, so the router can
 * keep running those hooks and uninstalling can put the setting back. An install CodeArmor did not
 * make, such as one from the standalone code-stats-hooks installers, is never modified.
 */
class CodeStatsInstaller(
    private val paths: CodeStatsPaths,
    private val logger: Logger,
) {
    /** Stores [environmentToken] when given, and otherwise reports whether a token is in place. */
    fun ensureToken(environmentToken: String?) {
        when {
            environmentToken != null -> {
                if (!CodeStatsFiles.isValidToken(environmentToken)) {
                    throw GradleException("CODESTATS_API_TOKEN does not look like a Code::Stats machine token")
                }
                CodeStatsFiles.writeToken(paths, environmentToken)
                logger.lifecycle("🔑 Stored the token from CODESTATS_API_TOKEN in ${paths.tokenFile} (readable only by you)")
            }
            paths.tokenFile.isFile -> logger.lifecycle("🔑 Using the token in ${paths.tokenFile}")
            else ->
                logger.warn(
                    "⚠️ No Code::Stats token yet. Pulses queue until you store your machine token " +
                        "(https://codestats.net/my/machines) in ${paths.tokenFile}, or rerun with CODESTATS_API_TOKEN set.",
                )
        }
        if (CodeStatsFiles.writeIdentitiesTemplate(paths)) {
            logger.lifecycle("📝 Wrote ${paths.identitiesFile}: list your email addresses there so cherry-picks of others' work do not count")
        }
    }

    fun installRepository(repository: GitRepository) {
        val git = repository.git
        clearOptOut(repository)
        val local = git.output("config", "--local", "--get", "core.hooksPath")
        val global = git.output("config", "--global", "--get", "core.hooksPath")
        val localDir = local?.let(repository::resolveHooksPath)
        val globalDir = global?.let(repository::resolveHooksPath)
        when {
            localDir != null && CodeStatsFiles.isCodeArmorInstall(localDir, paths) -> {
                CodeStatsFiles.writeScripts(paths)
                logger.lifecycle("📊 Code stats is already installed for this repository; refreshed the scripts in ${paths.scriptsDir}")
            }
            localDir != null && CodeStatsFiles.isCodeStatsHooksDir(localDir) ->
                logger.lifecycle("📊 This repository already reports through code stats hooks in $localDir, which CodeArmor did not install; left alone")
            localDir == null && globalDir != null && CodeStatsFiles.isCodeStatsHooksDir(globalDir) ->
                logger.lifecycle("📊 The global code stats install in $globalDir already covers this repository; nothing to install")
            else -> {
                CodeStatsFiles.writeScripts(paths)
                val records = RepositoryRecords(repository)
                // What Git ran before: the repository's own hooksPath, else the global one, else the
                // repository's own hooks directory (recorded as an empty line).
                CodeStatsFiles.writeLine(records.previousHooksPath, local ?: global ?: "")
                if (local != null) {
                    CodeStatsFiles.writeLine(records.previousLocalHooksPath, local)
                } else {
                    records.previousLocalHooksPath.delete()
                }
                setHooksPath(git, "--local")
                logger.lifecycle("📊 Installed code stats for this repository: core.hooksPath now points at ${paths.hooksDir}")
                (local ?: global)?.let { logger.lifecycle("   The hooks in $it still run first.") }
                if (local != null) {
                    logger.lifecycle(
                        "   A tool that rewrites core.hooksPath, such as husky on npm install, switches code stats off " +
                            "again; rerun armorCodeStatsInstall after it does.",
                    )
                }
            }
        }
    }

    /** Returns false when this repository has no CodeArmor install to remove; [quiet] skips saying so. */
    fun uninstallRepository(
        repository: GitRepository,
        quiet: Boolean = false,
    ): Boolean {
        val git = repository.git
        val local = git.output("config", "--local", "--get", "core.hooksPath")
        if (local == null || !CodeStatsFiles.isCodeArmorInstall(repository.resolveHooksPath(local), paths)) {
            if (!quiet) logger.lifecycle("ℹ️ CodeArmor has not installed code stats for this repository")
            return false
        }
        val records = RepositoryRecords(repository)
        val previousLocal = records.previousLocalHooksPath.takeIf { it.isFile }?.let(CodeStatsFiles::readFirstLine)
        if (previousLocal.isNullOrEmpty()) {
            git.run("config", "--local", "--unset", "core.hooksPath")
            logger.lifecycle("🧹 Removed code stats from this repository")
        } else {
            git.run("config", "--local", "core.hooksPath", previousLocal)
            logger.lifecycle("🧹 Removed code stats from this repository; core.hooksPath is $previousLocal again")
        }
        records.dir.deleteRecursively()
        return true
    }

    fun installGlobal(
        git: GitCommand,
        repository: GitRepository?,
    ) {
        repository?.let(::clearOptOut)
        val global = git.output("config", "--global", "--get", "core.hooksPath")
        val globalDir = global?.let { File(it) }
        when {
            globalDir != null && CodeStatsFiles.isCodeArmorInstall(globalDir, paths) -> {
                CodeStatsFiles.writeScripts(paths)
                logger.lifecycle("📊 Code stats is already installed for every repository; refreshed the scripts in ${paths.scriptsDir}")
            }
            globalDir != null && CodeStatsFiles.isCodeStatsHooksDir(globalDir) ->
                logger.lifecycle("📊 Code stats is already installed globally from $globalDir, which CodeArmor did not install; left alone")
            else -> {
                CodeStatsFiles.writeScripts(paths)
                if (global != null) {
                    CodeStatsFiles.writeLine(paths.globalPreviousFile, global)
                } else {
                    paths.globalPreviousFile.delete()
                }
                setHooksPath(git, "--global")
                logger.lifecycle("📊 Installed code stats for every repository on this machine: the global core.hooksPath is ${paths.hooksDir}")
                global?.let { logger.lifecycle("   The hooks in $it still run first.") }
            }
        }
        val local = repository?.git?.output("config", "--local", "--get", "core.hooksPath")
        if (repository != null && local != null && !CodeStatsFiles.isCodeStatsHooksDir(repository.resolveHooksPath(local))) {
            logger.warn(
                "⚠️ This repository sets its own core.hooksPath ($local), which overrides the global one, so code stats " +
                    "does not run here. Install it for this repository too: " +
                    "./gradlew armorCodeStatsInstall -P${CodeStatsSettings.SCOPE_PROPERTY}=REPO",
            )
        }
    }

    fun uninstallGlobal(git: GitCommand) {
        val global = git.output("config", "--global", "--get", "core.hooksPath")
        if (global == null || !CodeStatsFiles.isCodeArmorInstall(File(global), paths)) {
            logger.lifecycle("ℹ️ The global core.hooksPath (${global ?: "unset"}) is not CodeArmor's code stats install; left alone")
            return
        }
        val previous = paths.globalPreviousFile.takeIf { it.isFile }?.let(CodeStatsFiles::readFirstLine)
        if (previous.isNullOrEmpty()) {
            git.run("config", "--global", "--unset", "core.hooksPath")
            logger.lifecycle("🧹 Removed code stats from every repository on this machine")
        } else {
            git.run("config", "--global", "core.hooksPath", previous)
            logger.lifecycle("🧹 Removed code stats from every repository on this machine; the global core.hooksPath is $previous again")
        }
        paths.globalPreviousFile.delete()
        logger.lifecycle("   Kept the scripts in ${paths.scriptsDir} for repository installs, and your token and settings in ${paths.configDir}")
    }

    /** Stops reporting from [repository] while leaving a global install in place. */
    fun optOut(repository: GitRepository) {
        repository.git.run("config", "--local", "codestats.enabled", "false")
    }

    private fun clearOptOut(repository: GitRepository) {
        if (repository.git.output("config", "--local", "--get", "codestats.enabled") == "false") {
            repository.git.run("config", "--local", "--unset", "codestats.enabled")
        }
    }

    private fun setHooksPath(
        git: GitCommand,
        scope: String,
    ) {
        if (!git.run("config", scope, "core.hooksPath", paths.hooksDir.invariantSeparatorsPath)) {
            throw GradleException("Could not set core.hooksPath with git config $scope")
        }
    }
}
