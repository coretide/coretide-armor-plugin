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
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Git hooks are installed and removed only by explicit tasks, never while Gradle configures a build.
 *
 * Tests that push run a real `git push`; the hook then calls a stand-in `gradlew`
 * ([ArmorTestFixture.writeFakeGradlew]) instead of a nested Gradle build.
 */
class GitHooksTest {
    private val hooksEnabled = "    enableGitHooks = true"

    /** The pre-push hook CodeArmor 0.1.x wrote during configuration, trimmed to what identifies it. */
    private val legacyPrePush =
        """
        #!/bin/bash
        echo "🛡️ CodeArmor: Running pre-push security and test checks..."
        ./gradlew test --quiet --daemon
        """.trimIndent()

    /** The pre-commit hook CodeArmor 0.1.3 wrote; it calls tasks that no longer exist. */
    private val legacyPreCommit =
        """
        #!/bin/bash
        echo "🛡️ CodeArmor: Running pre-commit checks..."
        ./gradlew formatCode --quiet --daemon
        """.trimIndent()

    private fun gitProject(
        dir: File,
        armorConfig: String = hooksEnabled,
    ): File {
        ArmorTestFixture.writeProject(dir, armorConfig = armorConfig)
        ArmorTestFixture.initGitRepository(dir)
        return dir.resolve(".git/hooks")
    }

    private fun installHooks(dir: File) =
        ArmorTestFixture.runWithEnvironment(dir, "armorInstallGitHooks", set = ArmorTestFixture.isolatedGitEnvironment)

    /** Pushes HEAD to a new bare repository next to [dir], which fires the pre-push hook. */
    private fun push(dir: File): ArmorTestFixture.GitResult {
        val remote = dir.resolve("build/remote.git")
        if (!remote.exists()) {
            remote.mkdirs()
            ArmorTestFixture.git(remote, "init", "--quiet", "--bare")
        }
        return ArmorTestFixture.runGit(dir, "push", remote.absolutePath, "HEAD:refs/heads/main")
    }

    /** Commits a change to `change.txt`, which fires the commit-msg and pre-commit hooks. */
    private fun commit(
        dir: File,
        message: String,
        content: String = message,
        environment: Map<String, String> = ArmorTestFixture.isolatedGitEnvironment,
    ): ArmorTestFixture.GitResult {
        dir.resolve("change.txt").appendText("$content\n")
        ArmorTestFixture.git(dir, "add", "change.txt")
        return ArmorTestFixture.runGit(
            dir,
            "-c", "user.name=Armor Test",
            "-c", "user.email=armor@example.test",
            "-c", "commit.gpgsign=false",
            "commit", "--quiet", "--message", message,
            environment = environment,
        )
    }

    @Test
    fun `configuring a build never writes a hook`(
        @TempDir dir: File,
    ) {
        val hooks = gitProject(dir)

        ArmorTestFixture.runWithEnvironment(dir, "help", set = ArmorTestFixture.isolatedGitEnvironment)

        assertFalse(hooks.resolve("pre-push").exists(), "pre-push must only be written by armorInstallGitHooks")
    }

    @Test
    fun `armorInstallGitHooks installs a pre-push hook that runs quickBuild`(
        @TempDir dir: File,
    ) {
        val hooks = gitProject(dir)

        val result = installHooks(dir)

        val prePush = hooks.resolve("pre-push")
        assertTrue(prePush.canExecute(), "pre-push must be executable")
        assertContains(prePush.readText(), "CODEARMOR-MANAGED-HOOK")
        assertContains(prePush.readText(), "'quickBuild'")
        assertContains(result.output, "Installed the pre-push hook")
    }

    @Test
    fun `the pre-push hook lets a push through when its checks pass`(
        @TempDir dir: File,
    ) {
        gitProject(dir)
        installHooks(dir)
        ArmorTestFixture.writeFakeGradlew(dir, exitCode = 0)

        val push = push(dir)

        assertEquals(0, push.exitCode, push.output)
        assertContains(push.output, "pre-push checks passed")
        assertEquals(listOf("--quiet quickBuild"), dir.resolve("gradlew-calls.txt").readLines())
    }

    @Test
    fun `the pre-push hook blocks a push when its checks fail`(
        @TempDir dir: File,
    ) {
        gitProject(dir)
        installHooks(dir)
        ArmorTestFixture.writeFakeGradlew(dir, exitCode = 1)

        val push = push(dir)

        assertNotEquals(0, push.exitCode, "a failing check must block the push")
        assertContains(push.output, "pre-push checks failed")
        assertContains(push.output, "git push --no-verify")
    }

