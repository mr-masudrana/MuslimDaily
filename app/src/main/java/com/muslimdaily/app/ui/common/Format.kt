package com.muslimdaily.app.ui.common

import java.text.NumberFormat
import java.util.Locale

/** 12 -> "12" in English, "১২" in Bangla. */
fun localizeNumber(n: Int, locale: Locale): String =
    NumberFormat.getInstance(locale).apply { isGroupingUsed = false }.format(n)
