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
import dev.coretide.plugin.armor.configurator.CheckstyleConfigurator
import dev.coretide.plugin.armor.configurator.JacocoConfigurator
import dev.coretide.plugin.armor.configurator.OwaspConfigurator
import dev.coretide.plugin.armor.configurator.ResourceConfigurator
import dev.coretide.plugin.armor.configurator.SonarqubeConfigurator
import dev.coretide.plugin.armor.configurator.SpotbugsConfigurator
import dev.coretide.plugin.armor.configurator.SpotlessConfigurator
import dev.coretide.plugin.armor.configurator.VeracodeConfigurator
import org.gradle.api.Project

object ConfiguratorUtil {
    fun registerConfigurators(
        project: Project,
        extension: CodeArmorExtension,
        projectType: ProjectType,
    ) {
        ResourceConfigurator.configure(project, extension, projectType)
        if (extension.jacoco) JacocoConfigurator.configureJacoco(project, extension)
        if (extension.checkstyle &&
            ProjectDetector.needsCheckstyle(projectType)
        ) {
            CheckstyleConfigurator.configureCheckstyle(project, extension)
        }
        if (extension.spotbugs) SpotbugsConfigurator.configureSpotbugs(project, extension)
        if (extension.spotless) SpotlessConfigurator.configureSpotless(project, extension, projectType)
        if (extension.owasp) OwaspConfigurator.configureOwasp(project, extension)
        if (extension.veracode) VeracodeConfigurator.configureVeracode(project)
        if (extension.sonarqube) SonarqubeConfigurator.configureSonarqube(project, extension, projectType)
    }
}
