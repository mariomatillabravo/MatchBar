package com.matchbar.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.matchbar.app.data.model.Role
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "matchbar_prefs")

/** Preferencia de tema seleccionada por el usuario. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

class SessionStore(private val context: Context) {

    private val KEY_TOKEN = stringPreferencesKey("jwt")
    private val KEY_NAME = stringPreferencesKey("name")
    private val KEY_EMAIL = stringPreferencesKey("email")
    private val KEY_ROLE = stringPreferencesKey("role")
    private val KEY_USER_ID = stringPreferencesKey("user_id")
    private val KEY_THEME = stringPreferencesKey("theme_mode")

    data class Session(
        val token: String,
        val userId: String,
        val email: String,
        val name: String,
        val role: Role
    )

    /** Tema elegido por el usuario. Se conserva aunque cierre sesión. */
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME] = mode.name }
    }

    val session: Flow<Session?> = context.dataStore.data.map { prefs ->
        val token = prefs[KEY_TOKEN] ?: return@map null
        val role = prefs[KEY_ROLE]?.let { runCatching { Role.valueOf(it) }.getOrNull() } ?: return@map null
        Session(
            token = token,
            userId = prefs[KEY_USER_ID].orEmpty(),
            email = prefs[KEY_EMAIL].orEmpty(),
            name = prefs[KEY_NAME].orEmpty(),
            role = role
        )
    }

    /**
     * Guarda únicamente el token, sin el rol. La sesión sigue considerándose
     * nula (porque [session] exige también el rol), de modo que la navegación
     * no cambia todavía, pero el interceptor ya puede autenticar peticiones.
     * Útil durante el registro de un bar para subir fotos/licencia antes de
     * completar la sesión.
     */
    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs -> prefs[KEY_TOKEN] = token }
    }

    suspend fun save(token: String, userId: String, email: String, name: String, role: Role) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            prefs[KEY_USER_ID] = userId
            prefs[KEY_EMAIL] = email
            prefs[KEY_NAME] = name
            prefs[KEY_ROLE] = role.name
        }
    }

    suspend fun clear() {
        // Borramos solo los datos de sesión; mantenemos preferencias como el tema.
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_EMAIL)
            prefs.remove(KEY_NAME)
            prefs.remove(KEY_ROLE)
        }
    }

    /** Devuelve el token actual de forma síncrona para el interceptor de OkHttp. */
    fun currentTokenBlocking(): String? = runBlocking {
        context.dataStore.data.map { it[KEY_TOKEN] }.first()
    }
}
