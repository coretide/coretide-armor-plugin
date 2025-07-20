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

enum class ProjectType(
    val displayName: String,
) {
    JAVA_APPLICATION("Java Application"),
    JAVA_LIBRARY("Java Library"),
    KOTLIN_APPLICATION("Kotlin Application"),
    KOTLIN_LIBRARY("Kotlin Library"),
    MIXED_APPLICATION("Mixed Application"),
    MIXED_LIBRARY("Mixed Library"),
}
