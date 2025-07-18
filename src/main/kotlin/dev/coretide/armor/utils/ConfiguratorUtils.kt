/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.armor.utils

import dev.coretide.armor.CodeArmorExtension
import dev.coretide.armor.configurators.CheckstyleConfigurator
import dev.coretide.armor.configurators.JacocoConfigurator
import dev.coretide.armor.configurators.OwaspConfigurator
import dev.coretide.armor.configurators.ResourceConfigurator
import dev.coretide.armor.configurators.SonarqubeConfigurator
import dev.coretide.armor.configurators.SpotbugsConfigurator
import dev.coretide.armor.configurators.SpotlessConfigurator
import dev.coretide.armor.configurators.VeracodeConfigurator
import org.gradle.api.Project

object ConfiguratorUtils {
    fun registerConfigurators(
        project: Project,
        extension: CodeArmorExtension,
        projectType: dev.coretide.armor.ProjectType,
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
