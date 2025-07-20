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

import com.diffplug.spotless.LineEnding

enum class LineEndingType(
    val spotlessLineEnding: LineEnding,
) {
    UNIX(LineEnding.UNIX),
    WINDOWS(LineEnding.WINDOWS),
    MAC_CLASSIC(LineEnding.MAC_CLASSIC),
    PLATFORM_NATIVE(LineEnding.PLATFORM_NATIVE),
    GIT_ATTRIBUTES(LineEnding.GIT_ATTRIBUTES),
}
