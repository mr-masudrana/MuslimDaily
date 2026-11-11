package com.muslimdaily.app.data.dua

// Parsed from assets/dua/duas.json. Fields are nullable because Gson ignores Kotlin defaults.

data class LocalizedText(val en: String? = null, val bn: String? = null)

fun LocalizedText?.text(bangla: Boolean): String {
    if (this == null) return ""
    val primary = if (bangla) bn else en
    val fallback = if (bangla) en else bn
    return primary?.takeIf { it.isNotBlank() } ?: fallback.orEmpty()
}

data class DuaFile(val categories: List<DuaCategory>? = null)

data class DuaCategory(
    val id: String? = null,
    val title: LocalizedText? = null,
    val items: List<DuaItem>? = null,
)

data class DuaItem(
    val id: String? = null,
    val title: LocalizedText? = null,
    val arabic: String? = null,
    val transliteration: LocalizedText? = null,
    val translation: LocalizedText? = null,
    val reference: String? = null,
    val repeat: Int? = null,
)
