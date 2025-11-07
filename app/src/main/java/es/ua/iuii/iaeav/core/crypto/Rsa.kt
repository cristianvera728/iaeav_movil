package es.ua.iuii.iaeav.core.crypto

import android.util.Base64
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PSSParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

/**
 * Utilidades RSA para:
 * - parsear PEM pública (SubjectPublicKeyInfo)
 * - envolver clave AES (RSA-OAEP-256)
 */
object Rsa {
    /** Convierte un PEM "-----BEGIN PUBLIC KEY----- ... -----END PUBLIC KEY-----" a PublicKey */
    fun publicKeyFromPem(pem: String): PublicKey {
        val cleaned = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")
        val der = Base64.decode(cleaned, Base64.DEFAULT)
        val spec = X509EncodedKeySpec(der)
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }

    /**
     * Envuelve (cifra) una clave AES con RSA-OAEP-256 (SHA-256 con MGF1-SHA256).
     * Devuelve los bytes envueltos (raw).
     */
    fun wrapAesKey(aesKey: ByteArray, serverPub: PublicKey): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        val oaep = OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT
        )
        cipher.init(Cipher.ENCRYPT_MODE, serverPub, oaep)
        return cipher.doFinal(aesKey)
    }
}