    @Test
    fun `checks prePush sets what the hook runs`(
        @TempDir dir: File,
    ) {
        gitProject(
            dir,
            armorConfig =
                """
                $hooksEnabled
                    checks {
                        prePush = listOf("test", "spotbugsMain")
                    }
                """.trimIndent(),
        )
        installHooks(dir)
        ArmorTestFixture.writeFakeGradlew(dir, exitCode = 0)

        push(dir)

        assertEquals(listOf("--quiet test spotbugsMain"), dir.resolve("gradlew-calls.txt").readLines())
    }

    @Test
    fun `a hook CodeArmor did not write is left alone`(
        @TempDir dir: File,
    ) {
        val hooks = gitProject(dir)
        val custom = "#!/bin/sh\necho team hook\n"
        hooks.resolve("pre-push").writeText(custom)

        val result = installHooks(dir)

        assertEquals(custom, hooks.resolve("pre-push").readText())
        assertContains(result.output, "Left the existing pre-push hook alone")
    }

    @Test
    fun `hooks from CodeArmor 0_1 are upgraded, and its obsolete pre-commit hook removed`(
        @TempDir dir: File,
    ) {
        val hooks = gitProject(dir)
        hooks.resolve("pre-push").writeText(legacyPrePush)
        hooks.resolve("pre-commit").writeText(legacyPreCommit)

        val result = installHooks(dir)

        assertContains(hooks.resolve("pre-push").readText(), "CODEARMOR-MANAGED-HOOK")
        assertFalse(hooks.resolve("pre-commit").exists())
        assertContains(result.output, "Removed the obsolete pre-commit hook")
    }

    @Test
    fun `prePushEnabled false removes CodeArmor's pre-push hook`(
        @TempDir dir: File,
    ) {
        val hooks = gitProject(dir)
        installHooks(dir)
        ArmorTestFixture.writeProject(dir, armorConfig = "$hooksEnabled\n    prePushEnabled = false")

        installHooks(dir)

        assertFalse(hooks.resolve("pre-push").exists())
    }

    @Test
    fun `armorUninstallGitHooks removes only CodeArmor's hooks`(
        @TempDir dir: File,
    ) {
        val hooks = gitProject(dir)
        installHooks(dir)
        val teamHook = "#!/bin/sh\necho team hook\n"
        hooks.resolve("pre-commit").writeText(teamHook)

        ArmorTestFixture.runWithEnvironment(dir, "armorUninstallGitHooks", set = ArmorTestFixture.isolatedGitEnvironment)

        assertFalse(hooks.resolve("pre-push").exists())
        assertEquals(teamHook, hooks.resolve("pre-commit").readText())
    }

    @Test
    fun `conventionalCommits rejects a commit message that is not a Conventional Commit`(
        @TempDir dir: File,
    ) {
        val hooks = gitProject(dir, "$hooksEnabled\n    conventionalCommits = true")
        installHooks(dir)

        val rejected = commit(dir, "updated the parser")
        val unknownType = commit(dir, "feature: accept dates without a year")
        val accepted = commit(dir, "feat(parser)!: accept dates without a year")
        val merge = commit(dir, "Merge branch 'topic'")

        assertTrue(hooks.resolve("commit-msg").canExecute())
        assertNotEquals(0, rejected.exitCode)
        assertContains(rejected.output, "the commit message is not a Conventional Commit:")
        assertContains(rejected.output, "updated the parser")
        assertNotEquals(0, unknownType.exitCode)
        assertEquals(0, accepted.exitCode, accepted.output)
        assertEquals(0, merge.exitCode, merge.output)
    }

    @Test
    fun `conventionalCommitTypes sets the types a commit may start with`(
        @TempDir dir: File,
    ) {
        gitProject(dir, "$hooksEnabled\n    conventionalCommits = true\n    conventionalCommitTypes = mutableListOf(\"feature\", \"bad type\")")
        val install = installHooks(dir)

        val accepted = commit(dir, "feature: accept dates without a year")
        val rejected = commit(dir, "feat: accept dates without a year")

        assertContains(install.output, "Left out conventionalCommitTypes that are not plain words: bad type")
        assertEquals(0, accepted.exitCode, accepted.output)
        assertNotEquals(0, rejected.exitCode)
    }

