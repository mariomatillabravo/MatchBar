package com.matchbar.app.util

import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Formatea un instante ISO-8601 ("2025-05-12T19:00:00Z") a "12 may, 19:00". */
fun formatKickoff(iso: String): String = runCatching {
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val out = SimpleDateFormat("dd MMM, HH:mm", Locale("es", "ES"))
    out.format(parser.parse(iso) ?: Date())
}.getOrDefault(iso)

fun Throwable.userMessage(): String = when (this) {
    is HttpException -> when (code()) {
        401 -> "Credenciales inválidas o sesión expirada"
        403 -> "No tienes permisos para esta acción"
        404 -> "No encontrado"
        409 -> "Conflicto: el recurso ya existe"
        in 500..599 -> "Error del servidor. Inténtalo más tarde."
        else -> message ?: "Error HTTP ${code()}"
    }
    is IOException -> "No se pudo conectar con el servidor"
    else -> message ?: "Error inesperado"
}
