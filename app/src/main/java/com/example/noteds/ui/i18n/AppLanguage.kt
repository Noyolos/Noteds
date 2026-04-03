package com.example.noteds.ui.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Locale

private const val APP_SETTINGS = "app_settings"
private const val KEY_LANGUAGE = "app_language"

enum class AppLanguage(val storageValue: String) {
    SIMPLIFIED_CHINESE("zh-CN"),
    ENGLISH("en");

    val locale: Locale
        get() = when (this) {
            SIMPLIFIED_CHINESE -> Locale.SIMPLIFIED_CHINESE
            ENGLISH -> Locale.ENGLISH
        }

    val displayLabel: String
        get() = when (this) {
            SIMPLIFIED_CHINESE -> "简体中文"
            ENGLISH -> "English"
        }

    companion object {
        fun fromStorage(value: String?): AppLanguage {
            return entries.firstOrNull { it.storageValue == value } ?: SIMPLIFIED_CHINESE
        }
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.SIMPLIFIED_CHINESE }

@Stable
fun AppLanguage.pick(chinese: String, english: String): String {
    return if (this == AppLanguage.ENGLISH) english else chinese
}

fun Context.loadAppLanguage(): AppLanguage {
    val prefs = getSharedPreferences(APP_SETTINGS, Context.MODE_PRIVATE)
    return AppLanguage.fromStorage(prefs.getString(KEY_LANGUAGE, null))
}

fun Context.saveAppLanguage(language: AppLanguage) {
    val prefs = getSharedPreferences(APP_SETTINGS, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_LANGUAGE, language.storageValue).apply()
}

@Composable
fun rememberCurrencyFormatter(): NumberFormat {
    val language = LocalAppLanguage.current
    return remember(language) {
        NumberFormat.getCurrencyInstance(Locale("en", "MY")).apply {
            currency = Currency.getInstance("MYR")
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
    }
}

@Composable
fun rememberAppDateFormatter(): SimpleDateFormat {
    val language = LocalAppLanguage.current
    return remember(language) {
        val pattern = if (language == AppLanguage.ENGLISH) "dd MMM yyyy" else "yyyy年M月d日"
        SimpleDateFormat(pattern, language.locale)
    }
}
