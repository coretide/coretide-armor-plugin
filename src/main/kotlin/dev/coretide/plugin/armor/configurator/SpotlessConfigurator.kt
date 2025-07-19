/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.configurator

import com.diffplug.gradle.spotless.JavaExtension
import com.diffplug.gradle.spotless.KotlinExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep
import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.ProjectType
import dev.coretide.plugin.armor.config.JavaFormatterConfig
import dev.coretide.plugin.armor.config.KotlinFormatterConfig
import dev.coretide.plugin.armor.enumeration.JavaFormatter
import dev.coretide.plugin.armor.enumeration.KotlinFormatter
import dev.coretide.plugin.armor.enumeration.KtfmtStyle
import dev.coretide.plugin.armor.util.FileUtil
import dev.coretide.plugin.armor.util.LogUtil
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
                    it.eclipseWtp(EclipseWtpFormatterStep.XML)
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

    private fun configureJavaSpotless(
        java: JavaExtension,
        project: Project,
        extension: CodeArmorExtension,
    ) {
        LogUtil.verbose("🪄 Applying Java Spotless formatting to project: ${project.name}")
        val config = extension.javaFormatterConfig
        java.target(*config.targetIncludes.toTypedArray())
        java.targetExclude(*config.targetExcludes.toTypedArray())
        when (extension.javaFormatter) {
            JavaFormatter.GOOGLE_JAVA_FORMAT -> configureGoogleJavaFormat(java, config)
            JavaFormatter.ECLIPSE -> configureEclipseFormatter(java, config, project)
            JavaFormatter.PALANTIR_JAVA_FORMAT -> configurePalantirJavaFormat(java, config)
            JavaFormatter.CUSTOM -> configureCustomJavaFormatter(java, config)
        }
        if (config.leadingTabsToSpaces) {
            java.leadingTabsToSpaces(config.indentSize)
        }
        if (config.removeUnusedImports) {
            java.removeUnusedImports()
        }
        if (config.endWithNewline) {
            java.endWithNewline()
        }
        if (config.trimTrailingWhitespace) {
            java.trimTrailingWhitespace()
        }
        config.importOrderFile?.let { importOrderFile ->
            val file = project.file(importOrderFile)
            if (file.exists()) {
                java.importOrder(*file.readLines().toTypedArray())
                LogUtil.verbose("📋 Applied custom import order from: $importOrderFile")
            } else {
                LogUtil.verbose("⚠️ Import order file not found: $importOrderFile")
            }
        }
        if (extension.spotlessApplyLicenseHeader) {
            configureJavaLicenseHeader(java, project)
        }
        LogUtil.verbose("🎨 Configured Java Spotless with ${extension.javaFormatter.name.lowercase()}")
    }

    private fun configureGoogleJavaFormat(
        java: JavaExtension,
        config: JavaFormatterConfig,
    ) {
        val formatter = java.googleJavaFormat(config.googleJavaFormatVersion)
        if (config.googleJavaFormatAOSP) {
            formatter.aosp()
        }
        LogUtil.verbose(
            "   • Google Java Format ${config.googleJavaFormatVersion}${if (config.googleJavaFormatAOSP) " (AOSP style)" else ""}",
        )
    }

    private fun configureEclipseFormatter(
        java: JavaExtension,
        config: JavaFormatterConfig,
        project: Project,
    ) {
        val formatter = java.eclipse(config.eclipseVersion)
        config.eclipseConfigFile?.let { configFile ->
            val file = project.file(configFile)
            if (file.exists()) {
                formatter.configFile(file)
                LogUtil.verbose("   • Eclipse formatter with custom config: $configFile")
            } else {
                LogUtil.essential("⚠️ Eclipse config file not found: $configFile, using default")
                LogUtil.verbose("   • Eclipse formatter ${config.eclipseVersion} (default config)")
            }
        } ?: run {
            LogUtil.verbose("   • Eclipse formatter ${config.eclipseVersion} (default config)")
        }
    }

    private fun configurePalantirJavaFormat(
        java: JavaExtension,
        config: JavaFormatterConfig,
    ) {
        java.palantirJavaFormat(config.palantirJavaFormatVersion)
        LogUtil.verbose("   • Palantir Java Format ${config.palantirJavaFormatVersion}")
    }

    private fun configureCustomJavaFormatter(
        java: JavaExtension,
        config: JavaFormatterConfig,
    ) {
        config.customFormatterCommand?.let { command ->
            java.custom("customJavaFormatter") { content ->
                val processBuilder = ProcessBuilder(listOf(command) + config.customFormatterArgs)
                val process = processBuilder.start()

                process.outputStream.use { it.write(content.toByteArray()) }
                process.waitFor()

                if (process.exitValue() == 0) {
                    process.inputStream.bufferedReader().readText()
                } else {
                    LogUtil.essential("❌ Custom Java formatter failed: ${process.errorStream.bufferedReader().readText()}")
                    content
                }
            }
            LogUtil.verbose("   • Custom formatter: $command ${config.customFormatterArgs.joinToString(" ")}")
        } ?: run {
            LogUtil.verbose("❌ Custom formatter selected but no command specified!")
            configureGoogleJavaFormat(java, config)
        }
    }

    private fun configureKotlinSpotless(
        kotlin: KotlinExtension,
        project: Project,
        extension: CodeArmorExtension,
    ) {
        LogUtil.verbose("🪄 Applying Kotlin Spotless formatting to project: ${project.name}")
        val config = extension.kotlinFormatterConfig
        kotlin.target(*config.targetIncludes.toTypedArray())
        kotlin.targetExclude(*config.targetExcludes.toTypedArray())
        when (extension.kotlinFormatter) {
            KotlinFormatter.KTLINT -> {
                configureKtlintFormatter(kotlin, config)
            }

            KotlinFormatter.KTFMT -> {
                configureKtfmtFormatter(kotlin, config)
            }

            KotlinFormatter.CUSTOM -> {
                configureCustomKotlinFormatter(kotlin, config)
            }
        }

        if (config.endWithNewline) {
            kotlin.endWithNewline()
        }
        if (config.trimTrailingWhitespace) {
            kotlin.trimTrailingWhitespace()
        }

        if (extension.spotlessApplyLicenseHeader) {
            configureKotlinLicenseHeader(kotlin, project)
        }

        LogUtil.verbose("🎨 Configured Kotlin Spotless with ${extension.kotlinFormatter.name.lowercase()}")
    }

    private fun configureKtlintFormatter(
        kotlin: KotlinExtension,
        config: KotlinFormatterConfig,
    ) {
        kotlin
            .ktlint(config.ktlintVersion)
            .editorConfigOverride(config.ktlintEditorConfigOverrides.mapValues { it.value })
        LogUtil.verbose("   • ktlint ${config.ktlintVersion}")
    }

    private fun configureKtfmtFormatter(
        kotlin: KotlinExtension,
        config: KotlinFormatterConfig,
    ) {
        val ktfmtConfig = kotlin.ktfmt(config.ktfmtVersion)
        when (config.ktfmtStyle) {
            KtfmtStyle.KOTLINLANG -> ktfmtConfig.kotlinlangStyle()
            KtfmtStyle.GOOGLE -> ktfmtConfig.googleStyle()
            KtfmtStyle.META -> ktfmtConfig.metaStyle()
        }
        LogUtil.verbose("   • ktfmt ${config.ktfmtVersion} (${config.ktfmtStyle})")
    }

    private fun configureCustomKotlinFormatter(
        kotlin: KotlinExtension,
        config: KotlinFormatterConfig,
    ) {
        config.customFormatterCommand?.let { command ->
            kotlin.custom("customKotlinFormatter") { content ->
                val processBuilder = ProcessBuilder(listOf(command) + config.customFormatterArgs)
                val process = processBuilder.start()

                process.outputStream.use { it.write(content.toByteArray()) }
                process.waitFor()

                if (process.exitValue() == 0) {
                    process.inputStream.bufferedReader().readText()
                } else {
                    LogUtil.essential("❌ Custom Kotlin formatter failed: ${process.errorStream.bufferedReader().readText()}")
                    content
                }
            }
            LogUtil.verbose("   • Custom formatter: $command ${config.customFormatterArgs.joinToString(" ")}")
        } ?: run {
            LogUtil.essential("❌ Custom formatter selected but no command specified!")
            configureKtlintFormatter(kotlin, config)
        }
    }

    private fun configureKotlinLicenseHeader(
        kotlin: KotlinExtension,
        project: Project,
    ) {
        val licenseFile = FileUtil.createDefaultLicenseHeader(project)
        if (licenseFile.exists()) {
            kotlin.licenseHeaderFile(licenseFile)
        }
    }

    private fun configureJavaLicenseHeader(
        java: JavaExtension,
        project: Project,
    ) {
        val licenseFile = FileUtil.createDefaultLicenseHeader(project)
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
                LogUtil.verbose("✅ Spotless check completed using $formatter")
                LogUtil.verbose("🎨 Formatted files: ${enabledFormats.joinToString(", ")}")
                LogUtil.verbose("📊 Reports available at: build/reports/spotless/")
            }
        }

        project.tasks.named("spotlessApply") { task ->
            task.doLast {
                val formatter = extension.kotlinFormatter.name.lowercase()
                val enabledFormats = getEnabledFormats(extension)
                LogUtil.verbose("✅ Spotless formatting completed using $formatter")
                LogUtil.verbose("🎨 Formatted files: ${enabledFormats.joinToString(", ")}")
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
