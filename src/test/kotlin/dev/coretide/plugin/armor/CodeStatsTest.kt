/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Code stats reporting, end to end: installs through the armorCodeStats* tasks, then real commits
 * through the installed hooks.
 *
 * Each test gets its own [Machine]: a throwaway home directory, XDG directories and global git
 * configuration, so nothing touches the real machine. Reporting goes to an endpoint the script
 * refuses because it is not HTTPS, so pulses are produced and then stay in the queue, where the tests
 * count them; no request ever reaches Code::Stats.
 */
class CodeStatsTest {
    /** A developer machine in a temporary directory. */
    private class Machine(
        root: File,
    ) {
        val home: File = root.resolve("home").apply { mkdirs() }
        val gitConfig: File =
            home.resolve(".gitconfig").apply {
                writeText("[user]\n\tname = Test Dev\n\temail = dev@example.test\n[init]\n\tdefaultBranch = main\n")
            }
        val scriptsDir: File = home.resolve(".local/share/codearmor/code-stats-hooks")
        val hooksDir: File = scriptsDir.resolve("githooks")
        val configDir: File = home.resolve(".config/code-stats-hooks")
        val queue: File = home.resolve(".local/state/code-stats-hooks/queue")

        val environment: Map<String, String> =
            mapOf(
                "HOME" to home.path,
                "XDG_CONFIG_HOME" to home.resolve(".config").path,
                "XDG_DATA_HOME" to home.resolve(".local/share").path,
                "XDG_STATE_HOME" to home.resolve(".local/state").path,
                "GIT_CONFIG_GLOBAL" to gitConfig.path,
                "GIT_CONFIG_NOSYSTEM" to "1",
                "CODESTATS_ENDPOINT" to "http://127.0.0.1:1/never",
            )

        fun pulses(): List<File> = queue.listFiles { file -> file.name.endsWith(".ready.json") }?.toList().orEmpty()

        /** Waits for [count] pulses: reporting runs detached from the commit. */
        fun awaitPulses(count: Int): List<File> {
            repeat(120) {
                if (pulses().size >= count) return pulses()
                Thread.sleep(250)
            }
            return pulses()
        }

        /** Gives a detached reporter time to run, for asserting that it produced nothing. */
        fun settle() = Thread.sleep(SETTLE_MILLIS)

        fun git(
            dir: File,
            vararg args: String,
        ): String = ArmorTestFixture.git(dir, *args, environment = environment)

        fun globalHooksPath(): String? = ArmorTestFixture.runGit(home, "config", "--global", "--get", "core.hooksPath", environment = environment).output.trim().ifEmpty { null }

        fun localHooksPath(repo: File): String? = ArmorTestFixture.runGit(repo, "config", "--local", "--get", "core.hooksPath", environment = environment).output.trim().ifEmpty { null }

        /** Commits a new Java file, which fires post-commit. */
        fun commitJavaFile(
            repo: File,
            name: String,
        ) {
            repo.resolve("src/main/java/com/example/$name.java").apply {
                parentFile.mkdirs()
                writeText("package com.example;\n\npublic class $name {\n    int value() {\n        return 1;\n    }\n}\n")
            }
            git(repo, "add", "--all")
            git(repo, "commit", "--quiet", "--message", "add $name")
        }

        /** A hook script that records that it ran, as a developer's existing hook would. */
        fun writeRecordingHook(
            hook: File,
            log: File,
        ) {
            hook.parentFile.mkdirs()
            hook.writeText("#!/bin/sh\necho ran >> '${log.invariantSeparatorsPath}'\n")
            hook.setExecutable(true, false)
        }
    }

    private fun Machine.run(
        dir: File,
        vararg args: String,
        extra: Map<String, String> = emptyMap(),
    ): BuildResult = ArmorTestFixture.runWithEnvironment(dir, *args, set = environment + extra, unset = setOf("CI"))

    private fun project(
        dir: File,
        armorConfig: String = ENABLED,
    ): File {
        ArmorTestFixture.writeProject(dir, armorConfig = armorConfig)
        ArmorTestFixture.initGitRepository(dir)
        return dir
    }

