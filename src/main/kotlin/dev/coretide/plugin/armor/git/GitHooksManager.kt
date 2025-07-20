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
        if (extension.preCommitEnabled) {
            createPreCommitHook(gitHooksDir)
        }
        if (extension.prePushEnabled) {
            createPrePushHook(gitHooksDir)
        }
        LogUtil.essential("🪝 Git hooks configured successfully")
    }

    private fun createPreCommitHook(hooksDir: File) {
        val preCommitFile = File(hooksDir, "pre-commit")
        LogUtil.verbose("📜🪝 Creating pre-commit hook at ${preCommitFile.absolutePath}... ")
        val preCommitContent =
            """
            #!/bin/bash
            echo "🛡️ CodeArmor: Running pre-commit checks..."
            
            # Get staged files that need formatting
            STAGED_FILES=$(git diff --cached --name-only --diff-filter=ACMR | grep -E '\.(java|kt|kts|xml|json|js|ts|py|go|gradle)$')
            
            if [ -z "${'$'}STAGED_FILES" ]; then
                echo "No relevant files staged for commit. Skipping code formatting."
                exit 0
            fi
            
            echo "📋 Found staged files to format:"
            # shellcheck disable=SC2001
            echo "${'$'}STAGED_FILES" | sed 's/^/  - /'
            
            echo ""
            echo "🔧 Running code formatters..."
            
            echo "Running Spotless formatter..."
            if ./gradlew spotlessApply --quiet --daemon; then
                echo "✅ Spotless formatting completed"
            else
                echo "⚠️ Spotless found issues - attempting to continue"
            fi
            
            echo "Running checkstyle validation..."
            ./gradlew checkstyleMain checkstyleTest --quiet --daemon || echo "⚠️ Checkstyle found issues that need manual fixing."
            
            echo ""
            echo "📝 Re-adding formatted files to staging..."
            for FILE in ${'$'}STAGED_FILES; do
                if [ -f "${'$'}FILE" ]; then
                    git add "${'$'}FILE"
                    echo "  ✅ Added: ${'$'}FILE"
                fi
            done
            
            echo ""
            echo "✅ Pre-commit formatting completed!"
            echo "💡 If there were checkstyle issues, fix them manually and commit again"
            exit 0
            """.trimIndent()

        preCommitFile.writeText(preCommitContent)
        preCommitFile.setExecutable(true)
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
}
