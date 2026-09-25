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

import org.gradle.api.provider.ListProperty

/**
 * Which tasks each check tier runs, as task names.
 *
 * - [prePush]: basic checks, run by the pre-push hook that `armorInstallGitHooks` installs. A failure
 *   blocks the push. Defaults to `quickBuild` (compile and unit tests).
 * - [build]: local checks that need no network. `codeQuality` runs them, and `check`, so `build`,
 *   depends on `codeQuality`. Defaults to the SpotBugs and JaCoCo tasks of the tools switched on.
 *   These must not depend on `build` themselves, or `build` would depend on itself.
 * - [ci]: checks that need a network or a server, for CI. `fullAnalysis` runs them after
 *   `codeQuality`. Defaults to OWASP Dependency Check and SonarQube, when switched on.
 *
 * The SpotBugs plugin itself also makes `check` run every SpotBugs task; `spotbugs = false` is what
 * keeps SpotBugs out of `build` entirely.
 */
abstract class ChecksConfig {
    abstract val prePush: ListProperty<String>
    abstract val build: ListProperty<String>
    abstract val ci: ListProperty<String>
}
