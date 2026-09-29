/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.config

import org.gradle.api.provider.Property

/**
 * The versions of the tools CodeArmor runs, for a build that needs a newer or older one than CodeArmor's
 * default: a fix that has not reached a CodeArmor release yet, or a JDK a default does not support. Each defaults
 * to the version CodeArmor was tested with. SpotBugs has its own, in `spotbugs { toolVersion = "…" }`; detekt,
 * OWASP Dependency Check and Kover come with their Gradle plugins.
 *
 * ```kotlin
 * codeArmor {
 *     toolVersions {
 *         jacoco = "0.8.14"
 *         pitest = "1.19.0"
 *     }
 * }
 * ```
 */
abstract class ToolVersionsConfig {
    abstract val jacoco: Property<String>
    abstract val pitest: Property<String>
    abstract val errorProne: Property<String>
    abstract val nullAway: Property<String>
    abstract val archUnit: Property<String>
}
