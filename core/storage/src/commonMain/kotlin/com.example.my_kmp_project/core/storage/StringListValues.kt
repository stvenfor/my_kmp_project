package com.example.my_kmp_project.core.storage

/**
 * List encoding shared by every backend. Items are joined with the ASCII unit separator
 * (code point 31, 0x1F) -- the encoding Android already used for the home_favorites store,
 * so existing on-device values keep reading.
 *
 * An empty list encodes to the empty string. A separator inside a value is dropped, so a
 * round-trip can never invent extra items.
 */
public object StringListValues {
    /** Unit separator: cannot appear in the ids/labels these keys hold. */
    public val SEPARATOR: Char = Char(31)

    public fun encode(values: List<String>): String =
        values.map { it.replace(SEPARATOR.toString(), "") }.joinToString(SEPARATOR.toString())

    /** Reverse of [encode]; `null`/blank raw means "absent". */
    public fun decode(raw: String?): List<String>? {
        if (raw == null) return null
        if (raw.isEmpty()) return emptyList()
        return raw.split(SEPARATOR).filter { it.isNotEmpty() }
    }
}
