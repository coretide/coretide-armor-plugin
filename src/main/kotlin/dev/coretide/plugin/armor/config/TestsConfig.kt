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

/** How tests run, and the kinds of tests CodeArmor adds. */
abstract class TestsConfig {
    /**
     * Moves test tasks still on Gradle's default JUnit 4 runner to the JUnit Platform. A task that chose TestNG, or
     * configured the JUnit Platform itself, keeps its choice.
     */
    abstract val junitPlatform: Property<Boolean>

    /**
     * On CI (`CI=true`), a failed test is re-run up to this many times. One that then passes does not fail the build,
     * but is listed as flaky. 0 turns retries off.
     */
    abstract val flakyRetries: Property<Int>

    /** Tests that take at least this long are listed after each run. 0 turns the list off. */
    abstract val slowThresholdMillis: Property<Long>

    /**
     * An `integrationTest` suite in `src/integrationTest/java` or `src/integrationTest/kotlin`, with the unit tests'
     * dependencies. It runs after the unit tests, in the build tier, and counts towards coverage. Opt-in.
     */
    abstract val integrationTests: Property<Boolean>

    /**
     * ArchUnit on the test classpath, and `armorScaffoldArchitectureTests`, which writes a first architecture test
     * (package cycles, field injection, standard streams, generic exceptions). The tests run with the others, so a
     * broken rule fails the build. Opt-in.
     */
    abstract val architectureTests: Property<Boolean>
}
