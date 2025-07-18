/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.armor.configurators

import com.diffplug.gradle.spotless.KotlinExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import dev.coretide.armor.CodeArmorExtension
import dev.coretide.armor.KotlinFormatter
import dev.coretide.armor.KtfmtStyle
import dev.coretide.armor.ProjectType
import dev.coretide.armor.utils.FileUtils
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

object SpotlessConfigurator {
    fun configureSpotless(
        project: Project,
        extension: CodeArmorExtension,
        projectType: ProjectType,
    ) {
        project.pluginManager.apply("com.diffplug.spotless")

        project.configure<SpotlessExtension> {
            if (projectType == ProjectType.KOTLIN_LIBRARY ||
                projectType == ProjectType.KOTLIN_APPLICATION ||
                projectType == ProjectType.MIXED_LIBRARY ||
                projectType == ProjectType.MIXED_APPLICATION
            ) {
                kotlin {
                    configureKotlinSpotless(it, project, extension)
                }
            }
            if (projectType == ProjectType.JAVA_LIBRARY ||
                projectType == ProjectType.JAVA_APPLICATION ||
                projectType == ProjectType.MIXED_LIBRARY ||
                projectType == ProjectType.MIXED_APPLICATION
            ) {
                java {
                    configureJavaSpotless(it, project, extension)
                }
            }
            if (extension.spotlessFormats.json) {
                json {
                    it.target("**/*.json")
                    it.targetExclude("**/build/**", "**/generated/**", "**/node_modules/**")
                    it.gson().indentWithSpaces(extension.spotlessFormats.jsonIndentSpaces)
                    it.endWithNewline()
                    it.trimTrailingWhitespace()
                }
            }
            if (extension.spotlessFormats.xml) {
                format("xml") {
                    it.target("**/*.xml")
                    it.targetExclude("**/build/**", "**/generated/**", "**/target/**")
                    it.eclipseWtp(com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep.XML)
                    it.endWithNewline()
                    it.trimTrailingWhitespace()
                }
            }
            if (extension.spotlessFormats.yaml) {
                format("yaml") {
                    it.target("**/*.yml", "**/*.yaml")
                    it.targetExclude("**/build/**", "**/generated/**", "**/node_modules/**")
                    it.prettier().config(
                        mapOf(
                            "tabWidth" to extension.spotlessFormats.yamlTabWidth,
                            "printWidth" to extension.spotlessFormats.yamlPrintWidth,
                            "parser" to "yaml",
                        ),
                    )
                    it.endWithNewline()
                    it.trimTrailingWhitespace()
                }
            }
            if (extension.spotlessFormats.properties) {
                format("properties") {
                    it.target("**/*.properties")
                    it.targetExclude("**/build/**", "**/generated/**", "**/target/**")
                    it.endWithNewline()
                    it.trimTrailingWhitespace()
                }
            }
            if (extension.spotlessFormats.markdown) {
                format("markdown") {
                    it.target("**/*.md", "**/*.markdown")
                    it.targetExclude("**/build/**", "**/generated/**", "**/node_modules/**")
                    it.prettier().config(
                        mapOf(
                            "tabWidth" to extension.spotlessFormats.markdownTabWidth,
                            "printWidth" to extension.spotlessFormats.markdownPrintWidth,
                            "proseWrap" to extension.spotlessFormats.markdownProseWrap,
                            "parser" to "markdown",
                        ),
                    )
                    it.endWithNewline()
                    it.trimTrailingWhitespace()
                }
            }
            if (extension.spotlessFormats.html) {
                format("html") {
                    it.target("**/*.html", "**/*.htm")
                    it.targetExclude("**/build/**", "**/generated/**", "**/node_modules/**", "**/dist/**")
                    it.prettier().config(
                        mapOf(
                            "tabWidth" to extension.spotlessFormats.htmlTabWidth,
                            "printWidth" to extension.spotlessFormats.htmlPrintWidth,
                            "bracketSameLine" to extension.spotlessFormats.htmlBracketSameLine,
                            "parser" to "html",
                        ),
                    )
                    it.endWithNewline()
                    it.trimTrailingWhitespace()
                }
            }
            if (extension.spotlessFormats.css) {
                format("css") {
                    it.target("**/*.css", "**/*.scss", "**/*.sass", "**/*.less")
                    it.targetExclude("**/build/**", "**/generated/**", "**/node_modules/**", "**/dist/**")
                    it.prettier().config(
                        mapOf(
                            "tabWidth" to extension.spotlessFormats.cssTabWidth,
                            "printWidth" to extension.spotlessFormats.cssPrintWidth,
                            "singleQuote" to extension.spotlessFormats.cssSingleQuote,
                            "parser" to "css",
                        ),
                    )
                    it.endWithNewline()
                    it.trimTrailingWhitespace()
                }
            }
            if (extension.spotlessFormats.sql) {
                format("sql") {
                    it.target("**/*.sql")
                    it.targetExclude("**/build/**", "**/generated/**", "**/target/**")
                    it.endWithNewline()
                    it.trimTrailingWhitespace()
                    if (extension.spotlessFormats.sqlUppercaseKeywords) {
                        it.custom("sqlUppercase") { content ->
                            content.replace(
                                Regex(
                                    "\\b(select|from|where|and|or|order by|group by|having|join|inner join|left join|right join|full join|union|insert|update|delete|create|alter|drop|truncate|index|table|database|schema)\\b",
                                    RegexOption.IGNORE_CASE,
                                ),
                            ) { match ->
                                match.value.uppercase()
                            }
                        }
                    }
                }
            }
        }
        createSpotlessTasks(project, extension)
    }

