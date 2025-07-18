/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.armor.configurators

import org.gradle.api.Project

object VeracodeConfigurator {
    fun configureVeracode(project: Project) {
        project.afterEvaluate {
            project.tasks.findByName("veracodeUpload")?.let { task ->
                task.group = "verification"
                task.doLast {
                    println("✅ Veracode upload completed")
                }
            }
        }
    }
}
