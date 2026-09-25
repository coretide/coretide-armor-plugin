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

import dev.coretide.plugin.armor.codestats.CodeStatsSettings
import dev.coretide.plugin.armor.enumeration.CodeStatsScope
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

/** Who may decide what: the team's build files, or each developer's own settings. */
class CodeStatsSettingsTest {
    private val enabled = CodeStatsSettings.ENABLED_PROPERTY
    private val scope = CodeStatsSettings.SCOPE_PROPERTY

    private fun resolve(
        buildEnabled: Boolean = false,
        buildScope: CodeStatsScope = CodeStatsScope.REPO,
        developer: Map<String, String> = emptyMap(),
        projectEnabled: String? = developer[enabled],
        projectScope: String? = developer[scope],
    ) = CodeStatsSettings.resolve(buildEnabled, buildScope, developer, projectEnabled, projectScope)

    @Test
    fun `off by default, in this repository`() {
        val settings = resolve()

        assertEquals(false, settings.enabled)
        assertEquals(CodeStatsScope.REPO, settings.scope)
        assertTrue(settings.warnings.isEmpty())
    }

    @Test
    fun `a build script can switch it on for its repository`() {
        assertEquals(true, resolve(buildEnabled = true).enabled)
    }

    @Test
    fun `a developer can switch it off even when the build script switches it on`() {
        assertEquals(false, resolve(buildEnabled = true, developer = mapOf(enabled to "false")).enabled)
    }

    @Test
    fun `GLOBAL from a build script falls back to REPO with a warning`() {
        val settings = resolve(buildEnabled = true, buildScope = CodeStatsScope.GLOBAL)

        assertEquals(CodeStatsScope.REPO, settings.scope)
        assertTrue(settings.warnings.single().contains("in a build script is ignored"))
    }

    @Test
    fun `GLOBAL from a developer is accepted`() {
        assertEquals(CodeStatsScope.GLOBAL, resolve(developer = mapOf(scope to "global")).scope)
    }

    @Test
    fun `a scope in the project's gradle properties is ignored`() {
        val settings = resolve(projectScope = "GLOBAL")

        assertEquals(CodeStatsScope.REPO, settings.scope)
        assertTrue(settings.warnings.single().contains("project's gradle.properties is ignored"))
    }

    @Test
    fun `the project's gradle properties can switch it on`() {
        assertEquals(true, resolve(projectEnabled = "true").enabled)
    }

    @Test
    fun `invalid values are reported and ignored`() {
        val settings = resolve(buildEnabled = true, developer = mapOf(enabled to "yes", scope to "everywhere"))

        assertEquals(true, settings.enabled)
        assertEquals(CodeStatsScope.REPO, settings.scope)
        assertEquals(2, settings.warnings.size)
    }
}
