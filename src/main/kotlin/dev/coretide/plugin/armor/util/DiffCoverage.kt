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
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Coverage of the lines a change adds or modifies: the lines of `git diff --unified=0` that a JaCoCo-format XML
 * report (JaCoCo's or Kover's) has line data for. Lines without code, and files the report leaves out, do not count.
 */
object DiffCoverage {
    /** One source file's changed lines that have code, and those no test ran. */
    data class FileCoverage(
        val path: String,
        val lines: Int,
        val missed: List<Int>,
    )

    private val HUNK = Regex("""^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@""")

    /**
     * The lines each file gained or changed, by path from the repository's top, from a diff with no context
     * lines. Deleted files have none.
     */
    fun changedLines(diff: String): Map<String, Set<Int>> {
        val changed = linkedMapOf<String, MutableSet<Int>>()
        var current: MutableSet<Int>? = null
        diff.lineSequence().forEach { line ->
            when {
                line.startsWith("+++ ") -> current = targetPath(line.removePrefix("+++ "))?.let { changed.getOrPut(it) { linkedSetOf() } }
                line.startsWith("@@ ") -> {
                    val hunk = HUNK.find(line) ?: return@forEach
                    val start = hunk.groupValues[1].toInt()
                    val count = hunk.groupValues[2].ifEmpty { "1" }.toInt()
                    current?.addAll(start until start + count)
                }
            }
        }
        return changed.filterValues { it.isNotEmpty() }
    }

    /** `b/src/A.java`, quoted when git escapes the name, and followed by a tab when it holds a space. */
    private fun targetPath(header: String): String? {
        val path = header.trimEnd('\t').let { if (it.length > 1 && it.startsWith('"') && it.endsWith('"')) unquote(it) else it }
        if (path == "/dev/null") return null
        return path.removePrefix("b/")
    }

    /** A C-style quoted name, as git writes one with special characters. Octal escapes are UTF-8 bytes. */
    private fun unquote(quoted: String): String {
        val bytes = ByteArrayOutputStream()
        var i = 1
        val end = quoted.length - 1
        while (i < end) {
            val c = quoted[i]
            if (c != '\\' || i + 1 >= end) {
                bytes.write(c.toString().toByteArray(Charsets.UTF_8))
                i++
                continue
            }
            val next = quoted[i + 1]
            if (next in '0'..'3' && i + 3 < end) {
                bytes.write(quoted.substring(i + 1, i + 4).toInt(8))
                i += 4
            } else {
                bytes.write(
                    when (next) {
                        'n' -> '\n'
                        't' -> '\t'
                        else -> next
                    }.code,
                )
                i += 2
            }
        }
        return bytes.toString(Charsets.UTF_8)
    }

    /**
     * Line coverage from a JaCoCo-format report, by source file as `package/File.java`: for each line with code,
     * whether a test ran any of it.
     */
    fun lineCoverage(report: File): Map<String, Map<Int, Boolean>> {
        val document = XmlReports.parse(report)
        val coverage = mutableMapOf<String, Map<Int, Boolean>>()
        val packages = document.getElementsByTagName("package")
        for (p in 0 until packages.length) {
            val pkg = packages.item(p) as Element
            val prefix = pkg.getAttribute("name").let { if (it.isEmpty()) "" else "$it/" }
            XmlReports.children(pkg, "sourcefile").forEach { sourceFile ->
                val lines =
                    XmlReports.children(sourceFile, "line").associate {
                        it.getAttribute("nr").toInt() to ((it.getAttribute("ci").toIntOrNull() ?: 0) > 0)
                    }
                coverage[prefix + sourceFile.getAttribute("name")] = lines
            }
        }
        return coverage
    }

    /**
     * The changed lines of each source file that [coverage] measures. [changed] is keyed by path from [topLevel];
     * a file counts when it lies in one of [sourceDirectories]. A Kotlin file whose directory does not match its
     * package is found by name, when only one file in the report has that name.
     */
    fun measure(
        changed: Map<String, Set<Int>>,
        topLevel: File,
        sourceDirectories: Collection<File>,
        coverage: Map<String, Map<Int, Boolean>>,
    ): List<FileCoverage> {
        val byName = coverage.keys.groupBy { it.substringAfterLast('/') }
        // Canonical: git names the repository by its real path, and a build directory may sit behind a symbolic
        // link, as macOS's temporary directories do.
        val directories = sourceDirectories.map { it.canonicalFile }
        return changed.mapNotNull { (path, lines) ->
            val file = File(topLevel, path).canonicalFile
            val sourceDirectory = directories.firstOrNull { file.startsWith(it) } ?: return@mapNotNull null
            val relative = file.relativeTo(sourceDirectory).invariantSeparatorsPath
            val key = relative.takeIf { it in coverage } ?: byName[file.name]?.singleOrNull() ?: return@mapNotNull null
            val fileCoverage = coverage.getValue(key)
            val measured = lines.filter { it in fileCoverage }
            if (measured.isEmpty()) return@mapNotNull null
            FileCoverage(path, measured.size, measured.filter { fileCoverage[it] == false })
        }
    }
}
