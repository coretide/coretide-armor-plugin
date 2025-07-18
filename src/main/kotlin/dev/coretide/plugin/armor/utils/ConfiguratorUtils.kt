/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.utils

import dev.coretide.plugin.armor.CodeArmorExtension
import dev.coretide.plugin.armor.configurators.CheckstyleConfigurator
import dev.coretide.plugin.armor.configurators.JacocoConfigurator
import dev.coretide.plugin.armor.configurators.OwaspConfigurator
import dev.coretide.plugin.armor.configurators.ResourceConfigurator
import dev.coretide.plugin.armor.configurators.SonarqubeConfigurator
import dev.coretide.plugin.armor.configurators.SpotbugsConfigurator
import dev.coretide.plugin.armor.configurators.SpotlessConfigurator
import dev.coretide.plugin.armor.configurators.VeracodeConfigurator
import org.gradle.api.Project

object ConfiguratorUtils {
    fun registerConfigurators(
        project: Project,
        extension: CodeArmorExtension,
        projectType: dev.coretide.plugin.armor.ProjectType,
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
