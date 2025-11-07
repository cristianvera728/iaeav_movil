package es.ua.iuii.iaeav.core.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utilidades AES-GCM (128-bit tag). Devuelve/espera siempre ciphertext||tag.
 * Requisitos:
 * - key: 32 bytes (AES-256)
 * - iv/nonce: 12 bytes (96-bit)
 */
object AesGcm {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_ALGO = "AES"
    private const val TAG_BIT_LENGTH = 128 // 16 bytes de tag

    private val rng = SecureRandom()

    /** Genera una clave aleatoria de 256 bits */
    fun newKey(): ByteArray = ByteArray(32).also { rng.nextBytes(it) }

    /** Cifra un bloque (chunk) con IV/nonce proporcionado. Devuelve ciphertext||tag */
    fun encryptChunk(key: ByteArray, plaintext: ByteArray, iv: ByteArray, aad: ByteArray? = null): ByteArray {
        return encrypt(key, iv, plaintext, aad)
    }

    /** Descifra un bloque (chunk) con IV/nonce proporcionado. Recibe ciphertext||tag */
    fun decryptChunk(key: ByteArray, ctWithTag: ByteArray, iv: ByteArray, aad: ByteArray? = null): ByteArray {
        return decrypt(key, iv, ctWithTag, aad)
    }

    /** Cifra datos completos con AES-GCM. Devuelve ciphertext||tag */
    fun encrypt(key: ByteArray, iv: ByteArray, plaintext: ByteArray, aad: ByteArray? = null): ByteArray {
        require(key.size == 32) { "AES-256 key must be 32 bytes" }
        require(iv.size == 12) { "GCM nonce/IV must be 12 bytes" }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val sk = SecretKeySpec(key, KEY_ALGO)
        val gcm = GCMParameterSpec(TAG_BIT_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, sk, gcm)
        if (aad != null && aad.isNotEmpty()) cipher.updateAAD(aad)
        return cipher.doFinal(plaintext) // ciphertext || tag
    }

    /** Descifra datos completos con AES-GCM. Espera ciphertext||tag */
    fun decrypt(key: ByteArray, iv: ByteArray, ctWithTag: ByteArray, aad: ByteArray? = null): ByteArray {
        require(key.size == 32) { "AES-256 key must be 32 bytes" }
        require(iv.size == 12) { "GCM nonce/IV must be 12 bytes" }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val sk = SecretKeySpec(key, KEY_ALGO)
        val gcm = GCMParameterSpec(TAG_BIT_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, sk, gcm)
        if (aad != null && aad.isNotEmpty()) cipher.updateAAD(aad)
        return cipher.doFinal(ctWithTag)
    }

    /**
     * Helper usado en tu repo: si `encrypt=true`, cifra; si no, descifra.
     * - encrypt=true  -> devuelve ciphertext||tag
     * - encrypt=false -> espera ciphertext||tag y devuelve plaintext
     */
    fun decryptOrEncrypt(
        key: ByteArray,
        iv: ByteArray,
        data: ByteArray,
        aad: ByteArray? = null,
        encrypt: Boolean
    ): ByteArray = if (encrypt) encrypt(key, iv, data, aad) else decrypt(key, iv, data, aad)
}
