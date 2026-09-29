/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.util

import java.util.Properties

/** CodeArmor's own version, which the plugin's build writes into a resource. */
object PluginVersion {
    private const val RESOURCE = "/dev/coretide/plugin/armor/codearmor.properties"

    val current: String by lazy {
        PluginVersion::class.java.getResourceAsStream(RESOURCE)?.use { input ->
            Properties().apply { load(input) }.getProperty("version")
        } ?: "(unknown version)"
    }
}
