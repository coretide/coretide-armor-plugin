/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor

import dev.coretide.plugin.armor.configurator.DependencyHealthConfigurator
import dev.coretide.plugin.armor.util.SbomLicenses
import dev.coretide.plugin.armor.util.SbomLicenses.Component
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SbomLicensesTest {
    @Test
    fun `reads each component's licences, nested components included, and not the project's own`(
        @TempDir dir: File,
    ) {
        val bom = dir.resolve("bom.xml")
        bom.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <bom xmlns="http://cyclonedx.org/schema/bom/1.6" version="1">
              <metadata>
                <component type="library">
                  <group>com.example</group><name>app</name><version>1.0</version>
                  <licenses><license><id>Proprietary</id></license></licenses>
                </component>
              </metadata>
              <components>
                <component type="library">
                  <group>org.slf4j</group><name>slf4j-api</name><version>2.0.17</version>
                  <licenses><license><id>MIT</id></license></licenses>
                </component>
                <component type="library">
                  <group>com.example</group><name>shaded</name><version>1.0</version>
                  <licenses><expression>Apache-2.0 OR MIT</expression></licenses>
                  <components>
                    <component type="library">
                      <group>com.example</group><name>inner</name><version>2.0</version>
                      <licenses><license><name>The Apache Software License, Version 2.0</name></license></licenses>
                    </component>
                  </components>
                </component>
                <component type="library">
                  <group>com.example</group><name>mystery</name><version>0.1</version>
                </component>
              </components>
            </bom>
            """.trimIndent(),
        )

        val components = SbomLicenses.read(bom)

        assertEquals(
            listOf(
                Component("com.example:inner:2.0", listOf("The Apache Software License, Version 2.0")),
                Component("com.example:mystery:0.1", emptyList()),
                Component("com.example:shaded:1.0", listOf("Apache-2.0 OR MIT")),
                Component("org.slf4j:slf4j-api:2.0.17", listOf("MIT")),
            ),
            components,
        )
        assertEquals(
            listOf("Apache-2.0 OR MIT", "MIT", "The Apache Software License, Version 2.0", SbomLicenses.UNKNOWN),
            SbomLicenses.byLicense(components).keys.toList(),
        )
    }

    @Test
    fun `a component is forbidden only when every licence it offers is`() {
        val components =
            listOf(
                Component("gpl:only:1", listOf("GPL-3.0-only")),
                Component("dual:listed:1", listOf("GPL-3.0-only", "MIT")),
                Component("dual:expression:1", listOf("(GPL-3.0-only OR MIT)")),
                Component("both:needed:1", listOf("MIT AND GPL-3.0-only")),
                Component("with:exception:1", listOf("GPL-3.0-only WITH Classpath-exception-2.0")),
                Component("unknown:licence:1", emptyList()),
            )

        val forbidden = SbomLicenses.forbidden(components, listOf("gpl-3.0-ONLY")).map { it.coordinates }

        assertEquals(listOf("gpl:only:1", "both:needed:1"), forbidden)
        assertEquals(emptyList(), SbomLicenses.forbidden(components, emptyList()))
    }

    @Test
    fun `pre-releases are unstable, release qualifiers are not`() {
        listOf("1.0.0-M1", "2.0.0-RC2", "3.1-beta", "1.0-SNAPSHOT", "2.0.0-alpha1").forEach {
            assertTrue(DependencyHealthConfigurator.isUnstable(it), it)
        }
        listOf("1.0.0", "33.4.0-jre", "5.3.1.RELEASE", "1.2.Final", "2.0.17").forEach {
            assertFalse(DependencyHealthConfigurator.isUnstable(it), it)
        }
    }
}
