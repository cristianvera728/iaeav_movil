package es.ua.iuii.iaeav.core.crypto

import android.util.Base64

fun b64u(data: ByteArray): String =
    Base64.encodeToString(data, Base64.NO_WRAP or Base64.URL_SAFE).trimEnd('=')
