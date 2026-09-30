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

/** Checks as the production code compiles. Test code is not affected. */
abstract class CompilationConfig {
    /**
     * Compiler warnings fail the build: `-Xlint` with `-Werror` for Java, and `allWarningsAsErrors` with
     * `-Xjsr305=strict` for Kotlin, plus explicit API mode for Kotlin libraries. Opt-in.
     */
    abstract val strict: Property<Boolean>

    /**
     * Error Prone checks the Java sources, and its errors fail the build. It needs a JDK 21 compiler; with an older
     * one, compilation goes ahead without it, with a warning. Opt-in.
     */
    abstract val errorProne: Property<Boolean>

    /**
     * NullAway, an Error Prone check, fails the build where Java code may dereference null. It checks the project's
     * own packages and believes `@Nullable` annotations (JSpecify's, for example). Turns on Error Prone. Opt-in.
     */
    abstract val nullAway: Property<Boolean>
}
