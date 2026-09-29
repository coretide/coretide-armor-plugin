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
import dev.coretide.plugin.armor.util.XmlReports
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import java.io.File

/**
 * Accepts the SpotBugs findings already in the code: copies `spotbugsMain`'s XML report, run without a baseline,
 * to the baseline file. From then on `spotbugsMain` reports, and fails on, new findings only. SpotBugs matches a
 * finding by a hash of its bug pattern, class, method and names, not its line, so edits around it keep it in the
 * baseline.
 */
@UntrackedTask(because = "Writes the baseline into the source tree, where it is meant to be committed")
abstract class SpotbugsBaselineTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val report: RegularFileProperty

    @get:OutputFile
    abstract val baseline: RegularFileProperty

    init {
        group = "verification"
        description = "🐛 Accepts the SpotBugs findings already in the code, so only new ones fail the build"
    }

    @TaskAction
    fun write() {
        val source = report.get().asFile
        if (!source.isFile) throw GradleException("SpotBugs wrote no XML report at $source")
        val target = baseline.get().asFile
        target.parentFile.mkdirs()
        source.copyTo(target, overwrite = true)
        val findings = count(target)
        LogUtil.essential(
            this,
            if (findings == 0) {
                "🐛 SpotBugs baseline: no findings to accept, so ${target.path} lists none"
            } else {
                "🐛 SpotBugs baseline: $findings findings accepted in ${target.path}. Commit it; spotbugsMain now fails on new findings only"
            },
        )
    }

    companion object {
        const val TASK_NAME = "armorSpotbugsBaseline"

        /** The findings in a SpotBugs XML report, or baseline. */
        fun count(report: File): Int = XmlReports.parse(report).getElementsByTagName("BugInstance").length
    }
}
