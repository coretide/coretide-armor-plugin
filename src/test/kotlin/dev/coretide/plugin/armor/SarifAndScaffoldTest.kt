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

import dev.coretide.plugin.armor.ArmorTestFixture.Language
import dev.coretide.plugin.armor.task.SarifReportTask
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** SARIF for code scanning, and the `.editorconfig` and workflow `armorScaffoldProject` writes. */
class SarifAndScaffoldTest {
    private fun gathered(dir: File): Map<String, String> =
        dir
            .resolve("build/reports/sarif")
            .listFiles()
            .orEmpty()
            .associate { it.name to it.readText() }

    @Test
    fun `armorSarifReport gathers each tool's SARIF with a category of its own`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeProject(dir, language = Language.KOTLIN)

        // In one build: the report task must wait for the tools, not trip Gradle's implicit dependency check.
        ArmorTestFixture.run(dir, "spotbugsMain", "detekt", "armorSarifReport")

        val reports = gathered(dir)
        assertEquals(setOf("detekt-detekt.sarif", "spotbugs-spotbugsMain.sarif"), reports.keys)
        assertContains(reports.getValue("spotbugs-spotbugsMain.sarif"), "\"automationDetails\":{\"id\":\"codearmor/spotbugs/spotbugsMain/\"}")
        assertContains(reports.getValue("detekt-detekt.sarif"), "\"automationDetails\":{\"id\":\"codearmor/detekt/detekt/\"}")
    }

    @Test
    fun `in a multi-module build each module's runs get their own category`(
        @TempDir dir: File,
    ) {
        ArmorTestFixture.writeMultiModuleProject(dir)

        ArmorTestFixture.run(dir, "spotbugsMain", "armorSarifReport", "--configuration-cache")

        val reports = gathered(dir)
        assertEquals(setOf("module-a-spotbugs-spotbugsMain.sarif", "module-b-spotbugs-spotbugsMain.sarif"), reports.keys)
        assertContains(reports.getValue("module-a-spotbugs-spotbugsMain.sarif"), "codearmor/module-a/spotbugs/spotbugsMain/")
        assertContains(reports.getValue("module-b-spotbugs-spotbugsMain.sarif"), "codearmor/module-b/spotbugs/spotbugsMain/")
    }

    @Test
    fun `categories come from where a report lies, and several runs are numbered`() {
        assertEquals("codearmor/spotbugs/spotbugsMain", SarifReportTask.category("build/reports/spotbugs/spotbugsMain.sarif"))
        assertEquals(
            "codearmor/services/api/dependency-check/dependency-check-report",
            SarifReportTask.category("services/api/build/reports/dependency-check/dependency-check-report.sarif"),
        )

        val rewritten = SarifReportTask.withCategory("""{"version":"2.1.0","runs":[{"tool":{}},{"tool":{}}]}""", "codearmor/x")

        assertContains(rewritten, "\"id\":\"codearmor/x/0\"")
        assertContains(rewritten, "\"id\":\"codearmor/x/1\"")
    }

    @Test
    fun `armorScaffoldProject writes an editorconfig and a workflow, and keeps existing files`(
        @TempDir repository: File,
    ) {
        // A Gradle build in a subdirectory of the repository: the workflow goes to the top, and runs from there.
        val app = repository.resolve("app")
        ArmorTestFixture.writeProject(app)
        ArmorTestFixture.initGitRepository(repository)

        val first = ArmorTestFixture.runWithEnvironment(app, "armorScaffoldProject", set = ArmorTestFixture.isolatedGitEnvironment)
        val workflow = repository.resolve(".github/workflows/codearmor.yml")
        workflow.writeText(workflow.readText() + "# edited\n")
        val second = ArmorTestFixture.runWithEnvironment(app, "armorScaffoldProject", set = ArmorTestFixture.isolatedGitEnvironment)

        assertContains(first.output, "Created ${workflow.canonicalFile.absolutePath}")
        assertContains(app.resolve(".editorconfig").readText(), "root = true")
        assertFalse(repository.resolve(".editorconfig").exists())
        val text = workflow.readText()
        assertContains(text, "working-directory: app")
        assertContains(text, "sarif_file: app/build/reports/sarif")
        assertContains(text, "security-events: write")
        assertTrue(text.endsWith("# edited\n"), "an existing workflow was overwritten")
        assertContains(second.output, "Kept existing")
    }

    @Test
    fun `the workflow runs from the repository's top when the build lives there`() {
        val workflow = dev.coretide.plugin.armor.task.ScaffoldProjectTask.workflow("")

        assertFalse(workflow.contains("working-directory"))
        assertContains(workflow, "sarif_file: build/reports/sarif")
    }
}
