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

import java.io.DataInputStream
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

/**
 * The plugin is built with JDK 21 but must load in a Gradle build running on JDK 17, so its
 * classes have to target Java 17 (class file major version 61). The jdk17-consumer CI job checks
 * the same thing end to end, including the Java version the published metadata advertises.
 */
class BytecodeTargetTest {
    @Test
    fun `plugin classes target Java 17`() {
        val resource = "/" + CodeArmorPlugin::class.java.name.replace('.', '/') + ".class"
        val majorVersion =
            DataInputStream(checkNotNull(javaClass.getResourceAsStream(resource))).use { input ->
                input.readInt() // magic number
                input.readUnsignedShort() // minor version
                input.readUnsignedShort()
            }

        assertEquals(JAVA_17_CLASS_FILE_VERSION, majorVersion)
    }

    private companion object {
        const val JAVA_17_CLASS_FILE_VERSION = 61
    }
}
