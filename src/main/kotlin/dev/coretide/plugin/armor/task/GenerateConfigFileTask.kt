/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Materializes a tool config file (SpotBugs exclude filter, OWASP suppressions) into the build
 * directory.
 *
 * This exists so the configurators never write into the project tree while Gradle is configuring:
 * doing so mutates a file-system entry Gradle is tracking, which invalidates the configuration
 * cache on the *next* run. With generation modelled as a task, the content is a declared input and
 * the file a declared output, so the wiring is both cacheable and up-to-date checked.
 */
@CacheableTask
abstract class GenerateConfigFileTask : DefaultTask() {
    @get:Input
    abstract val content: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        group = "build setup"
        description = "Generates a CodeArmor tool configuration file"
    }

    @TaskAction
    fun generate() {
        val target = outputFile.get().asFile
        target.parentFile?.mkdirs()
        target.writeText(content.get())
    }
}
