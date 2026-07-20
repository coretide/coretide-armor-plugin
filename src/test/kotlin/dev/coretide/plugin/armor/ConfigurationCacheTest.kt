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
 * Acceptance criteria for the configuration-cache rework (A3).
 *
 * A cache *entry being stored* is not the interesting assertion — the interesting one is that a
 * second identical invocation **reuses** it, which only happens when no configuration-time state
 * leaked into task execution.
 */
class ConfigurationCacheTest {
    @Test
    fun `logExclusionInfo reuses the configuration cache on a second run`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir)

        val first = ArmorTestFixture.run(dir, "logExclusionInfo", "--configuration-cache")
        assertContains(first.output, "Configuration cache entry stored")

        val second = ArmorTestFixture.run(dir, "logExclusionInfo", "--configuration-cache")
        assertContains(second.output, "Configuration cache entry reused")
    }

    @Test
    fun `quickBuild reuses the configuration cache on a second run`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir)

        ArmorTestFixture.run(dir, "quickBuild", "--configuration-cache")
        val second = ArmorTestFixture.run(dir, "quickBuild", "--configuration-cache")

        assertContains(second.output, "Configuration cache entry reused")
    }

    @Test
    fun `multi-module build reuses the configuration cache on a second run`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeMultiModuleProject(dir)

        ArmorTestFixture.run(dir, "help", "--configuration-cache")
        val second = ArmorTestFixture.run(dir, "help", "--configuration-cache")

        assertContains(second.output, "Configuration cache entry reused")
    }
}