    @Test
    fun `code stats is off by default`(
        @TempDir dir: File,
        @TempDir root: File,
    ) {
        val machine = Machine(root)
        val repo = project(dir, armorConfig = "")

        val result = machine.run(repo, "armorCodeStatsInstall")

        assertContains(result.output, "Code stats is disabled")
        assertNull(machine.localHooksPath(repo))
        assertFalse(machine.scriptsDir.exists())
    }

    @Test
    fun `a repository install reports commits and keeps the repository's own hooks running`(
        @TempDir dir: File,
        @TempDir root: File,
    ) {
        val machine = Machine(root)
        val repo = project(dir)
        val ownHookLog = root.resolve("own-hook.log")
        machine.writeRecordingHook(repo.resolve(".git/hooks/post-commit"), ownHookLog)

        val result = machine.run(repo, "armorCodeStatsInstall")
        machine.commitJavaFile(repo, "Reported")

        assertContains(result.output, "Installed code stats for this repository")
        assertEquals(machine.hooksDir.canonicalFile, File(checkNotNull(machine.localHooksPath(repo))).canonicalFile)
        assertNull(machine.globalHooksPath(), "a repository install must not touch the global configuration")
        val pulses = machine.awaitPulses(1)
        assertEquals(1, pulses.size, "the commit should have produced one pulse")
        assertContains(pulses.single().readText(), "\"language\":\"Java\"")
        assertEquals(listOf("ran"), ownHookLog.readLines(), "the repository's own post-commit hook must still run")
    }

    @Test
    fun `a repository install chains to the repository's own hooksPath, and uninstall restores it`(
        @TempDir dir: File,
        @TempDir root: File,
    ) {
        val machine = Machine(root)
        val repo = project(dir)
        val huskyLog = root.resolve("husky.log")
        machine.writeRecordingHook(repo.resolve(".husky/post-commit"), huskyLog)
        machine.git(repo, "config", "core.hooksPath", ".husky")

        machine.run(repo, "armorCodeStatsInstall")
        machine.commitJavaFile(repo, "Chained")

        assertEquals(1, machine.awaitPulses(1).size)
        assertEquals(listOf("ran"), huskyLog.readLines(), "the hooks the repository used before must still run")

        val uninstall = machine.run(repo, "armorCodeStatsUninstall")

        assertContains(uninstall.output, "core.hooksPath is .husky again")
        assertEquals(".husky", machine.localHooksPath(repo))
        assertFalse(repo.resolve(".git/code-stats-hooks").exists())
    }

    @Test
    fun `uninstall unsets core hooksPath when there was none before`(
        @TempDir dir: File,
        @TempDir root: File,
    ) {
        val machine = Machine(root)
        val repo = project(dir)
        machine.run(repo, "armorCodeStatsInstall")

        machine.run(repo, "armorCodeStatsUninstall")

        assertNull(machine.localHooksPath(repo))
    }

    @Test
    fun `GLOBAL scope in a build script is refused`(
        @TempDir dir: File,
        @TempDir root: File,
    ) {
        val machine = Machine(root)
        val repo =
            project(
                dir,
                armorConfig =
                    """
                        codeStats {
                            enabled = true
                            scope = dev.coretide.plugin.armor.enumeration.CodeStatsScope.GLOBAL
                        }
                    """.trimIndent(),
            )

        val result = machine.run(repo, "armorCodeStatsInstall")

        assertContains(result.output, "codeStats.scope = GLOBAL in a build script is ignored")
        assertNull(machine.globalHooksPath())
        assertEquals(machine.hooksDir.canonicalFile, File(checkNotNull(machine.localHooksPath(repo))).canonicalFile)
    }

