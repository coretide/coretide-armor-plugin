package com.example.text

/** Turns a title into the lowercase, hyphenated form a URL can carry: `Hello, World!` becomes `hello-world`. */
public fun slugify(title: String): String =
    title
        .lowercase()
        .split(NOT_A_LETTER_OR_DIGIT)
        .filter { it.isNotEmpty() }
        .joinToString("-")

/** A slug, cut to at most [maxLength] characters at a word boundary. */
public fun slugify(
    title: String,
    maxLength: Int,
): String {
    require(maxLength > 0) { "maxLength must be positive: $maxLength" }
    val slug = slugify(title)
    if (slug.length <= maxLength) return slug
    val boundary = slug.lastIndexOf('-', startIndex = maxLength)
    return if (boundary > 0) slug.substring(0, boundary) else slug.substring(0, maxLength)
}

private val NOT_A_LETTER_OR_DIGIT = Regex("[^\\p{L}\\p{N}]+")
