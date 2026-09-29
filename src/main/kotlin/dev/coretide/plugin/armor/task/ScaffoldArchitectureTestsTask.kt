/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.task

import dev.coretide.plugin.armor.util.FileUtil
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import java.io.File

/** Writes a first ArchUnit test into the project's test sources. An existing one is never overwritten. */
@UntrackedTask(because = "Scaffolds into the project's sources on demand; the file is the project's once written")
abstract class ScaffoldArchitectureTestsTask : DefaultTask() {
    /** The packages the rules cover; the test goes in the first. */
    @get:Input
    abstract val packages: ListProperty<String>

    @get:Input
    abstract val kotlin: Property<Boolean>

    @get:Internal
    abstract val testSourceDirectory: DirectoryProperty

    init {
        group = "build setup"
        description = "🏛️ Writes a first ArchUnit architecture test"
    }

    @TaskAction
    fun scaffold() {
        val packages = packages.get()
        if (packages.isEmpty()) {
            throw GradleException("No package to put the architecture test in: the sources declare none, and the project has no group")
        }
        val directory = File(testSourceDirectory.get().asFile, packages.first().replace('.', '/'))
        val file = File(directory, if (kotlin.get()) "ArchitectureTest.kt" else "ArchitectureTest.java")
        val content = if (kotlin.get()) kotlinTest(packages) else javaTest(packages)
        if (FileUtil.scaffoldIfAbsent(file, content)) {
            logger.lifecycle("🏛️ Created ${file.absolutePath}; it runs with the other tests")
        } else {
            logger.lifecycle("↩️  Kept existing ${file.absolutePath}")
        }
    }

    companion object {
        private const val HEADER =
            "Written by `./gradlew armorScaffoldArchitectureTests`: a start for the project's architecture rules.\n" +
                " * More rules: https://www.archunit.org/userguide/html/000_Index.html"

        fun javaTest(packages: List<String>): String {
            val analyzed = packages.joinToString(", ") { "\"$it\"" }
            return """
                |package ${packages.first()};
                |
                |import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
                |import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
                |import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
                |import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
                |import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
                |
                |import com.tngtech.archunit.core.importer.ImportOption;
                |import com.tngtech.archunit.junit.AnalyzeClasses;
                |import com.tngtech.archunit.junit.ArchTest;
                |import com.tngtech.archunit.lang.ArchRule;
                |
                |/**
                | * $HEADER
                | *
                | * Each rule also passes when it finds nothing to check, as in a new project.
                | */
                |@AnalyzeClasses(packages = {$analyzed}, importOptions = ImportOption.DoNotIncludeTests.class)
                |class ArchitectureTest {
                |    /** The packages directly under each top-level package do not depend on each other in a cycle. */
                |    @ArchTest
                |    static final ArchRule packagesAreFreeOfCycles =
                |        slices().matching("${packages.first()}.(*)..").should().beFreeOfCycles().allowEmptyShould(true);
                |
                |    @ArchTest
                |    static final ArchRule noFieldInjection = NO_CLASSES_SHOULD_USE_FIELD_INJECTION.allowEmptyShould(true);
                |
                |    @ArchTest
                |    static final ArchRule noStandardStreams = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.allowEmptyShould(true);
                |
                |    @ArchTest
                |    static final ArchRule noGenericExceptions = NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.allowEmptyShould(true);
                |
                |    @ArchTest
                |    static final ArchRule noJavaUtilLogging = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.allowEmptyShould(true);
                |}
                |
            """.trimMargin()
        }

        fun kotlinTest(packages: List<String>): String {
            val analyzed = packages.joinToString(", ") { "\"$it\"" }
            return """
                |package ${packages.first()}
                |
                |import com.tngtech.archunit.core.importer.ImportOption
                |import com.tngtech.archunit.junit.AnalyzeClasses
                |import com.tngtech.archunit.junit.ArchTest
                |import com.tngtech.archunit.lang.ArchRule
                |import com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
                |import com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS
                |import com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION
                |import com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING
                |import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
                |
                |/**
                | * $HEADER
                | *
                | * Each rule also passes when it finds nothing to check, as in a new project.
                | */
                |@AnalyzeClasses(packages = [$analyzed], importOptions = [ImportOption.DoNotIncludeTests::class])
                |internal class ArchitectureTest {
                |    /** The packages directly under each top-level package do not depend on each other in a cycle. */
                |    @ArchTest
                |    val packagesAreFreeOfCycles: ArchRule =
                |        slices().matching("${packages.first()}.(*)..").should().beFreeOfCycles().allowEmptyShould(true)
                |
                |    @ArchTest
                |    val noFieldInjection: ArchRule = NO_CLASSES_SHOULD_USE_FIELD_INJECTION.allowEmptyShould(true)
                |
                |    @ArchTest
                |    val noStandardStreams: ArchRule = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.allowEmptyShould(true)
                |
                |    @ArchTest
                |    val noGenericExceptions: ArchRule = NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.allowEmptyShould(true)
                |
                |    @ArchTest
                |    val noJavaUtilLogging: ArchRule = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.allowEmptyShould(true)
                |}
                |
            """.trimMargin()
        }
    }
}