    @Test
    fun `secretScan blocks a commit gitleaks finds a secret in`(
        @TempDir dir: File,
        @TempDir tools: File,
    ) {
        val hooks = gitProject(dir, "$hooksEnabled\n    secretScan = true")
        // A stand-in gitleaks that finds a "secret" in the staged changes.
        val gitleaks =
            tools.resolve("gitleaks").apply {
                writeText(
                    """
                    #!/bin/sh
                    [ "${'$'}1 ${'$'}2" = "git --help" ] && exit 0
                    [ "${'$'}*" = "git --pre-commit --staged --redact --no-banner" ] || exit 2
                    if git diff --cached | grep -q 'AKIA'; then echo "leak found"; exit 1; fi
                    """.trimIndent() + "\n",
                )
                setExecutable(true, false)
            }
        installHooks(dir)
        val environment = ArmorTestFixture.isolatedGitEnvironment + ("CODEARMOR_GITLEAKS" to gitleaks.invariantSeparatorsPath)

        val blocked = commit(dir, "chore: add a key", content = "key = AKIAIOSFODNN7EXAMPLE", environment = environment)
        ArmorTestFixture.git(dir, "reset", "--quiet", "--hard")
        val clean = commit(dir, "chore: add a note", environment = environment)

        assertTrue(hooks.resolve("pre-commit").canExecute())
        assertNotEquals(0, blocked.exitCode)
        assertContains(blocked.output, "leak found")
        assertContains(blocked.output, "gitleaks found a secret in the staged changes")
        assertEquals(0, clean.exitCode, clean.output)
    }

    @Test
    fun `without gitleaks the secret scan warns and lets the commit through`(
        @TempDir dir: File,
    ) {
        gitProject(dir, "$hooksEnabled\n    secretScan = true")
        installHooks(dir)
        val environment = ArmorTestFixture.isolatedGitEnvironment + ("CODEARMOR_GITLEAKS" to dir.resolve("no-gitleaks").invariantSeparatorsPath)

        val result = commit(dir, "chore: add a note", environment = environment)

        assertEquals(0, result.exitCode, result.output)
        assertContains(result.output, "gitleaks is not installed")
    }

    @Test
    fun `switching a hook off removes the one CodeArmor wrote, and uninstall removes them all`(
        @TempDir dir: File,
    ) {
        val hooks = gitProject(dir, "$hooksEnabled\n    conventionalCommits = true\n    secretScan = true")
        installHooks(dir)
        assertTrue(hooks.resolve("commit-msg").exists() && hooks.resolve("pre-commit").exists())

        ArmorTestFixture.writeProject(dir, armorConfig = "$hooksEnabled\n    secretScan = true")
        val result = installHooks(dir)
        assertFalse(hooks.resolve("commit-msg").exists())
        assertContains(result.output, "conventionalCommits = false: removed CodeArmor's commit-msg hook")

        ArmorTestFixture.runWithEnvironment(dir, "armorUninstallGitHooks", set = ArmorTestFixture.isolatedGitEnvironment)
        assertFalse(hooks.resolve("pre-commit").exists())
        assertFalse(hooks.resolve("pre-push").exists())
    }

    @Test
    fun `a Gradle build in a subdirectory of the repository runs its checks from there`(
        @TempDir repo: File,
    ) {
        val app = repo.resolve("app")
        ArmorTestFixture.writeProject(app, armorConfig = hooksEnabled)
        ArmorTestFixture.initGitRepository(repo)
        installHooks(app)
        ArmorTestFixture.writeFakeGradlew(app, exitCode = 0)

        val push = push(repo)

        assertEquals(0, push.exitCode, push.output)
        assertEquals(listOf("--quiet quickBuild"), app.resolve("gradlew-calls.txt").readLines())
    }

    @Test
    fun `a linked worktree installs into the repository's shared hooks`(
        @TempDir dir: File,
    ) {
        val hooks = gitProject(dir)
        val worktree = dir.resolve("build/worktree")
        ArmorTestFixture.git(dir, "worktree", "add", "--quiet", worktree.absolutePath)

        installHooks(worktree)

        assertContains(hooks.resolve("pre-push").readText(), "CODEARMOR-MANAGED-HOOK")
    }

    @Test
    fun `warns when core hooksPath makes git ignore the repository's hooks`(
        @TempDir dir: File,
    ) {
        gitProject(dir)
        ArmorTestFixture.git(dir, "config", "core.hooksPath", ".husky")

        val result = installHooks(dir)

        assertContains(result.output, "core.hooksPath is set to .husky")
    }

    @Test
    fun `outside a git repository nothing is installed`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, armorConfig = hooksEnabled)

        val result = installHooks(dir)

        assertContains(result.output, "not inside a git work tree")
    }
}
