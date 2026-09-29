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

import dev.coretide.plugin.armor.configurator.DetektConfigurator
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.gradle.api.GradleException
import org.junit.jupiter.api.Test

/** The Kotlin version guard in front of detekt. */
class DetektVersionTest {
    @Test
    fun `the versions CodeArmor states are those of the bundled detekt plugin`() {
        // detekt keeps them in an internal class; read here so that bumping detekt without updating the
        // guard fails the build instead of silently keeping the old Kotlin limit.
        val buildConfig = Class.forName("dev.detekt.detekt_gradle_plugin.BuildConfig")

        assertEquals(buildConfig.getField("DETEKT_VERSION").get(null), DetektConfigurator.DETEKT_VERSION)
        assertEquals(buildConfig.getField("KOTLIN_IMPLEMENTATION_VERSION").get(null), DetektConfigurator.DETEKT_KOTLIN_VERSION)
    }

    @Test
    fun `detekt analyses code for its own Kotlin release and older ones`() {
        assertTrue(DetektConfigurator.supports("2.4.10", "2.4.10"))
        assertTrue(DetektConfigurator.supports("2.4.20", "2.4.10"), "a patch release adds no syntax")
        assertTrue(DetektConfigurator.supports("2.2.21", "2.4.10"))
        assertTrue(DetektConfigurator.supports("1.9.24", "2.4.10"))
    }

    @Test
    fun `detekt is left off for a newer Kotlin release`() {
        assertFalse(DetektConfigurator.supports("2.5.0", "2.4.10"))
        assertFalse(DetektConfigurator.supports("2.5.0-Beta1", "2.4.10"))
        assertFalse(DetektConfigurator.supports("3.0.0", "2.4.10"))
    }

    @Test
    fun `an unreadable version does not switch detekt off`() {
        assertTrue(DetektConfigurator.supports("unknown", "2.4.10"))
    }

    @Test
    fun `a class-loading failure is found behind Gradle's wrapping`() {
        val missing = NoClassDefFoundError("org/jetbrains/kotlin/gradle/dsl/KotlinJvmProjectExtension")
        val wrapped = GradleException("Failed to apply plugin 'dev.detekt'.", RuntimeException("while registering tasks", missing))

        assertSame(missing, DetektConfigurator.missingClass(wrapped))
        assertNull(DetektConfigurator.missingClass(GradleException("something else")))
    }
}
