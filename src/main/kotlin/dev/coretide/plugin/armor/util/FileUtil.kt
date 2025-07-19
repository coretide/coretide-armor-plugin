/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.util

import org.gradle.api.Project
import java.io.File

@Suppress("HttpUrlsUsage")
object FileUtil {
    fun createDefaultSpotbugsExclude(
        project: Project,
        excludeFile: File? = null,
    ): File {
        val configDir = project.file("config/spotbugs")
        configDir.mkdirs()
        val file = excludeFile ?: configDir.resolve("spotbugs-exclude.xml")
        if (!file.exists()) {
            file.writeText(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <FindBugsFilter>
                    <!-- 
                        SpotBugs exclude filter configuration
                        
                        This file contains patterns for excluding certain findings from SpotBugs analysis.
                        Use this to suppress false positives or accepted issues.
                    -->
                    
                    <!-- Exclude generated code -->
                    <Match>
                        <Class name="~.*\.generated\..*"/>
                    </Match>
                    
                    <!-- Exclude test classes -->
                    <Match>
                        <Class name="~.*Test.*"/>
                    </Match>
                    
                    <!-- Exclude configuration classes -->
                    <Match>
                        <Class name="~.*Config.*"/>
                    </Match>
                    
                    <!-- Exclude DTOs/POJOs from serialization warnings -->
                    <Match>
                        <Class name="~.*\.dto\..*"/>
                        <Bug pattern="SE_NO_SERIALVERSIONID"/>
                    </Match>
                    
                    <Match>
                        <Class name="~.*\.model\..*"/>
                        <Bug pattern="SE_NO_SERIALVERSIONID"/>
                    </Match>
                    
                    <!-- Exclude Spring Boot application main classes -->
                    <Match>
                        <Class name="~.*Application"/>
                        <Method name="main"/>
                    </Match>
                    
                    <!-- Exclude common framework false positives -->
                    <Match>
                        <Bug pattern="EI_EXPOSE_REP"/>
                        <Class name="~.*\.entity\..*"/>
                    </Match>
                    
                    <Match>
                        <Bug pattern="EI_EXPOSE_REP2"/>
                        <Class name="~.*\.entity\..*"/>
                    </Match>
                    
                    <!-- Exclude Lombok generated methods -->
                    <Match>
                        <Bug pattern="EQ_DOESNT_OVERRIDE_EQUALS"/>
                        <Method name="~.*equals.*"/>
                    </Match>
                    
                    <!-- Exclude repository interfaces -->
                    <Match>
                        <Class name="~.*Repository"/>
                        <Bug pattern="SE_NO_SERIALVERSIONID"/>
                    </Match>
                    
                    <!-- Common Spring annotation false positives -->
                    <Match>
                        <Bug pattern="UWF_UNWRITTEN_FIELD"/>
                        <Class name="~.*\.entity\..*"/>
                    </Match>
                    
                    <Match>
                        <Bug pattern="UWF_UNWRITTEN_FIELD"/>
                        <Class name="~.*\.dto\..*"/>
                    </Match>
                </FindBugsFilter>
                """.trimIndent(),
            )
            LogUtil.verbose("📝 Created default SpotBugs exclude file: ${file.absolutePath}")
        }
        return file
    }

    fun createDefaultLicenseHeader(
        project: Project,
        licenseFile: File? = null,
    ): File {
        val configDir = project.file("config/spotless")
        configDir.mkdirs()
        val file = licenseFile ?: configDir.resolve("license-header.txt")
        if (!file.exists()) {
            file.writeText(
                """
                /*
                 * Copyright (c) ${'$'}YEAR ${project.group}
                 *
                 * Licensed under the Apache License, Version 2.0 (the "License");
                 * you may not use this file except in compliance with the License.
                 * You may obtain a copy of the License at
                 *
                 *     http://www.apache.org/licenses/LICENSE-2.0
                 *
                 * Unless required by applicable law or agreed to in writing, software
                 * distributed under the License is distributed on an "AS IS" BASIS,
                 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
                 * See the License for the specific language governing permissions and
                 * limitations under the License.
                 */
                """.trimIndent(),
            )

            LogUtil.verbose("📝 Created default license header: ${file.absolutePath}")
        }
        return file
    }

    fun createDefaultOwaspSuppression(
        project: Project,
        suppressionFile: File? = null,
    ): File {
        val configDir = project.file("config/owasp")
        configDir.mkdirs()
        val file = suppressionFile ?: configDir.resolve("suppressions.xml")
        if (!file.exists()) {
            file.writeText(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                    <!-- 
                        Default OWASP Dependency Check Suppressions
                        
                        Add suppressions here for false positives or accepted risks.
                        
                        Example suppression:
                        <suppress>
                            <notes><![CDATA[
                                This is a false positive for our use case because...
                            ]]></notes>
                            <packageUrl regex="true">^pkg:maven/com\.example/.*$</packageUrl>
                            <cve>CVE-2023-12345</cve>
                        </suppress>
                    -->
                    
                    <!-- Common Spring Boot false positives -->
                    <suppress>
                        <notes><![CDATA[
                            Spring Boot starter dependencies are managed by Spring team
                            and vulnerabilities are typically patched in newer versions.
                        ]]></notes>
                        <packageUrl regex="true">^pkg:maven/org\.springframework\.boot/spring-boot-starter.*$</packageUrl>
                        <vulnerabilityName regex="true">.*spring.*</vulnerabilityName>
                    </suppress>
                    
                    <!-- Test dependencies -->
                    <suppress>
                        <notes><![CDATA[
                            Test dependencies are not part of production runtime
                        ]]></notes>
                        <packageUrl regex="true">^pkg:maven/org\.junit/.*$</packageUrl>
                    </suppress>
                    
                    <suppress>
                        <notes><![CDATA[
                            Test dependencies are not part of production runtime
                        ]]></notes>
                        <packageUrl regex="true">^pkg:maven/org\.mockito/.*$</packageUrl>
                    </suppress>
                </suppressions>
                """.trimIndent(),
            )
            LogUtil.verbose("📝 Created default OWASP suppression file: ${file.absolutePath}")
        }
        return file
    }

