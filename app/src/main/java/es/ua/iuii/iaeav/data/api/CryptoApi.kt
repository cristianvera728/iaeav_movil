package es.ua.iuii.iaeav.data.api

import retrofit2.http.GET

/**
 * # Respuesta de Clave Pública (PublicKeyRes)
 *
 * DTO que contiene la clave pública (PEM) del servidor y metadatos de cifrado
 * siguiendo el estándar JSON Web Key (JWK).
 */
data class PublicKeyRes(
    /** Algoritmo de clave recomendado/usado (ej. "RSA-OAEP-256"). */
    val alg: String?,
    /** Algoritmo de cifrado de contenido (ej. "A256GCM"). */
    val enc: String?,
    /** Identificador de la clave (Key ID - KID). */
    val kid: String?,
    /** Tipo de clave (Key Type - KTY), típicamente "RSA". */
    val kty: String?,
    /** Clave pública codificada en formato PEM. Es la clave usada para envolver la DEK. */
    val pem: String
)

/**
 * # Interfaz de la API de Criptografía (CryptoApi)
 *
 * Define los endpoints para las operaciones relacionadas con el intercambio de claves
 * y la información criptográfica necesaria para el cifrado de datos.
 */
interface CryptoApi {

    /**
     * Obtiene la clave pública RSA del servidor.
     * * Esta clave se utiliza en el cliente para cifrar la Clave de Cifrado de Datos (DEK)
     * antes de enviarla al servidor (Key Wrapping).
     * * @return [PublicKeyRes] Objeto que contiene la clave pública PEM y sus metadatos.
     */
    @GET("crypto/public-key")
    suspend fun getPublicKey(): PublicKeyRes
}