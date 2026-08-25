package com.soundly.data.utils

import java.nio.charset.StandardCharsets

/**
 * Intenta corregir problemas comunes de codificación (Mojibake).
 * Por ejemplo, cuando una cadena UTF-8 se interpreta erróneamente como ISO-8859-1.
 */
fun String?.fixEncoding(): String {
    if (this == null || this.isBlank()) return this ?: ""
    
    // Si contiene caracteres que son típicos de Mojibake (secuencias UTF-8 rotas)
    // Buscamos patrones comunes como 'Ã' seguido de otro caracter extendido
    if (this.contains("Ã")) {
        try {
            val bytes = this.toByteArray(StandardCharsets.ISO_8859_1)
            val fixed = String(bytes, StandardCharsets.UTF_8)
            
            // Verificamos si la cadena resultante es razonable (no tiene caracteres de control extraños)
            // y si realmente cambió algo para mejor
            if (fixed != this && !fixed.contains("\uFFFD")) {
                return fixed
            }
        } catch (e: Exception) {
            // Si falla, devolvemos la original
        }
    }
    
    return this
}
