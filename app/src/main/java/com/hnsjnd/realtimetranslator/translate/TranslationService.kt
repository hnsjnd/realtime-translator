package com.hnsjnd.realtimetranslator.translate

interface TranslationService {
    /**
     * Translate [text] from [sourceLang] to [targetLang] (ISO codes; source may be "auto").
     */
    suspend fun translate(text: String, sourceLang: String, targetLang: String): String
}