    fun createDefaultCheckstyleConfig(project: Project): File {
        val configDir = project.file("config/checkstyle")
        configDir.mkdirs()

        val configFile = File(configDir, "checkstyle.xml")
        if (!configFile.exists()) {
            val defaultConfig =
                """
                <?xml version="1.0"?>
                <!DOCTYPE module PUBLIC
                    "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
                    "https://checkstyle.org/dtds/configuration_1_3.dtd">
                <module name="Checker">
                    <property name="severity" value="error"/>
                    <property name="fileExtensions" value="java, properties, xml"/>
                    
                    <module name="SuppressionFilter">
                        <property name="file" value="${'$'}{checkstyle.suppressions.file}"/>
                    </module>
                    
                    <module name="TreeWalker">
                        <module name="TypeName"/>
                        <module name="MethodName"/>
                        <module name="PackageName"/>
                        <module name="ParameterName"/>
                        <module name="StaticVariableName"/>
                        <module name="LocalVariableName"/>
                        
                        <module name="AvoidStarImport"/>
                        <module name="IllegalImport"/>
                        <module name="RedundantImport"/>
                        <module name="UnusedImports"/>
                        
                        <module name="LineLength">
                            <property name="max" value="120"/>
                        </module>
                        <module name="MethodLength"/>
                        <module name="ParameterNumber"/>
                        
                        <module name="EmptyStatement"/>
                        <module name="EqualsHashCode"/>
                        <module name="HiddenField"/>
                        <module name="IllegalInstantiation"/>
                        <module name="InnerAssignment"/>
                        <module name="MagicNumber"/>
                        <module name="MissingSwitchDefault"/>
                        <module name="SimplifyBooleanExpression"/>
                        <module name="SimplifyBooleanReturn"/>
                        
                        <module name="DesignForExtension"/>
                        <module name="FinalClass"/>
                        <module name="HideUtilityClassConstructor"/>
                        <module name="InterfaceIsType"/>
                        <module name="VisibilityModifier"/>
                        
                        <module name="ArrayTypeStyle"/>
                        <module name="FinalParameters"/>
                        <module name="TodoComment"/>
                        <module name="UpperEll"/>
                    </module>
                </module>
                """.trimIndent()

            configFile.writeText(defaultConfig)
        }

        return configFile
    }

    fun createDefaultCheckstyleSuppression(project: Project): File {
        val configDir = project.file("config/checkstyle")
        configDir.mkdirs()

        val suppressionFile = File(configDir, "suppressions.xml")
        if (!suppressionFile.exists()) {
            val defaultSuppression =
                """
                <?xml version="1.0"?>
                <!DOCTYPE suppressions PUBLIC
                    "-//Checkstyle//DTD SuppressionFilter Configuration 1.2//EN"
                    "https://checkstyle.org/dtds/suppressions_1_2.dtd">
                <suppressions>
                    <suppress files=".*[/\\]generated[/\\].*" checks=".*"/>
                    <suppress files=".*[/\\]build[/\\].*" checks=".*"/>
                    <suppress files=".*[/\\]target[/\\].*" checks=".*"/>
                    <suppress files=".*Test\.java" checks="MagicNumber"/>
                    <suppress files=".*Test\.java" checks="MethodLength"/>
                    <suppress files=".*Application\.java" checks="HideUtilityClassConstructor"/>
                    <suppress files=".*[/\\]dto[/\\].*" checks="DesignForExtension"/>
                    <suppress files=".*[/\\]entity[/\\].*" checks="DesignForExtension"/>
                    <suppress files=".*[/\\]model[/\\].*" checks="DesignForExtension"/>
                </suppressions>
                """.trimIndent()

            suppressionFile.writeText(defaultSuppression)
        }

        return suppressionFile
    }
}
