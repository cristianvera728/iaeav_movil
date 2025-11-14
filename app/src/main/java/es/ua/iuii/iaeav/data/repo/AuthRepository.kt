package es.ua.iuii.iaeav.data.repo

import es.ua.iuii.iaeav.core.storage.SecurePrefs
import es.ua.iuii.iaeav.data.api.AuthApi
import es.ua.iuii.iaeav.data.model.GoogleLoginReq
import es.ua.iuii.iaeav.data.model.LoginReq

class AuthRepository(
    private val api: AuthApi,
    private val sp: SecurePrefs
) {
    suspend fun login(username: String, pass: String) {
        val res = api.login(LoginReq(username, pass))
        sp.saveJwt(res.jwt())
    }

    suspend fun register(username: String, pass: String) {
        api.register(LoginReq(username, pass))
    }

    suspend fun googleLogin(idToken: String) {
        val res = api.googleLogin(GoogleLoginReq(idToken))
        sp.saveJwt(res.jwt())
    }
}