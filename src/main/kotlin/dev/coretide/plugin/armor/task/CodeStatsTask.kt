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

import dev.coretide.plugin.armor.codestats.CodeStatsPaths
import dev.coretide.plugin.armor.enumeration.CodeStatsScope
import dev.coretide.plugin.armor.git.GitCommand
import dev.coretide.plugin.armor.git.GitRepository
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

/** Common inputs of the code stats tasks: the settings in effect and the Gradle root they act on. */
@DisableCachingByDefault(because = "Code stats tasks act on git configuration and files outside the build")
abstract class CodeStatsTask : DefaultTask() {
    @get:Internal
    abstract val gradleRootDirectory: DirectoryProperty

    @get:Input
    abstract val codeStatsEnabled: Property<Boolean>

    @get:Input
    abstract val scope: Property<CodeStatsScope>

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        group = "code stats"
    }

    @get:Internal
    protected val paths: CodeStatsPaths
        get() = CodeStatsPaths.fromEnvironment()

    @get:Internal
    protected val git: GitCommand
        get() = GitCommand(execOperations, gradleRootDirectory.get().asFile)

    protected fun locateRepository(): GitRepository? = GitRepository.locate(execOperations, gradleRootDirectory.get().asFile)

    /** Code stats reports a developer's own work, so it is never installed on CI. */
    protected fun runningOnCi(): Boolean = System.getenv("CI") == "true"
}
