package es.ua.iuii.iaeav.data.api

import retrofit2.http.GET

data class PublicKeyRes(
    val alg: String?,
    val enc: String?,
    val kid: String?,
    val kty: String?,
    val pem: String
)

interface CryptoApi {
    @GET("crypto/public-key")
    suspend fun getPublicKey(): PublicKeyRes
}
