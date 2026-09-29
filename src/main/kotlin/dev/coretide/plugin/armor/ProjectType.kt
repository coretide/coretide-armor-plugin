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
    val isApplication: Boolean,
    val hasKotlin: Boolean,
) {
    JAVA_APPLICATION("Java Application", isApplication = true, hasKotlin = false),
    JAVA_LIBRARY("Java Library", isApplication = false, hasKotlin = false),
    KOTLIN_APPLICATION("Kotlin Application", isApplication = true, hasKotlin = true),
    KOTLIN_LIBRARY("Kotlin Library", isApplication = false, hasKotlin = true),
    MIXED_APPLICATION("Mixed Application", isApplication = true, hasKotlin = true),
    MIXED_LIBRARY("Mixed Library", isApplication = false, hasKotlin = true),
}
