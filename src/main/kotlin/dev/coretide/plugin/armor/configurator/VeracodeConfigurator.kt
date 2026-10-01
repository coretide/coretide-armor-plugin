/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.configurator

import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.util.LogUtil
import org.gradle.api.Project

object VeracodeConfigurator {
    /** What replaces `veracode = true`: the upload in the CI tier, which `fullAnalysis` runs. */
    const val REPLACEMENT = "checks { ci.add(\"veracodeUpload\") }"

    /** Whether the build switched it on; the one place CodeArmor reads the deprecated setting. */
    @Suppress("DEPRECATION")
    fun enabled(extension: CodeArmorExtension): Boolean = extension.veracode

    fun configureVeracode(project: Project) {
        project.afterEvaluate {
            val upload = project.tasks.findByName("veracodeUpload")
            if (upload == null) {
                LogUtil.essential(
                    "⚠️ CodeArmor: veracode = true, but no veracodeUpload task exists. " +
                        "Apply a Veracode Gradle plugin; until then fullAnalysis skips the Veracode scan.",
                )
                return@afterEvaluate
            }
            upload.group = "verification"
            upload.doLast {
                LogUtil.verbose("✅ Veracode upload completed")
            }
        }
    }
}
