package com.matchbar.app.util

import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatKickoff(iso: String): String = runCatching {
    val instant = Instant.parse(iso)
    val formatter = DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale("es", "ES"))
        .withZone(ZoneId.of("Europe/Madrid"))
    formatter.format(instant)
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
