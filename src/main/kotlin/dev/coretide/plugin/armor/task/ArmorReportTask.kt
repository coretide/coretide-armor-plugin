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

import dev.coretide.plugin.armor.util.LogUtil
import dev.coretide.plugin.armor.util.ReportSummary
import dev.coretide.plugin.armor.util.ReportSummary.Row
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import java.io.File

/**
 * One page, `build/reports/codearmor/index.html`, summing up what the tools reported, with links to their own
 * reports; and the same in a few lines on the console. It reads what exists and runs no tool itself.
 */
@UntrackedTask(because = "Sums up whatever reports exist at the time; cheap, and always current")
abstract class ArmorReportTask : DefaultTask() {
    /** The projects to sum up, by path, in the same order as [buildDirectories]. */
    @get:Internal
    abstract val projectPaths: ListProperty<String>

    @get:Internal
    abstract val buildDirectories: ListProperty<Directory>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun summarize() {
        val sections =
            projectPaths.get().zip(buildDirectories.get()).map { (path, buildDir) -> path to ReportSummary.rows(buildDir.asFile) }
        val page = report.get().asFile
        page.parentFile.mkdirs()
        page.writeText(render(sections, page.parentFile))

        val withRows = sections.filter { it.second.isNotEmpty() }
        if (withRows.isEmpty()) {
            LogUtil.essential(this, "📋 CodeArmor summary: no reports yet; run the checks first, for example ./gradlew build")
            return
        }
        LogUtil.essential(this, "📋 CodeArmor summary: ${page.path}")
        withRows.forEach { (path, rows) ->
            if (sections.size > 1) LogUtil.essential(this, "   $path")
            rows.forEach { LogUtil.essential(this, "   ${it.status.icon} ${it.tool}: ${it.result}") }
        }
        // On GitHub Actions, the same on the run's page.
        System.getenv("GITHUB_STEP_SUMMARY")?.takeIf { it.isNotBlank() }?.let { summary ->
            File(summary).appendText(markdown(withRows, headings = sections.size > 1))
        }
    }

    companion object {
        /** The summary as GitHub-flavoured Markdown, for a GitHub Actions job summary. */
        fun markdown(
            sections: List<Pair<String, List<Row>>>,
            headings: Boolean,
        ): String =
            buildString {
                appendLine("### 🛡️ CodeArmor summary")
                appendLine()
                sections.forEach { (path, rows) ->
                    if (headings) {
                        appendLine("**${cell(path)}**")
                        appendLine()
                    }
                    appendLine("| | Tool | Result |")
                    appendLine("|---|---|---|")
                    rows.forEach { appendLine("| ${it.status.icon} | ${cell(it.tool)} | ${cell(it.result)} |") }
                    appendLine()
                }
            }

        /** Text in a Markdown table cell: no column breaks, line breaks or HTML. */
        private fun cell(text: String): String =
            text.replace("\\", "\\\\").replace("|", "\\|").replace("<", "&lt;").replace("\n", " ")

        fun render(
            sections: List<Pair<String, List<Row>>>,
            directory: File,
        ): String =
            buildString {
                appendLine("<!doctype html>")
                appendLine("<html lang=\"en\"><head><meta charset=\"utf-8\">")
                appendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                appendLine("<title>CodeArmor summary</title>")
                appendLine("<style>$STYLE</style></head><body><main>")
                appendLine("<h1>CodeArmor summary</h1>")
                sections.forEach { (path, rows) ->
                    if (sections.size > 1) appendLine("<h2>${escape(path)}</h2>")
                    if (rows.isEmpty()) {
                        appendLine("<p class=\"empty\">No reports yet. Run the checks first, for example <code>./gradlew build</code>.</p>")
                    } else {
                        appendLine("<table><thead><tr><th></th><th>Tool</th><th>Result</th><th>Report</th></tr></thead><tbody>")
                        rows.forEach { row ->
                            val link =
                                row.report?.let {
                                    "<a href=\"${escape(it.relativeToOrSelf(directory).invariantSeparatorsPath)}\">${escape(it.name)}</a>"
                                } ?: ""
                            appendLine(
                                "<tr class=\"${row.status.name.lowercase()}\"><td>${row.status.icon}</td><td>${escape(row.tool)}</td>" +
                                    "<td>${escape(row.result)}</td><td>$link</td></tr>",
                            )
                        }
                        appendLine("</tbody></table>")
                    }
                }
                appendLine("</main></body></html>")
            }

        private fun escape(text: String): String =
            text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

        private val STYLE =
            """
            :root { color-scheme: light dark; --fg: #1f2328; --bg: #ffffff; --line: #d1d9e0; --muted: #59636e;
              --warn: #9a6700; --fail: #d1242f; }
            @media (prefers-color-scheme: dark) { :root { --fg: #e6edf3; --bg: #0d1117; --line: #3d444d;
              --muted: #9198a1; --warn: #d29922; --fail: #f85149; } }
            body { margin: 0; background: var(--bg); color: var(--fg);
              font: 15px/1.5 system-ui, -apple-system, "Segoe UI", sans-serif; }
            main { max-width: 960px; margin: 0 auto; padding: 24px 16px; }
            h1 { font-size: 24px; margin: 0 0 16px; } h2 { font-size: 18px; margin: 28px 0 8px; }
            table { width: 100%; border-collapse: collapse; }
            th, td { text-align: left; padding: 8px; border-bottom: 1px solid var(--line); vertical-align: top; }
            th { color: var(--muted); font-weight: 600; }
            tr.warning td:nth-child(3) { color: var(--warn); } tr.failed td:nth-child(3) { color: var(--fail); }
            .empty { color: var(--muted); }
            a { color: inherit; }
            """.trimIndent().replace("\n", " ")
    }
}
