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

import java.io.File

/**
 * Default content for the config files armor manages, plus scaffolding helpers.
 *
 * Content and writing are deliberately separate: the configurators wire the *content* into a
 * generator task whose output lives under `build/`, because writing into the project tree during
 * configuration invalidates Gradle's configuration cache on the following run. Writing into
 * `config/` is now an explicit opt-in via the `armorScaffoldConfigs` task.
 */
object FileUtil {
    const val SPOTBUGS_EXCLUDE_FILENAME = "spotbugs-exclude.xml"
    const val OWASP_SUPPRESSION_FILENAME = "suppressions.xml"

    fun defaultSpotbugsExcludeContent(): String =
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
        """.trimIndent()

    fun defaultOwaspSuppressionContent(): String =
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
        """.trimIndent()

    /**
     * Writes [content] to [file] only when it is absent, creating parent directories.
     * Returns true when a file was actually written.
     *
     * Only ever called from task execution (`armorScaffoldConfigs`) — never at configuration time.
     */
    fun scaffoldIfAbsent(
        file: File,
        content: String,
    ): Boolean {
        if (file.exists()) {
            return false
        }
        file.parentFile?.mkdirs()
        file.writeText(content)
        return true
    }
}
