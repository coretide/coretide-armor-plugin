/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.configurator

import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.util.LogUtil
import dev.coretide.plugin.armor.util.TestResults
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test
import org.gradle.testretry.TestRetryTaskExtension
import java.io.File
import java.util.Locale

/**
 * Flaky and slow tests. On CI a failed test is re-run; one that then passes does not fail the build, but is
 * named as flaky instead of disappearing. After every test run, tests over the slow threshold are listed.
 */
object TestReportingConfigurator {
    /** More failures than this is a broken build, not flakiness: no retries. */
    private const val MAX_FAILURES_TO_RETRY = 10

    fun configure(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        project.plugins.withType(JavaPlugin::class.java) {
            if (extension.tests.flakyRetries.get() > 0) {
                project.pluginManager.apply("org.gradle.test-retry")
            }
            val onCi = project.providers.environmentVariable("CI").map { it == "true" }.orElse(false)
            val retries = extension.tests.flakyRetries.get()
            val slowMillis = extension.tests.slowThresholdMillis.get()
            project.tasks.withType(Test::class.java).configureEach { test ->
                if (retries > 0) {
                    test.extensions.configure(TestRetryTaskExtension::class.java) { retry ->
                        retry.maxRetries.set(onCi.map { if (it) retries else 0 })
                        retry.maxFailures.set(MAX_FAILURES_TO_RETRY)
                        retry.failOnPassedAfterRetry.set(false)
                    }
                }
                val results = test.reports.junitXml.outputLocation
                val taskPath = test.path
                test.doLast {
                    summarize(taskPath, results.get().asFile, slowMillis)
                }
            }
        }
    }

    private fun summarize(
        taskPath: String,
        resultsDir: File,
        slowMillis: Long,
    ) {
        val executions = TestResults.read(resultsDir)
        val flaky = TestResults.flaky(executions)
        if (flaky.isNotEmpty()) {
            LogUtil.essential("⚠️ CodeArmor: ${flaky.size} test(s) in $taskPath failed, then passed on a retry (flaky):")
            flaky.forEach { LogUtil.essential("   • $it") }
        }
        if (slowMillis > 0) {
            val slow = TestResults.slow(executions, slowMillis)
            if (slow.isNotEmpty()) {
                LogUtil.essential("🐢 CodeArmor: slowest tests in $taskPath (over ${seconds(slowMillis)}):")
                slow.forEach { LogUtil.essential("   • ${it.id} ${seconds(it.millis)}") }
            }
        }
    }

    private fun seconds(millis: Long): String = String.format(Locale.ROOT, "%.1fs", millis / 1000.0)
}
