package es.ua.iuii.iaeav.core.storage

import android.content.SharedPreferences

class SecurePrefs(private val sp: SharedPreferences) {
    fun saveJwt(t: String) = sp.edit().putString("jwt", t).apply()
    fun getJwt(): String? = sp.getString("jwt", null)
    fun clear() = sp.edit().clear().apply()
}

