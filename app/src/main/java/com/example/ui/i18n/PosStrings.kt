package com.example.ui.i18n

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppLanguage(val code: String, val badge: String) {
    BENGALI("bn", "বাংলা"),
    ENGLISH("en", "EN"),
    BILINGUAL("both", "BN/EN");

    companion object {
        fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: BILINGUAL
    }
}

fun AppLanguage.pick(bn: String, en: String): String = when (this) {
    AppLanguage.BENGALI -> bn
    AppLanguage.ENGLISH -> en
    AppLanguage.BILINGUAL -> "$bn • $en"
}

fun AppLanguage.shortPick(bn: String, en: String): String = when (this) {
    AppLanguage.BENGALI -> bn
    AppLanguage.ENGLISH -> en
    AppLanguage.BILINGUAL -> bn
}

fun formatTaka(amount: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale("en", "BD")).apply {
        maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
        minimumFractionDigits = 0
    }
    return "৳${nf.format(amount)}"
}

fun formatQty(qty: Double): String {
    return if (qty % 1.0 == 0.0) qty.toInt().toString()
    else String.format(Locale.US, "%.2f", qty).trimEnd('0').trimEnd('.')
}

fun formatDateTime(epochMs: Long): String {
    if (epochMs <= 0L) return ""
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
    return sdf.format(Date(epochMs))
}

fun formatDateOnly(epochMs: Long): String {
    if (epochMs <= 0L) return ""
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    return sdf.format(Date(epochMs))
}
