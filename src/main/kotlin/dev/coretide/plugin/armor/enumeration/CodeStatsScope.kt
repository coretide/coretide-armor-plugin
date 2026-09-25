/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.enumeration

/** Where code stats reporting is installed. */
enum class CodeStatsScope {
    /** This repository only: sets the repository's own core.hooksPath. */
    REPO,

    /**
     * Every repository on the machine, including ones cloned later: sets the global core.hooksPath.
     * Only a developer can choose it, in ~/.gradle/gradle.properties or on the command line.
     */
    GLOBAL,
}
