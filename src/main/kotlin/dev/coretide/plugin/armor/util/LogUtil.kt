/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.util

import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.enumeration.ArmorLogLevel
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger

object LogUtil {
    private var currentLogLevel: ArmorLogLevel = ArmorLogLevel.VERBOSE
    private var projectLogger: Logger? = null

    fun initialize(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        currentLogLevel = extension.logLevel // Assuming you'll add this to your extension
        projectLogger = project.logger
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
        projectLogger?.lifecycle(message) ?: println(message)
    }
}
