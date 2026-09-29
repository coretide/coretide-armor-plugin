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

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** The licences of the components in a CycloneDX SBOM, in its XML form. */
object SbomLicenses {
    const val UNKNOWN = "(no licence information)"

    /** A dependency, as `group:name:version`, and its licences: SPDX ids, names or expressions. */
    data class Component(
        val coordinates: String,
        val licenses: List<String>,
    )

    fun read(bom: File): List<Component> {
        val factory =
            DocumentBuilderFactory.newInstance().apply {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                isExpandEntityReferences = false
            }
        val root = factory.newDocumentBuilder().parse(bom).documentElement
        // The components section, not the metadata's description of the project itself.
        val components = children(root, "components").firstOrNull() ?: return emptyList()
        return descendants(components, "component")
            .map { component ->
                val coordinates = listOfNotNull(text(component, "group"), text(component, "name"), text(component, "version")).joinToString(":")
                Component(coordinates, licenses(component))
            }.distinctBy { it.coordinates }
            .sortedBy { it.coordinates }
    }

    /** Components by licence. One under several licences appears under each; one without any under [UNKNOWN]. */
    fun byLicense(components: List<Component>): Map<String, List<String>> =
        components
            .flatMap { component -> component.licenses.ifEmpty { listOf(UNKNOWN) }.map { it to component.coordinates } }
            .groupBy({ it.first }, { it.second })
            .toSortedMap(compareBy<String> { it == UNKNOWN }.thenBy { it.lowercase() })

    /**
     * Components that can only be used under a [forbidden] licence. Several licences, or an `OR` expression, are
     * a choice: a component is only forbidden when every choice is. A choice is forbidden when it, or one of the
     * licences it combines with `AND`, is named in [forbidden], ignoring case. `GPL-2.0-only WITH
     * Classpath-exception-2.0` is its own licence: forbidding `GPL-2.0-only` does not forbid it.
     */
    fun forbidden(
        components: List<Component>,
        forbidden: Collection<String>,
    ): List<Component> {
        if (forbidden.isEmpty()) return emptyList()
        val names = forbidden.map(::normalize).toSet()
        fun forbiddenChoice(choice: String): Boolean = choice.split(AND).any { normalize(it) in names }
        return components.filter { component ->
            val choices = component.licenses.flatMap { it.split(OR) }
            choices.isNotEmpty() && choices.all(::forbiddenChoice)
        }
    }

    private val OR = Regex("\\s+OR\\s+", RegexOption.IGNORE_CASE)
    private val AND = Regex("\\s+AND\\s+", RegexOption.IGNORE_CASE)

    private fun normalize(license: String): String =
        license
            .replace('(', ' ')
            .replace(')', ' ')
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()

    private fun licenses(component: Element): List<String> =
        children(component, "licenses")
            .flatMap { licenses -> children(licenses) }
            .mapNotNull { choice ->
                when (choice.tagName) {
                    "license" -> text(choice, "id") ?: text(choice, "name")
                    "expression" -> choice.textContent.trim().ifEmpty { null }
                    else -> null
                }
            }.distinct()

    private fun text(
        element: Element,
        tag: String,
    ): String? =
        children(element, tag)
            .firstOrNull()
            ?.textContent
            ?.trim()
            ?.ifEmpty { null }

    private fun children(
        element: Element,
        tag: String? = null,
    ): List<Element> {
        val nodes = element.childNodes
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }.filter { tag == null || it.tagName == tag }
    }

    /** Components can nest: a shaded jar lists what it contains. */
    private fun descendants(
        element: Element,
        tag: String,
    ): List<Element> =
        children(element, tag).flatMap { child ->
            listOf(child) + children(child, "components").flatMap { descendants(it, tag) }
        }
}
