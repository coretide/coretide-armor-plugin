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

import java.io.File

/** The packages a project's own Java and Kotlin sources declare. */
object SourcePackages {
    private val PACKAGE = Regex("""^\s*package\s+([A-Za-z_][\w.]*)""")

    /** Every package declared by a `.java` or `.kt` file under [sourceDirs]. */
    fun declaredIn(sourceDirs: Iterable<File>): Set<String> =
        sourceDirs
            .filter { it.isDirectory }
            .flatMap { dir ->
                dir
                    .walkTopDown()
                    .filter { it.isFile && (it.extension == "java" || it.extension == "kt") }
                    .mapNotNull(::declaredPackage)
                    .toList()
            }.toSortedSet()

    /** The outermost of [packages]: `com.example` covers `com.example.util`. */
    fun roots(packages: Set<String>): Set<String> =
        packages.filter { candidate -> packages.none { other -> candidate.startsWith("$other.") } }.toSortedSet()

    private fun declaredPackage(file: File): String? =
        file.useLines { lines -> lines.firstNotNullOfOrNull { PACKAGE.find(it)?.groupValues?.get(1) } }
}
