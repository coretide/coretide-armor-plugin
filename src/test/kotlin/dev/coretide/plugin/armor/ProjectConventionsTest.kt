/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor

import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** What CodeArmor sets on SonarQube, test tasks and resources, and how it classifies a project. */
class ProjectConventionsTest {
    @Test
    fun `sonar reads SpotBugs findings from the report SpotBugs writes`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, extraScript = PRINT_SONAR + PRINT_SPOTBUGS_XML)

        val result = ArmorTestFixture.run(dir, "printSonarProperties", "printSpotbugsXml")

        val spotbugsXml = line(result, "SPOTBUGS_XML ")
        assertTrue(spotbugsXml.endsWith("spotbugsMain.xml"), spotbugsXml)
        assertEquals(spotbugsXml, sonar(result)["sonar.java.spotbugs.reportPaths"])
    }

    @Test
    fun `sonar_token is set only when configured, so SONAR_TOKEN from the environment still counts`(
        @TempDir unset: File,
        @TempDir configured: File,
    ) {
        ArmorTestFixture.writeProject(unset, extraScript = PRINT_SONAR)
        ArmorTestFixture.writeProject(configured, armorConfig = "    sonarToken = \"from-the-build\"", extraScript = PRINT_SONAR)

        val withoutToken = sonar(ArmorTestFixture.run(unset, "printSonarProperties"))
        val withToken = sonar(ArmorTestFixture.run(configured, "printSonarProperties"))

        // The SonarScanner ignores SONAR_TOKEN once sonar.token is set, even to an empty value.
        assertFalse("sonar.token" in withoutToken, "sonar.token is set: ${withoutToken["sonar.token"]}")
        assertEquals("from-the-build", withToken["sonar.token"])
    }

    @Test
    fun `sonar_host_url is set only when configured, and SONAR_HOST_URL overrides the build`(
        @TempDir unset: File,
        @TempDir configured: File,
    ) {
        ArmorTestFixture.writeProject(unset, extraScript = PRINT_SONAR)
        ArmorTestFixture.writeProject(configured, armorConfig = "    sonarHostUrl = \"https://sonar.example.com\"", extraScript = PRINT_SONAR)
        val noServer = setOf("SONAR_HOST_URL", "SONAR_TOKEN")

        val withoutHost = sonar(ArmorTestFixture.runWithEnvironment(unset, "printSonarProperties", unset = noServer))
        val withHost = sonar(ArmorTestFixture.runWithEnvironment(configured, "printSonarProperties", unset = noServer))
        val overridden =
            sonar(
                ArmorTestFixture.runWithEnvironment(
                    configured,
                    "printSonarProperties",
                    set = mapOf("SONAR_HOST_URL" to "https://sonar.ci.example.com"),
                    unset = noServer,
                ),
            )

        // Left unset, the SonarScanner picks its own default server, SonarQube Cloud.
        assertFalse("sonar.host.url" in withoutHost, "sonar.host.url is set: ${withoutHost["sonar.host.url"]}")
        assertEquals("https://sonar.example.com", withHost["sonar.host.url"])
        assertEquals("https://sonar.ci.example.com", overridden["sonar.host.url"])
    }

    @Test
    fun `sonar takes sources from the source sets and the Java version from the project`(
        @TempDir dir: File,
    ) {
        // An extra source directory, as generated or custom sources would add.
        ArmorTestFixture.writeProject(
            dir,
            extraScript =
                PRINT_SONAR +
                    """
                    sourceSets.main { java.srcDir("src/extra/java") }
                    tasks.register("printJavaVersion") {
                        val version = java.sourceCompatibility.toString()
                        doLast { println("JAVA_VERSION " + version) }
                    }
                    """.trimIndent(),
        )
        dir.resolve("src/extra/java/com/example").apply { mkdirs() }.resolve("Extra.java").writeText(
            "package com.example;\n\npublic class Extra {}\n",
        )

        val result = ArmorTestFixture.run(dir, "printSonarProperties", "printJavaVersion")

        val properties = sonar(result)
        assertContains(properties.getValue("sonar.sources"), "src${File.separator}extra${File.separator}java")
        assertContains(properties.getValue("sonar.sources"), "src${File.separator}main${File.separator}java")
        assertEquals(line(result, "JAVA_VERSION "), properties["sonar.java.source"])
        // Keys 0.2.0 made up for Kotlin; the SonarScanner does not read them.
        listOf("sonar.kotlin.source", "sonar.kotlin.target", "sonar.kotlin.binaries").forEach { key ->
            assertFalse(key in properties, "$key is set")
        }
    }

    @Test
    fun `sonarJavaVersion still overrides the Java version`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, armorConfig = """    sonarJavaVersion = "17"""", extraScript = PRINT_SONAR)

        val properties = sonar(ArmorTestFixture.run(dir, "printSonarProperties"))

        assertEquals("17", properties["sonar.java.source"])
        assertEquals("17", properties["sonar.java.target"])
    }

    @Test
    fun `test tasks on the default runner move to the JUnit Platform`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, extraScript = PRINT_TEST_FRAMEWORK)

        assertEquals("JUnitPlatformOptions", line(ArmorTestFixture.run(dir, "printTestFramework", "--warning-mode=fail"), "FRAMEWORK "))
    }

    @Test
    fun `a test task that chose TestNG keeps it`(
        @TempDir dir: File,
    ) {
        // 0.2.0 and earlier switched every test task to the JUnit Platform, TestNG included.
        ArmorTestFixture.writeProject(dir, extraScript = "tasks.test { useTestNG() }\n$PRINT_TEST_FRAMEWORK")

        assertEquals("TestNGOptions", line(ArmorTestFixture.run(dir, "printTestFramework", "--warning-mode=fail"), "FRAMEWORK "))
    }

    @Test
    fun `JUnit Platform settings a build configured are kept`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(
            dir,
            extraScript =
                """
                tasks.test { useJUnitPlatform { includeTags("fast") } }
                gradle.projectsEvaluated {
                    val tags = (tasks.test.get().options as org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions).includeTags
                    tasks.register("printTags") { doLast { println("TAGS " + tags) } }
                }
                """.trimIndent(),
        )

        assertEquals("[fast]", line(ArmorTestFixture.run(dir, "printTags", "--warning-mode=fail"), "TAGS "))
    }

    @Test
    fun `junitPlatform = false keeps JUnit 4`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, armorConfig = "    junitPlatform = false", extraScript = PRINT_TEST_FRAMEWORK)

        assertEquals("JUnitOptions", line(ArmorTestFixture.run(dir, "printTestFramework", "--warning-mode=fail"), "FRAMEWORK "))
    }

    @Test
    fun `passing tests stay quiet and failures are shown in full`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(
            dir,
            armorConfig = "    coverageMinimum = 0.0\n    coverageClassMinimum = 0.0",
            extraScript =
                """
                dependencies {
                    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
                    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
                }
                """.trimIndent(),
        )
        val tests = dir.resolve("src/test/java/com/example").apply { mkdirs() }
        tests.resolve("SampleTest.java").writeText(
            """
            package com.example;

            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.assertEquals;

            class SampleTest {
                @Test
                void adds() {
                    System.out.println("OUTPUT-FROM-A-PASSING-TEST");
                    assertEquals(3, new Sample().add(1, 2));
                }
            }
            """.trimIndent(),
        )

        val passing = ArmorTestFixture.run(dir, "test")
        assertFalse(passing.output.contains("OUTPUT-FROM-A-PASSING-TEST"), "a passing test's output was printed")
        assertFalse(passing.output.contains("SampleTest > adds() PASSED"), "passing tests were listed")

        tests.resolve("SampleTest.java").writeText(
            tests.resolve("SampleTest.java").readText().replace("assertEquals(3,", "assertEquals(4,"),
        )
        val failing = ArmorTestFixture.runAndFail(dir, "test")
        assertContains(failing.output, "SampleTest > adds() FAILED")
        // FULL: the assertion message and the frame in the test, not just the exception's class.
        assertContains(failing.output, "expected: <4> but was: <3>")
        assertContains(failing.output, "com.example.SampleTest.adds(SampleTest.java:")
    }

    @Test
    fun `a mixed Java and Kotlin application gets resource processing`(
        @TempDir dir: File,
    ) {
        // The Kotlin file only needs to exist: detection looks at the sources, and nothing compiles it.
        ArmorTestFixture.writeProject(dir, extraPlugins = listOf("application"))
        dir.resolve("src/main/kotlin/com/example").apply { mkdirs() }.resolve("Extra.kt").writeText("package com.example\n")
        dir.resolve("src/main/resources").apply { mkdirs() }.resolve("application.properties").writeText("app.version=@appVersion@\n")

        val result = ArmorTestFixture.run(dir, "processResources")

        assertContains(result.output, "Detected Mixed Application project")
        val processed = dir.resolve("build/resources/main/application.properties").readText()
        assertFalse(processed.contains("@appVersion@"), processed)
    }

    @Test
    fun `a project is an application because of its plugins, not its name`(
        @TempDir library: File,
        @TempDir application: File,
    ) {
        // 0.2.0 and earlier took any project named *-service or *-app for an application.
        ArmorTestFixture.writeProject(library)
        library.resolve("settings.gradle.kts").writeText("""rootProject.name = "billing-service"""")
        ArmorTestFixture.writeProject(application, extraPlugins = listOf("application"))
        application.resolve("settings.gradle.kts").writeText("""rootProject.name = "billing"""")

        assertContains(ArmorTestFixture.run(library, "help").output, "Detected Java Library project")
        assertContains(ArmorTestFixture.run(application, "help").output, "Detected Java Application project")
    }

    private fun line(
        result: BuildResult,
        prefix: String,
    ): String =
        result.output
            .lineSequence()
            .firstOrNull { it.startsWith(prefix) }
            ?.removePrefix(prefix)
            ?: error("No line starting with '$prefix' in:\n${result.output}")

    private fun sonar(result: BuildResult): Map<String, String> =
        result.output
            .lineSequence()
            .filter { it.startsWith("SONAR ") }
            .map { it.removePrefix("SONAR ").split("=", limit = 2) }
            .associate { it[0] to it[1] }

    private companion object {
        // The sonar task is registered after the build script runs, so it is looked up when the task executes.
        val PRINT_SONAR =
            """
            tasks.register("printSonarProperties") {
                doLast {
                    val sonarTask = project.tasks.getByName("sonar") as org.sonarqube.gradle.SonarTask
                    sonarTask.properties.get().toSortedMap().forEach { (key, value) -> println("SONAR " + key + "=" + value) }
                }
            }

            """.trimIndent() + "\n"

        val PRINT_SPOTBUGS_XML =
            """
            tasks.register("printSpotbugsXml") {
                doLast {
                    val spotbugsMain = project.tasks.getByName("spotbugsMain") as com.github.spotbugs.snom.SpotBugsTask
                    println("SPOTBUGS_XML " + spotbugsMain.reports.getByName("xml").outputLocation.get().asFile.absolutePath)
                }
            }
            """.trimIndent()

        // Read once every project is configured: CodeArmor sets up test tasks in its own afterEvaluate.
        val PRINT_TEST_FRAMEWORK =
            """
            gradle.projectsEvaluated {
                val framework = tasks.test.get().options.javaClass.simpleName.removeSuffix("_Decorated")
                tasks.register("printTestFramework") { doLast { println("FRAMEWORK " + framework) } }
            }
            """.trimIndent()
    }
}
