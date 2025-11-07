package es.ua.iuii.iaeav.core.crypto

import android.util.Base64
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

fun publicKeyFromPem(pem: String): PublicKey {
    val cleaned = pem
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replace("\\s".toRegex(), "")
    val der = Base64.decode(cleaned, Base64.DEFAULT)
    val spec = X509EncodedKeySpec(der)
    return KeyFactory.getInstance("RSA").generatePublic(spec)
}
