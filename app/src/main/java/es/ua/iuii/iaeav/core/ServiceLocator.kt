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

/**
 * # Service Locator para la Aplicación
 *
 * Objeto singleton central responsable de la inicialización y provisión de
 * todas las dependencias principales (APIs y Repositorios) de la aplicación.
 * Sigue el patrón Service Locator.
 *
 * **NOTA:** Debe ser inicializado una única vez mediante [init]
 * al arrancar la [android.app.Application].
 */
object ServiceLocator {

    /** URL base del backend para las peticiones de API (ej. https://iaeav.iuii.ua.es/api/v1/) */
    private const val BASE_URL = "https://iaeav.iuii.ua.es/api/v1/"

    /** Bandera para asegurar que la inicialización se realiza solo una vez. */
    @Volatile private var initialized = false

    // --- Componentes Expuestos (Acceso de Solo Lectura) ---

    /** Almacenamiento seguro y cifrado para tokens y datos sensibles. */
    lateinit var securePrefs: SecurePrefs; private set

    /** Instancia de Retrofit, la base para crear todas las interfaces de API. */
    lateinit var retrofit: Retrofit; private set

    // --- Interfaces de API ---

    /** Interfaz de Retrofit para gestionar la Autenticación y el Perfil. */
    lateinit var authApi: AuthApi; private set

    /** Interfaz de Retrofit para obtener la clave pública de cifrado del servidor. */
    lateinit var cryptoApi: CryptoApi; private set

    /** Interfaz de Retrofit para la subida de grabaciones. */
    lateinit var recordingsApi: RecordingsApi; private set

    // --- Repositorios (Lógica de Negocio) ---

    /** Repositorio principal para las operaciones de autenticación (login, registro, perfil). */
    lateinit var authRepository: AuthRepository; private set

    /** Repositorio que orquesta el flujo completo de grabación, cifrado y subida de archivos. */
    lateinit var recordingRepository: RecordingRepository; private set

    /**
     * Inicializa todos los componentes y dependencias de la aplicación.
     *
     * @param appContext El Context de la aplicación, usado para crear el almacenamiento seguro.
     */
    fun init(appContext: Context) {
        if (initialized) return

        // 1. Configuración de SecurePrefs (SharedPreferences Cifrados)
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
        // Se crea el wrapper de SecurePrefs para facilitar el acceso a los datos
        securePrefs = SecurePrefs(esp)


        // 2. Interceptor de Autorización (Añade el JWT automáticamente)
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()

            // Evitar reescribir tokens específicos (ej. los usados en la subida chunked)
            if (original.header("Authorization") != null) {
                return@Interceptor chain.proceed(original)
            }

            // Añadir el token JWT de la sesión si existe
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


        // 3. Interceptor de Logging (Solo en desarrollo/debug)
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Muestra el cuerpo completo de las peticiones/respuestas
        }

        // 4. Cliente OkHttp
        val okHttp = OkHttpClient.Builder()
            .addInterceptor(authInterceptor) // Aplicamos el JWT a todas las peticiones
            .addInterceptor(logger) // Registro de logs para debug
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS) // Tiempo de espera para la lectura (ej. respuesta grande)
            .writeTimeout(60, TimeUnit.SECONDS) // Tiempo de espera para la escritura (ej. subida de archivo)
            .build()

        // 5. Configuración de Moshi (Serialización JSON)
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory()) // Habilita el soporte para las data classes de Kotlin
            .build()

        // 6. Instancia de Retrofit
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        // 7. Creación de APIs
        authApi = retrofit.create(AuthApi::class.java)
        cryptoApi = retrofit.create(CryptoApi::class.java)
        recordingsApi = retrofit.create(RecordingsApi::class.java)

        // 8. Creación de Repositorios (Inyección de dependencias)
        authRepository = AuthRepository(authApi, securePrefs)
        recordingRepository = RecordingRepository(recordingsApi, cryptoApi)

        initialized = true
    }
}