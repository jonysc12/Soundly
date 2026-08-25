package com.soundly.cloud

// ==================== HELPERS ====================
fun String.cleanYouTubeArtist(): String = this
    .replace(Regex(" -?\\s*Topic\\s*$", RegexOption.IGNORE_CASE), "")
    .replace(Regex("^\\s*-\\s*"), "")
    .trim()

fun String.forceSquareHighRes(): String {
    return if (this.contains("googleusercontent.com")) {
        val baseUrl = this.substringBefore("=")
        // w600 es ideal para la lista, -c es el recorte cuadrado perfecto
        "$baseUrl=w600-h600-c-rj-l90"
    } else {
        this
    }
}

fun Long.toMinSec(): String = "${this / 60}:${String.format(java.util.Locale.ROOT, "%02d", this % 60)}"

/**
 * Intenta corregir problemas comunes de codificación (Mojibake).
 */
fun String?.fixEncoding(): String {
    if (this == null || this.isBlank()) return this ?: ""
    if (this.contains("Ã")) {
        try {
            val bytes = this.toByteArray(java.nio.charset.StandardCharsets.ISO_8859_1)
            val fixed = String(bytes, java.nio.charset.StandardCharsets.UTF_8)
            if (fixed != this && !fixed.contains("\uFFFD")) return fixed
        } catch (e: Exception) {}
    }
    return this
}
