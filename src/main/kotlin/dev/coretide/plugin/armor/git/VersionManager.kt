/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.git

import dev.coretide.plugin.armor.util.LogUtil
import org.gradle.api.Project
import javax.inject.Inject

abstract class VersionManager
    @Inject
    constructor() {
        companion object {
            fun configureVersionFromGit(project: Project) {
                val versionProvider =
                    project.providers.of(GitValueSource::class.java) {
                        it.parameters.operation.set(GitOperation.VERSION)
                        it.parameters.projectDir.set(project.projectDir.absolutePath)
                    }

                project.version = versionProvider.get()
                LogUtil.essential("📋 Project version set to: ${project.version}")
            }
        }
    }
