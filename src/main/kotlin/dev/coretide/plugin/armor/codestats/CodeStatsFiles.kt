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

import dev.coretide.plugin.armor.git.GitRepository
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions

/** The files a code stats install consists of, and how to recognise one. */
object CodeStatsFiles {
    /** The first lines of the router, and so of every hook in a code stats hooks directory. */
    private const val ROUTER_MARKER = "CODE-STATS-HOOKS-MANAGED-ROUTER"
    private const val RESOURCES = "/dev/coretide/plugin/armor/codestats/"
    private val TOKEN_PATTERN = Regex("[A-Za-z0-9._~-]+")
    private val isWindows = System.getProperty("os.name").startsWith("Windows")

    /**
     * Every client-side hook name Git looks up. The router answers all of them, so taking over
     * `core.hooksPath` does not silently disable any hook a developer already has.
     */
    private val HOOK_NAMES =
        listOf(
            "applypatch-msg", "pre-applypatch", "post-applypatch", "pre-commit", "pre-merge-commit",
            "prepare-commit-msg", "commit-msg", "post-commit", "pre-rebase", "post-checkout", "post-merge",
            "pre-push", "pre-auto-gc", "post-rewrite", "sendemail-validate", "push-to-checkout",
            "post-index-change", "reference-transaction", "p4-changelist", "p4-prepare-changelist",
            "p4-post-changelist", "p4-pre-submit", "pre-receive", "update", "proc-receive", "post-receive",
            "post-update",
        )

    /** True when [dir] holds code stats hooks: CodeArmor's, or a standalone code-stats-hooks install. */
    fun isCodeStatsHooksDir(dir: File): Boolean {
        val postCommit = File(dir, "post-commit")
        return postCommit.isFile && ROUTER_MARKER in postCommit.readText()
    }

    /**
     * The code stats hooks directory Git uses for [repository] right now, or null when its
     * `core.hooksPath` is unset or points at other hooks. Asks git afresh, so it reflects changes made
     * since [repository] was located.
     */
    fun activeHooksDir(repository: GitRepository): File? =
        repository.git
            .output("config", "--get", "core.hooksPath")
            ?.let(repository::resolveHooksPath)
            ?.takeIf(::isCodeStatsHooksDir)

    /** True when [dir] is the hooks directory CodeArmor installs. */
    fun isCodeArmorInstall(
        dir: File,
        paths: CodeStatsPaths,
    ): Boolean = dir.canonicalFile == paths.hooksDir.canonicalFile

    /** Writes, or refreshes, CodeArmor's copy of the scripts. */
    fun writeScripts(paths: CodeStatsPaths) {
        paths.hooksDir.mkdirs()
        paths.library.parentFile.mkdirs()
        val router = File(paths.hooksDir, "_router")
        copyResource("_router", router)
        copyResource("codestats.sh", paths.library)
        HOOK_NAMES.forEach { linkToRouter(router, File(paths.hooksDir, it)) }
    }

    /** The records a repository-scope install keeps in the repository's git directory. */
    class RepositoryRecords(
        repository: GitRepository,
    ) {
        val dir = File(repository.commonDir, "code-stats-hooks")

        /** Where the router hands hooks over to; empty means the repository's own hooks directory. */
        val previousHooksPath = File(dir, "previous-hooks-path")

        /** The repository's own `core.hooksPath` before the install, restored by uninstalling. */
        val previousLocalHooksPath = File(dir, "previous-local-hooks-path")
    }

    /**
     * Where active code stats hooks hand a hook over to in [repository]: what Git would run without
     * them. Mirrors the router's own lookup.
     */
    fun dispatchDirectory(
        repository: GitRepository,
        paths: CodeStatsPaths,
    ): File {
        val records = RepositoryRecords(repository)
        val recorded =
            when {
                records.previousHooksPath.isFile -> readFirstLine(records.previousHooksPath)
                paths.globalPreviousFile.isFile -> readFirstLine(paths.globalPreviousFile)
                else -> ""
            }
        return if (recorded.isEmpty()) repository.hooksDir else repository.resolveHooksPath(recorded)
    }

    fun readFirstLine(file: File): String = file.useLines { it.firstOrNull().orEmpty() }.removeSuffix("\r")

    /** Writes [line] and a newline: the scripts read these files line by line. */
    fun writeLine(
        file: File,
        line: String,
    ) {
        file.parentFile.mkdirs()
        file.writeText(line + "\n")
    }

    /** True for a value shaped like a Code::Stats machine token. */
    fun isValidToken(token: String): Boolean = TOKEN_PATTERN.matches(token)

    /** Stores a machine token readable only by its owner, as the standalone installers do. */
    fun writeToken(
        paths: CodeStatsPaths,
        token: String,
    ) {
        require(isValidToken(token)) { "That does not look like a Code::Stats machine token" }
        paths.configDir.mkdirs()
        val tokenPath = paths.tokenFile.toPath()
        Files.deleteIfExists(tokenPath)
        if ("posix" in FileSystems.getDefault().supportedFileAttributeViews()) {
            Files.setPosixFilePermissions(paths.configDir.toPath(), PosixFilePermissions.fromString("rwx------"))
            Files.createFile(tokenPath, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
        } else {
            Files.createFile(tokenPath)
            val file = tokenPath.toFile()
            file.setReadable(false, false)
            file.setReadable(true, true)
            file.setWritable(false, false)
            file.setWritable(true, true)
        }
        Files.writeString(tokenPath, token + "\n")
    }

    /** Writes the identities template when there is no identities file yet. */
    fun writeIdentitiesTemplate(paths: CodeStatsPaths): Boolean {
        if (paths.identitiesFile.exists()) return false
        paths.configDir.mkdirs()
        paths.identitiesFile.writeText(
            """
            |# Which commits count as your work. One per line, matched case-insensitively
            |# against the commit author's email or name. Globs allowed. Delete this file
            |# to count every commit you create locally.
            |#
            |# A cherry-pick creates a local commit carrying somebody else's authorship,
            |# so without this their work is credited to you.
            |#
            |#   you@example.com
            |#   you@your-laptop.local
            |#   12345+you@users.noreply.github.com
            |#   *@your-company.com
            |#   Your Name
            |
            """.trimMargin(),
        )
        return true
    }

    private fun copyResource(
        name: String,
        target: File,
    ) {
        val resource = checkNotNull(CodeStatsFiles::class.java.getResourceAsStream(RESOURCES + name)) { "CodeArmor is missing $name" }
        Files.deleteIfExists(target.toPath())
        resource.use { input -> target.outputStream().use { input.copyTo(it) } }
        makeExecutable(target)
    }

    /**
     * Each hook name is a relative symlink to the router, so refreshing the router updates them all.
     * Windows gets copies: its symlinks need Developer Mode or elevation. The router finds its hook
     * name from its own file name either way.
     */
    private fun linkToRouter(
        router: File,
        hook: File,
    ) {
        Files.deleteIfExists(hook.toPath())
        if (!isWindows) {
            try {
                Files.createSymbolicLink(hook.toPath(), Paths.get(router.name))
                return
            } catch (_: Exception) {
                // Fall through to a copy, for file systems without symlinks.
            }
        }
        router.copyTo(hook)
        makeExecutable(hook)
    }

    private fun makeExecutable(file: File) {
        file.setReadable(true, false)
        file.setExecutable(true, false)
    }
}
