package es.ua.iuii.iaeav.core.storage

import android.content.SharedPreferences

/**
 * # Preferencias Seguras (SecurePrefs)
 *
 * Clase wrapper que simplifica el acceso y la manipulación del token JWT
 * almacenado en [SharedPreferences] cifradas (proporcionadas por el AndroidX Security Crypto Library).
 *
 * El propósito principal es abstraer la lógica de acceso a la clave "jwt".
 *
 * @property sp Instancia de SharedPreferences, que debe ser una instancia de
 * [androidx.security.crypto.EncryptedSharedPreferences].
 */
class SecurePrefs(private val sp: SharedPreferences) {

    /**
     * Guarda el token JWT de la sesión actual.
     *
     * @param t El token JWT (String) a guardar.
     */
    fun saveJwt(t: String) = sp.edit().putString("jwt", t).apply()

    /**
     * Recupera el token JWT almacenado.
     *
     * @return El token JWT como String, o null si no existe una sesión activa.
     */
    fun getJwt(): String? = sp.getString("jwt", null)

    /**
     * Elimina todos los datos almacenados en las preferencias seguras.
     *
     * Usado para cerrar la sesión del usuario (logout) y limpiar cualquier token o
     * configuración sensible.
     */
    fun clear() = sp.edit().clear().apply()
}