    @Test
    fun `a developer's GLOBAL scope covers every repository, and uninstall restores the previous global hooksPath`(
        @TempDir dir: File,
        @TempDir root: File,
    ) {
        val machine = Machine(root)
        val repo = project(dir, armorConfig = "")
        val theirHooks = root.resolve("their-hooks")
        val theirLog = root.resolve("theirs.log")
        machine.writeRecordingHook(theirHooks.resolve("post-commit"), theirLog)
        machine.git(machine.home, "config", "--global", "core.hooksPath", theirHooks.invariantSeparatorsPath)
        val developer = arrayOf("-Pcodearmor.codestats.enabled=true", "-Pcodearmor.codestats.scope=GLOBAL")

        val install = machine.run(repo, "armorCodeStatsInstall", *developer)
        machine.commitJavaFile(repo, "Everywhere")

        assertContains(install.output, "Installed code stats for every repository")
        assertEquals(machine.hooksDir.canonicalFile, File(checkNotNull(machine.globalHooksPath())).canonicalFile)
        assertNull(machine.localHooksPath(repo))
        assertEquals(1, machine.awaitPulses(1).size)
        assertEquals(listOf("ran"), theirLog.readLines(), "the global hooks used before must still run")

        machine.run(repo, "armorCodeStatsUninstall", *developer)

        assertEquals(theirHooks.canonicalFile, File(checkNotNull(machine.globalHooksPath())).canonicalFile)
    }

    @Test
    fun `an install CodeArmor did not make is left alone`(
        @TempDir dir: File,
        @TempDir root: File,
    ) {
        val machine = Machine(root)
        val repo = project(dir)
        // Stands in for an install from the standalone code-stats-hooks installers.
        val standalone = machine.home.resolve(".local/share/code-stats-hooks/githooks")
        standalone.mkdirs()
        val router = "#!/bin/sh\n# CODE-STATS-HOOKS-MANAGED-ROUTER\nexit 0\n"
        standalone.resolve("post-commit").writeText(router)
        machine.git(machine.home, "config", "--global", "core.hooksPath", standalone.invariantSeparatorsPath)

        val repositoryInstall = machine.run(repo, "armorCodeStatsInstall")
        val globalInstall = machine.run(repo, "armorCodeStatsInstall", "-Pcodearmor.codestats.scope=GLOBAL")

        assertContains(repositoryInstall.output, "already covers this repository")
        assertContains(globalInstall.output, "which CodeArmor did not install; left alone")
        assertNull(machine.localHooksPath(repo))
        assertEquals(standalone.canonicalFile, File(checkNotNull(machine.globalHooksPath())).canonicalFile)
        assertEquals(router, standalone.resolve("post-commit").readText())
    }

    @Test
    fun `disabling code stats opts a repository out of a global install`(
        @TempDir dir: File,
        @TempDir root: File,
    ) {
        val machine = Machine(root)
        val repo = project(dir, armorConfig = "")
        machine.run(repo, "armorCodeStatsInstall", "-Pcodearmor.codestats.enabled=true", "-Pcodearmor.codestats.scope=GLOBAL")

        val optOut = machine.run(repo, "armorCodeStatsInstall", "-Pcodearmor.codestats.enabled=false")
        machine.commitJavaFile(repo, "NotCounted")
        machine.settle()

        assertContains(optOut.output, "opted this repository out")
        assertEquals("false", machine.git(repo, "config", "--get", "codestats.enabled").trim())
        assertEquals(0, machine.pulses().size, "an opted-out repository must not report")
        assertContains(machine.run(repo, "armorCodeStatsStatus", "-Pcodearmor.codestats.enabled=false").output, "opted out")

        machine.run(repo, "armorCodeStatsInstall", "-Pcodearmor.codestats.enabled=true", "-Pcodearmor.codestats.scope=GLOBAL")
        machine.commitJavaFile(repo, "CountedAgain")

        assertEquals(1, machine.awaitPulses(1).size, "enabling again must clear the opt-out")
    }

