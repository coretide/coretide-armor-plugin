/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.util

import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.enumeration.ArmorLogLevel
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logging

object LogUtil {
    /**
     * Log level is shared static state, so in a multi-module build the last project to configure
     * wins. That is acceptable for armor's own status output, but note it is not per-project.
     *
     * Deliberately a static Gradle logger rather than a retained `Project.logger`: the Gradle
     * daemon keeps this object alive between builds, so holding a Project's logger both leaks the
     * build's object graph and risks logging into an already-finished build.
     */
    @Volatile
    private var currentLogLevel: ArmorLogLevel = ArmorLogLevel.VERBOSE

    private val logger = Logging.getLogger("CodeArmor")

    fun initialize(
        @Suppress("UNUSED_PARAMETER") project: Project,
        extension: CodeArmorExtension,
    ) {
        currentLogLevel = extension.logLevel
    }

    fun verbose(message: String) {
        if (currentLogLevel == ArmorLogLevel.VERBOSE) {
            log(message)
        }
    }

    fun essential(message: String) {
        if (currentLogLevel == ArmorLogLevel.VERBOSE || currentLogLevel == ArmorLogLevel.ESSENTIAL) {
            log(message)
        }
    }

    fun verbose(
        task: Task,
        message: String,
    ) {
        if (currentLogLevel == ArmorLogLevel.VERBOSE) {
            task.logger.lifecycle(message)
        }
    }

    fun essential(
        task: Task,
        message: String,
    ) {
        if (currentLogLevel == ArmorLogLevel.VERBOSE || currentLogLevel == ArmorLogLevel.ESSENTIAL) {
            task.logger.lifecycle(message)
        }
    }

    fun verbosePrint(message: String) {
        if (currentLogLevel == ArmorLogLevel.VERBOSE) {
            println(message)
        }
    }

    fun essentialPrint(message: String) {
        if (currentLogLevel == ArmorLogLevel.VERBOSE || currentLogLevel == ArmorLogLevel.ESSENTIAL) {
            println(message)
        }
    }

    private fun log(message: String) {
        logger.lifecycle(message)
    }
}
