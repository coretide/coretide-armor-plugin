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

import dev.coretide.plugin.armor.enumeration.CodeStatsScope
import org.gradle.api.provider.Property

/**
 * Reporting commit activity to Code::Stats (https://codestats.net) from git hooks.
 *
 * Off by default. A build script can switch it on for its own repository ([CodeStatsScope.REPO]).
 * A developer can override both settings in ~/.gradle/gradle.properties or with -P:
 * `codearmor.codestats.enabled` and `codearmor.codestats.scope`. [CodeStatsScope.GLOBAL] is only
 * accepted from there: a shared build file must not change every developer's global git configuration.
 *
 * Nothing is installed until `armorCodeStatsInstall` runs.
 */
abstract class CodeStatsConfig {
    abstract val enabled: Property<Boolean>
    abstract val scope: Property<CodeStatsScope>
}
