package com.soundly.ui.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleManager {
    /**
     * Aplica el idioma a nivel de aplicación.
     * Al haber configurado 'configChanges' en el manifiesto, la actividad ya no se
     * destruirá, eliminando el parpadeo negro. Compose se encargará de recomponer
     * los textos automáticamente.
     */
    fun applyLanguage(languageCode: String) {
        val appLocale: LocaleListCompat = if (languageCode.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    /**
     * Alias para compatibilidad con llamadas existentes
     */
    fun applyLanguageSeamless(languageCode: String, activity: android.app.Activity? = null) {
        applyLanguage(languageCode)
    }
}
