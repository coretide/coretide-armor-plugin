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
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Writes where JaCoCo's reporting library lies in Gradle's dependency cache, one path per line, for the combined
 * coverage report on the root project.
 *
 * A list rather than a copy of the jars: the Gradle daemon keeps the class loader that loaded them open, so a copy
 * in the build directory stays locked on Windows, and neither `clean` nor anything else can delete it.
 */
@DisableCachingByDefault(because = "Writing a few paths is faster than caching them, and they differ between machines")
abstract class ListJacocoAntTask : DefaultTask() {
    /** The paths are the output, so they are part of the input too. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val library: ConfigurableFileCollection

    @get:OutputFile
    abstract val listing: RegularFileProperty

    @TaskAction
    fun write() {
        listing.get().asFile.writeText(library.files.joinToString("") { it.absolutePath + "\n" })
    }
}