    private fun configureKotlinSpotless(
        kotlin: KotlinExtension,
        project: Project,
        extension: CodeArmorExtension,
    ) {
        kotlin.target("src/main/kotlin/**/*.kt", "src/test/kotlin/**/*.kt")
        kotlin.targetExclude("**/generated/**", "**/build/**")
        when (extension.kotlinFormatter) {
            KotlinFormatter.KTLINT -> {
                configureKtlintFormatter(kotlin, extension)
            }

            KotlinFormatter.KTFMT -> {
                configureKtfmtFormatter(kotlin, extension)
            }
        }
        kotlin.endWithNewline()
        kotlin.trimTrailingWhitespace()
        if (extension.spotlessApplyLicenseHeader) {
            configureKotlinLicenseHeader(kotlin, project)
        }
    }

    private fun configureKtlintFormatter(
        kotlin: KotlinExtension,
        extension: CodeArmorExtension,
    ) {
        val defaultOverrides: MutableMap<String, Any> =
            mutableMapOf(
                "max_line_length" to "120",
                "indent_size" to "4",
                "ktlint_standard_no-wildcard-imports" to "disabled",
                "ktlint_standard_filename" to "disabled",
            )
        val finalOverrides: MutableMap<String, Any> =
            defaultOverrides.apply {
                putAll(extension.ktlintEditorConfigOverrides.mapValues { it.value })
            }
        kotlin
            .ktlint(extension.ktlintVersion)
            .editorConfigOverride(finalOverrides)
        println("🎨 Configured Spotless with ktlint ${extension.ktlintVersion}")
    }

    private fun configureKtfmtFormatter(
        kotlin: KotlinExtension,
        extension: CodeArmorExtension,
    ) {
        val ktfmtConfig = kotlin.ktfmt(extension.ktfmtVersion)
        when (extension.ktfmtStyle) {
            KtfmtStyle.KOTLINLANG -> ktfmtConfig.kotlinlangStyle()
            KtfmtStyle.GOOGLE -> ktfmtConfig.googleStyle()
            KtfmtStyle.META -> ktfmtConfig.metaStyle()
        }
        println("🎨 Configured Spotless with ktfmt ${extension.ktfmtVersion} (${extension.ktfmtStyle})")
    }

    private fun configureJavaSpotless(
        java: com.diffplug.gradle.spotless.JavaExtension,
        project: Project,
        extension: CodeArmorExtension,
    ) {
        java.target("src/main/java/**/*.java", "src/test/java/**/*.java")
        java.targetExclude("**/generated/**", "**/build/**")
        java.googleJavaFormat()
        java.removeUnusedImports()
        java.endWithNewline()
        java.trimTrailingWhitespace()
        if (extension.spotlessApplyLicenseHeader) {
            configureJavaLicenseHeader(java, project)
        }
    }

    private fun configureKotlinLicenseHeader(
        kotlin: KotlinExtension,
        project: Project,
    ) {
        val licenseFile = FileUtils.createDefaultLicenseHeader(project)
        if (licenseFile.exists()) {
            kotlin.licenseHeaderFile(licenseFile)
        }
    }

    private fun configureJavaLicenseHeader(
        java: com.diffplug.gradle.spotless.JavaExtension,
        project: Project,
    ) {
        val licenseFile = FileUtils.createDefaultLicenseHeader(project)
        if (licenseFile.exists()) {
            java.licenseHeaderFile(licenseFile)
        }
    }

    private fun createSpotlessTasks(
        project: Project,
        extension: CodeArmorExtension,
    ) {
        project.tasks.named("spotlessCheck") { task ->
            task.doLast {
                val formatter = extension.kotlinFormatter.name.lowercase()
                val enabledFormats = getEnabledFormats(extension)
                println("✅ Spotless check completed using $formatter")
                println("🎨 Formatted files: ${enabledFormats.joinToString(", ")}")
                println("📊 Reports available at: build/reports/spotless/")
            }
        }

        project.tasks.named("spotlessApply") { task ->
            task.doLast {
                val formatter = extension.kotlinFormatter.name.lowercase()
                val enabledFormats = getEnabledFormats(extension)
                println("✅ Spotless formatting completed using $formatter")
                println("🎨 Formatted files: ${enabledFormats.joinToString(", ")}")
            }
        }
    }

    private fun getEnabledFormats(extension: CodeArmorExtension): List<String> {
        val formats = mutableListOf<String>()

        with(extension.spotlessFormats) {
            if (json) formats.add("JSON")
            if (xml) formats.add("XML")
            if (yaml) formats.add("YAML")
            if (properties) formats.add("Properties")
            if (markdown) formats.add("Markdown")
            if (html) formats.add("HTML")
            if (css) formats.add("CSS")
            if (sql) formats.add("SQL")
        }

        return formats
    }
}
