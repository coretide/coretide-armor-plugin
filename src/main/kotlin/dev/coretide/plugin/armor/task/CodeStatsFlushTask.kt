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

import dev.coretide.plugin.armor.codestats.CodeStatsFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import java.io.File

/**
 * Retries delivering queued pulses now instead of on the next commit, using the reporting script of
 * the code stats install this repository reports through.
 */
@UntrackedTask(because = "Sends queued pulses to Code::Stats")
abstract class CodeStatsFlushTask : CodeStatsTask() {
    init {
        description = "📤 Retries delivering queued code stats pulses to Code::Stats"
    }

    @TaskAction
    fun flush() {
        val paths = paths
        val activeLibrary =
            locateRepository()
                ?.let(CodeStatsFiles::activeHooksDir)
                ?.let { File(it.parentFile, "lib/codestats.sh") }
                ?.takeIf { it.isFile }
        val library = activeLibrary ?: paths.library.takeIf { it.isFile }
        if (library == null) {
            logger.lifecycle("ℹ️ Code stats is not installed, so there is nothing to deliver")
            return
        }
        val before = paths.queuedPulses()
        if (before == 0) {
            logger.lifecycle("✅ No pulses are waiting")
            return
        }
        val result =
            execOperations.exec {
                it.commandLine(bash(), library.absolutePath, "flush")
                it.isIgnoreExitValue = true
            }
        val after = paths.queuedPulses()
        if (result.exitValue == 0) {
            logger.lifecycle("✅ Delivered $before queued pulse(s)")
        } else {
            logger.warn(
                "⚠️ $after of $before pulse(s) are still queued in ${paths.queueDir}. Check the token in " +
                    "${paths.tokenFile} and the network; they are retried on each commit for up to seven days.",
            )
        }
    }

    /** Git for Windows' own bash, since `bash` on a Windows PATH may be the WSL launcher. */
    private fun bash(): String {
        if (!System.getProperty("os.name").startsWith("Windows")) return "bash"
        // git --exec-path is <install>/mingw64/libexec/git-core; bash is <install>/bin/bash.exe.
        return git
            .output("--exec-path")
            ?.let { File(it).parentFile?.parentFile?.parentFile }
            ?.let { File(it, "bin/bash.exe") }
            ?.takeIf { it.isFile }
            ?.absolutePath
            ?: "bash"
    }
}
