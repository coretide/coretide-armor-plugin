/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.git

import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.util.LogUtil
import org.gradle.api.Project
import java.io.File

object GitHooksManager {
    fun configureGitHooks(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        val gitHooksDir = project.file(".git/hooks")
        if (!gitHooksDir.exists()) {
            LogUtil.verbose("ℹ️ Git hooks directory not found. Skipping Git hooks setup.")
            return
        }
        if (extension.prePushEnabled) {
            if (hookExists(gitHooksDir, "pre-push")) {
                LogUtil.essential("⚠️ Pre-push hook already exists. Skipping to preserve custom hooks.")
            } else {
                createPrePushHook(gitHooksDir)
            }
        }
        LogUtil.essential("🪝 Git hooks configured successfully")
    }

    private fun createPrePushHook(hooksDir: File) {
        val prePushFile = File(hooksDir, "pre-push")
        LogUtil.verbose("🫸🪝 Creating pre-push hook at ${prePushFile.absolutePath}...")
        val prePushContent =
            """
            #!/bin/bash
            echo "🛡️ CodeArmor: Running pre-push security and test checks..."
            
            # Run tests first
            echo "🧪 Running tests..."
            if ./gradlew test --quiet --daemon; then
                echo "✅ Tests passed"
            else
                echo "❌ Tests failed - cannot push"
                exit 1
            fi
            
            # Run full analysis (security + quality)
            echo "🔒 Running full analysis..."
            ./gradlew fullAnalysis --quiet --daemon || echo "⚠️ Full analysis found issues - review before pushing"
            
            echo ""
            echo "✅ Pre-push checks completed!"
            echo "💡 Review any security warnings before pushing to production"
            exit 0
            """.trimIndent()

        prePushFile.writeText(prePushContent)
        prePushFile.setExecutable(true)
    }

    @Suppress("SameParameterValue")
    private fun hookExists(
        hooksDir: File,
        hookName: String,
    ): Boolean {
        val hookFile = File(hooksDir, hookName)
        return hookFile.exists()
    }
}
