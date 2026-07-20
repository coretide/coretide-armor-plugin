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

import dev.coretide.plugin.armor.util.FileUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

/**
 * Copies armor's default tool configs into the project's `config/` directory so they can be
 * committed and customized.
 *
 * Previously this happened implicitly during configuration. It is now opt-in: run
 * `./gradlew armorScaffoldConfigs`, then point `spotbugsConfig.excludeFile` /
 * `owaspSuppressionFile` at the generated files (or leave them unset to keep using the
 * build-directory defaults).
 *
 * Existing files are never overwritten.
 */
@UntrackedTask(because = "Scaffolds into the project tree on demand; outputs are user-owned once written")
abstract class ScaffoldConfigsTask : DefaultTask() {
    @get:org.gradle.api.tasks.Internal
    abstract val configDirectory: DirectoryProperty

    init {
        group = "build setup"
        description = "📝 Writes CodeArmor's default tool configs into config/ for customization"
    }

    @TaskAction
    fun scaffold() {
        val configDir = configDirectory.get().asFile

        val targets =
            listOf(
                configDir.resolve("spotbugs").resolve(FileUtil.SPOTBUGS_EXCLUDE_FILENAME) to
                    FileUtil.defaultSpotbugsExcludeContent(),
                configDir.resolve("owasp").resolve(FileUtil.OWASP_SUPPRESSION_FILENAME) to
                    FileUtil.defaultOwaspSuppressionContent(),
            )

        targets.forEach { (file, content) ->
            if (FileUtil.scaffoldIfAbsent(file, content)) {
                logger.lifecycle("📝 Created ${file.absolutePath}")
            } else {
                logger.lifecycle("↩️  Kept existing ${file.absolutePath}")
            }
        }
    }
}
