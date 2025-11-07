package es.ua.iuii.iaeav.core

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import es.ua.iuii.iaeav.core.storage.SecurePrefs
import es.ua.iuii.iaeav.data.api.AuthApi
import es.ua.iuii.iaeav.data.api.CryptoApi
import es.ua.iuii.iaeav.data.api.RecordingsApi
import es.ua.iuii.iaeav.data.repo.AuthRepository
import es.ua.iuii.iaeav.data.repo.RecordingRepository
import java.util.concurrent.TimeUnit

object ServiceLocator {
    // Base del backend (con barra final)
    private const val BASE_URL = "https://iaeav.iuii.ua.es/api/v1/"

    @Volatile private var initialized = false

    // Expuestos
    lateinit var securePrefs: SecurePrefs; private set
    lateinit var retrofit: Retrofit; private set
    lateinit var authApi: AuthApi; private set
    lateinit var cryptoApi: CryptoApi; private set
    lateinit var recordingsApi: RecordingsApi; private set

    lateinit var authRepository: AuthRepository; private set
    lateinit var recordingRepository: RecordingRepository; private set

    fun init(appContext: Context) {
        if (initialized) return

        // SharedPreferences cifrados
        val master = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val esp = EncryptedSharedPreferences.create(
            appContext,
            "secure",
            master,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        securePrefs = SecurePrefs(esp)


        val authInterceptor = Interceptor { chain ->
            val original = chain.request()

            // Si la petición YA trae Authorization (ej. upload_token), no añadir otra
            if (original.header("Authorization") != null) {
                return@Interceptor chain.proceed(original)
            }

            val userToken = securePrefs.getJwt()
            val req = if (!userToken.isNullOrBlank()) {
                original.newBuilder()
                    .header("Authorization", "Bearer $userToken")
                    .build()
            } else {
                original
            }
            chain.proceed(req)
        }


        // Logging (BODY en debug; si quieres, baja a BASIC en prod)
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttp = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logger)
            // (Opcional) timeouts razonables para subidas
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory()) // soporte data classes de Kotlin
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        authApi = retrofit.create(AuthApi::class.java)
        cryptoApi = retrofit.create(CryptoApi::class.java)
        recordingsApi = retrofit.create(RecordingsApi::class.java)

        authRepository = AuthRepository(authApi, securePrefs)
        recordingRepository = RecordingRepository(recordingsApi, cryptoApi)

        initialized = true
    }
}
