/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.util

import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.ProjectType
import dev.coretide.plugin.armor.configurator.CompilerConfigurator
import dev.coretide.plugin.armor.configurator.DetektConfigurator
import dev.coretide.plugin.armor.configurator.JacocoConfigurator
import dev.coretide.plugin.armor.configurator.OwaspConfigurator
import dev.coretide.plugin.armor.configurator.ResourceConfigurator
import dev.coretide.plugin.armor.configurator.SonarqubeConfigurator
import dev.coretide.plugin.armor.configurator.SpotbugsConfigurator
import dev.coretide.plugin.armor.configurator.VeracodeConfigurator
import org.gradle.api.Project
import java.io.File

object ConfiguratorUtil {
    /** [aggregatedCoverage]: in a multi-module build, the root's combined coverage report. */
    fun registerConfigurators(
        project: Project,
        extension: CodeArmorExtension,
        projectType: ProjectType,
        aggregatedCoverage: File? = null,
    ) {
        ResourceConfigurator.configure(project, extension, projectType)
        CompilerConfigurator.configure(project, extension, projectType)
        if (extension.jacoco) JacocoConfigurator.configureJacoco(project, extension)
        if (extension.spotbugs) SpotbugsConfigurator.configureSpotbugs(project, extension)
        DetektConfigurator.configure(project, extension)
        if (extension.owasp) OwaspConfigurator.configureOwasp(project, extension)
        if (extension.veracode) VeracodeConfigurator.configureVeracode(project)
        if (extension.sonarqube) SonarqubeConfigurator.configureSonarqube(project, extension, aggregatedCoverage)
    }
}
