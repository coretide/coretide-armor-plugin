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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Version derivation from git tags (`enableVersionFromGit`).
 *
 * Only the leading `v` of a tag may be dropped. Stripping every `v` turned `v1.2.0-dev` into
 * `1.2.0-de`.
 */
class GitVersionTest {
    private val printVersion =
        """
        // Registered lazily, so this runs after CodeArmor has set the version in afterEvaluate.
        tasks.register("printVersion") {
            val armorVersion = project.version.toString()
            doLast { println("VERSION=" + armorVersion) }
        }
        """.trimIndent()

    /** CI tag variables would take precedence over the repository's own tags. */
    private val ciTagVariables = setOf("CI_COMMIT_TAG", "GITHUB_REF")

    private fun writeVersionedProject(dir: File) {
        ArmorTestFixture.writeProject(
            dir,
            armorConfig = "    enableVersionFromGit = true",
            extraScript = printVersion,
        )
    }

    @Test
    fun `a v-prefixed git tag keeps every other letter`(
        @TempDir dir: File,
    ) {
        writeVersionedProject(dir)
        ArmorTestFixture.initGitRepository(dir, tag = "v1.2.0-dev")

        val result = ArmorTestFixture.runWithEnvironment(dir, "printVersion", unset = ciTagVariables)

        assertContains(result.output.lines(), "VERSION=1.2.0-dev")
    }

    @Test
    fun `a v-prefixed tag in GITHUB_REF keeps every other letter`(
        @TempDir dir: File,
    ) {
        writeVersionedProject(dir)

        val result =
            ArmorTestFixture.runWithEnvironment(
                dir,
                "printVersion",
                set = mapOf("GITHUB_REF" to "refs/tags/v2.0.0-preview"),
                unset = setOf("CI_COMMIT_TAG"),
            )

        assertContains(result.output.lines(), "VERSION=2.0.0-preview")
    }

    @Test
    fun `a v-prefixed tag in CI_COMMIT_TAG keeps every other letter`(
        @TempDir dir: File,
    ) {
        writeVersionedProject(dir)

        val result =
            ArmorTestFixture.runWithEnvironment(
                dir,
                "printVersion",
                set = mapOf("CI_COMMIT_TAG" to "v3.1.0-dev"),
                unset = setOf("GITHUB_REF"),
            )

        assertContains(result.output.lines(), "VERSION=3.1.0-dev")
    }
}
