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

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** What a test task's JUnit XML results say about flaky and slow tests. */
object TestResults {
    data class Execution(
        val className: String,
        val name: String,
        val millis: Long,
        val failed: Boolean,
    ) {
        val id: String get() = "$className > $name"
    }

    /** Every test execution in [resultsDir]; a retried test appears once per run. */
    fun read(resultsDir: File): List<Execution> {
        val files = resultsDir.listFiles { file -> file.name.startsWith("TEST-") && file.name.endsWith(".xml") } ?: return emptyList()
        val factory =
            DocumentBuilderFactory.newInstance().apply {
                // Test results are local build output, but there is no reason to resolve anything they refer to.
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                isExpandEntityReferences = false
            }
        return files.sortedBy { it.name }.flatMap { file ->
            val cases = factory.newDocumentBuilder().parse(file).getElementsByTagName("testcase")
            (0 until cases.length).map { index ->
                val case = cases.item(index) as Element
                Execution(
                    className = case.getAttribute("classname"),
                    name = case.getAttribute("name"),
                    millis = ((case.getAttribute("time").toDoubleOrNull() ?: 0.0) * 1000).toLong(),
                    failed = case.getElementsByTagName("failure").length > 0 || case.getElementsByTagName("error").length > 0,
                )
            }
        }
    }

    /** Tests that failed and then passed on a retry. */
    fun flaky(executions: List<Execution>): List<String> =
        executions
            .groupBy { it.id }
            .filterValues { runs -> runs.any { it.failed } && runs.any { !it.failed } }
            .keys
            .sorted()

    /** The slowest passing runs over [thresholdMillis], slowest first. */
    fun slow(
        executions: List<Execution>,
        thresholdMillis: Long,
        limit: Int = 5,
    ): List<Execution> =
        executions
            .filter { !it.failed && it.millis >= thresholdMillis }
            .sortedByDescending { it.millis }
            .take(limit)
}
