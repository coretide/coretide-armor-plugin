/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.codestats

import java.io.File

/**
 * Where code stats lives on this machine.
 *
 * The configuration and state directories are the ones the standalone code-stats-hooks installers
 * use, so an existing token, identities list and delivery queue are shared. CodeArmor's scripts live
 * in a directory of their own, so CodeArmor never overwrites an install it did not make.
 *
 * Paths come from the same environment variables the hook scripts read, HOME and the XDG variables,
 * rather than Java's `user.home`, so CodeArmor and the scripts always agree.
 */
class CodeStatsPaths(
    val scriptsDir: File,
    val configDir: File,
    val stateDir: File,
) {
    val hooksDir: File get() = File(scriptsDir, "githooks")
    val library: File get() = File(scriptsDir, "lib/codestats.sh")
    val tokenFile: File get() = File(configDir, "token")
    val identitiesFile: File get() = File(configDir, "identities")

    /** What a global install replaced as `core.hooksPath`; the router hands hooks over to it. */
    val globalPreviousFile: File get() = File(configDir, "previous-hooks-path")
    val queueDir: File get() = File(stateDir, "queue")

    /** Pulses waiting to be delivered. */
    fun queuedPulses(): Int = queueDir.listFiles { file -> file.name.startsWith("pulse-") && file.name.endsWith(".ready.json") }?.size ?: 0

    companion object {
        fun fromEnvironment(environment: (String) -> String? = System::getenv): CodeStatsPaths {
            val home = environment("HOME")?.takeIf { it.isNotBlank() } ?: System.getProperty("user.home")

            fun xdg(
                variable: String,
                fallback: String,
            ): String = environment(variable)?.takeIf { it.isNotBlank() } ?: "$home/$fallback"

            return CodeStatsPaths(
                scriptsDir = File(xdg("XDG_DATA_HOME", ".local/share"), "codearmor/code-stats-hooks"),
                configDir = File(xdg("XDG_CONFIG_HOME", ".config"), "code-stats-hooks"),
                stateDir = File(xdg("XDG_STATE_HOME", ".local/state"), "code-stats-hooks"),
            )
        }
    }
}