    @Test
    fun `a token from CODESTATS_API_TOKEN is stored readable only by its owner`(
        @TempDir dir: File,
        @TempDir root: File,
    ) {
        val machine = Machine(root)
        val repo = project(dir)

        machine.run(repo, "armorCodeStatsInstall", extra = mapOf("CODESTATS_API_TOKEN" to "abc.123-token"))

        val token = machine.configDir.resolve("token")
        assertEquals("abc.123-token\n", token.readText())
        if (!isWindows) {
            assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(token.toPath())))
        }
    }

    @Test
    fun `queued pulses are delivered with the stored token, even one saved without a trailing newline`(
        @TempDir dir: File,
        @TempDir root: File,
    ) {
        val machine = Machine(root)
        val repo = project(dir)
        machine.run(repo, "armorCodeStatsInstall")
        machine.configDir.resolve("token").writeText("secret-token")
        machine.queue.mkdirs()
        val now = System.currentTimeMillis() / 1000
        machine.queue.resolve("pulse-$now-abc-1.ready.json").writeText("""{"coded_at":"2026-01-01T00:00:00+00:00","xps":[]}""")
        // A stand-in curl that records the request's headers and answers as Code::Stats does.
        val curlLog = root.resolve("curl.log")
        val curl =
            root.resolve("curl").apply {
                writeText(
                    """
                    #!/bin/sh
                    previous=""
                    for argument in "${'$'}@"; do
                      [ "${'$'}previous" = "--config" ] && cat "${'$'}argument" >> '${curlLog.invariantSeparatorsPath}'
                      previous="${'$'}argument"
                    done
                    printf '201'
                    """.trimIndent() + "\n",
                )
                setExecutable(true, false)
            }

        val result =
            machine.run(
                repo,
                "armorCodeStatsFlush",
                extra =
                    mapOf(
                        "CODESTATS_CURL" to curl.invariantSeparatorsPath,
                        "CODESTATS_ENDPOINT" to "https://codestats.invalid/api/my/pulses",
                    ),
            )

        assertContains(result.output, "Delivered 1 queued pulse")
        assertEquals(0, machine.pulses().size)
        assertContains(curlLog.readText(), "X-API-Token: secret-token")
    }

    @Test
    fun `the build hints when code stats is enabled but not installed`(
        @TempDir dir: File,
        @TempDir root: File,
    ) {
        val machine = Machine(root)
        val repo = project(dir)

        val before = machine.run(repo, "help")
        machine.run(repo, "armorCodeStatsInstall")
        val after = machine.run(repo, "help")

        assertContains(before.output, "code stats is enabled but this repository is not reporting")
        assertFalse(after.output.contains("not reporting"), after.output)
    }

    @Test
    fun `nothing is installed on CI`(
        @TempDir dir: File,
        @TempDir root: File,
    ) {
        val machine = Machine(root)
        val repo = project(dir)

        val result = machine.run(repo, "armorCodeStatsInstall", extra = mapOf("CI" to "true"))

        assertContains(result.output, "CI is set")
        assertNull(machine.localHooksPath(repo))
    }

    @Test
    fun `status reports what is installed`(
        @TempDir dir: File,
        @TempDir root: File,
    ) {
        val machine = Machine(root)
        val repo = project(dir)
        machine.run(repo, "armorCodeStatsInstall")

        val result = machine.run(repo, "armorCodeStatsStatus")

        assertContains(result.output, "Code stats: enabled, scope REPO")
        assertContains(result.output, "reporting through CodeArmor's install")
    }

    @Test
    fun `CodeArmor's pre-push hook runs through the code stats hooks`(
        @TempDir dir: File,
        @TempDir root: File,
    ) {
        val machine = Machine(root)
        val repo = project(dir, armorConfig = "$ENABLED\n    enableGitHooks = true")
        machine.run(repo, "armorCodeStatsInstall")

        val hooks = machine.run(repo, "armorInstallGitHooks")
        ArmorTestFixture.writeFakeGradlew(repo, exitCode = 0)
        val remote = root.resolve("remote.git").apply { mkdirs() }
        machine.git(remote, "init", "--quiet", "--bare")
        val push = ArmorTestFixture.runGit(repo, "push", remote.absolutePath, "HEAD:refs/heads/main", environment = machine.environment)

        assertContains(hooks.output, "so CodeArmor's pre-push hook runs")
        assertEquals(0, push.exitCode, push.output)
        assertTrue(repo.resolve("gradlew-calls.txt").isFile, "the pre-push hook should have run through the router")
    }

    private companion object {
        const val ENABLED = "    codeStats {\n        enabled = true\n    }"
        val isWindows = System.getProperty("os.name").startsWith("Windows")
        val SETTLE_MILLIS = if (isWindows) 10_000L else 5_000L
    }
}
