package es.ua.iuii.iaeav.core.audio


import android.media.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Gestiona la grabación de audio desde el micrófono y la guarda en formato WAV.
 *
 * Esta clase maneja el ciclo de vida de [AudioRecord], escribe los datos de audio crudo (PCM)
 * en un archivo temporal y, al finalizar, ensambla un archivo .wav completo con
 * la cabecera correspondiente.
 *
 * @param sampleRate La frecuencia de muestreo deseada para la grabación (ej. 48000 Hz).
 */
class AudioRecorder(
    private val sampleRate: Int = 48000
) {
    // Objeto principal de Android para capturar audio del hardware.
    private var recorder: AudioRecord? = null

    // Hilo dedicado para leer datos del buffer de AudioRecord y escribirlos a disco.
    // Esto evita bloquear el hilo principal (UI).
    private var recordingThread: Thread? = null

    // Flag volátil para controlar el estado de grabación de forma segura entre hilos.
    @Volatile private var isRecording = false

    // Archivo temporal para almacenar los datos de audio crudo (PCM).
    private var pcmFile: File? = null

    // Archivo final donde se guardará el audio en formato WAV.
    private var wavFile: File? = null

    /**
     * Inicia el proceso de grabación.
     *
     * Configura [AudioRecord], crea un hilo para procesar el audio y comienza
     * a guardar los datos PCM crudos en un archivo temporal.
     *
     * @param outputWav El archivo [File] donde se guardará la grabación final en formato WAV.
     * @throws IllegalStateException Si AudioRecord no se puede inicializar (ej. permisos faltantes o hardware ocupado).
     */
    fun start(outputWav: File) {
        // Calcula el tamaño mínimo del buffer necesario para la configuración de audio.
        // Esto es crucial para evitar latencia o pérdida de datos.
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_24BIT_PACKED)

        // Usamos un buffer más grande que el mínimo (x2) para más estabilidad,
        // asegurando un mínimo de 4096 bytes.
        val bufferSize = (minBuffer * 2).coerceAtLeast(4096)

        // Nota: Los permisos (RECORD_AUDIO) se asume que ya han sido concedidos
        // por la UI antes de llamar a este método.
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,        // Fuente de audio: Micrófono
            sampleRate,                           // Frecuencia de muestreo
            AudioFormat.CHANNEL_IN_MONO,          // Configuración de canal (mono)
            AudioFormat.ENCODING_PCM_24BIT_PACKED,       // Formato de datos (16 bits por muestra) exigen 24
            bufferSize                            // Tamaño del buffer de grabación
        )

        // Verifica si el grabador se inicializó correctamente.
        if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("AudioRecord no se pudo inicializar. ¿Faltan permisos?")
        }

        // Define los archivos temporales y finales.
        // pcmFile guardará los datos crudos (ej. "rec-123.raw")
        pcmFile = File(outputWav.parentFile, outputWav.nameWithoutExtension + ".raw")
        // wavFile es la referencia al archivo final (ej. "rec-123.wav")
        wavFile = outputWav

        // Actualiza el estado y comienza la captura de audio.
        isRecording = true
        recorder?.startRecording()

        // Inicia un hilo separado para leer los datos del buffer y escribirlos a disco.
        recordingThread = Thread {
            // Usa 'use' para asegurar que el FileOutputStream se cierre automáticamente.
            FileOutputStream(pcmFile!!).use { fos ->
                val buf = ByteArray(bufferSize) // Buffer de lectura

                // Bucle principal: se ejecuta mientras 'isRecording' sea verdadero.
                while (isRecording) {
                    // Lee datos del buffer de hardware de AudioRecord.
                    val read = recorder?.read(buf, 0, buf.size) ?: 0
                    // Si se leyeron datos válidos, escríbelos al archivo PCM crudo.
                    if (read > 0) fos.write(buf, 0, read)
                }
            }
        }.also { it.start() } // Inicia el hilo.
    }

    /**
     * Detiene la grabación y ensambla el archivo WAV final.
     *
     * Señala al hilo de grabación que se detenga, espera a que termine,
     * lee los datos PCM crudos del archivo temporal, escribe el archivo WAV
     * completo (cabecera + datos) y elimina el archivo PCM temporal.
     *
     * @return El archivo [File] final en formato WAV.
     * @throws IllegalStateException Si no se encuentra el archivo PCM o WAV de destino.
     */
    fun stop(): File {
        // Señala al hilo de grabación que debe detenerse.
        isRecording = false

        // Detiene la captura de hardware y libera los recursos.
        recorder?.stop()
        recorder?.release()
        recorder = null

        // Espera a que el hilo de grabación termine de escribir los últimos datos (join).
        recordingThread?.join()
        recordingThread = null

        // --- Ensamblaje del archivo WAV ---

        // Asegura que los archivos existen antes de proceder.
        val raw = pcmFile ?: throw IllegalStateException("Archivo PCM temporal no encontrado")
        val wav = wavFile ?: throw IllegalStateException("Archivo WAV de destino no encontrado")

        // Lee todos los datos crudos de audio del archivo temporal.
        val pcmData = raw.readBytes()

        // Escribe el archivo WAV final (cabecera + datos).
        writeWav(wav, pcmData, sampleRate)

        // Elimina el archivo PCM temporal que ya no necesitamos.
        raw.delete()

        // Devuelve el archivo WAV completo.
        return wav
    }

    /**
     * Escribe la cabecera WAV estándar (formato PCM 44 bytes) seguida de los datos PCM crudos.
     *
     * @param wav El archivo de destino donde se escribirá.
     * @param pcm Los datos de audio crudo (payload).
     * @param sampleRate La frecuencia de muestreo (ej. 48000).
     */
    private fun writeWav(wav: File, pcm: ByteArray, sampleRate: Int) {
        // --- Constantes del formato WAV (PCM 24-bit Mono) ---
        val channels = 1
        val bitsPerSample = 24
        // Cálculo de metadatos requeridos por la cabecera WAV
        val byteRate = sampleRate * channels * bitsPerSample / 8 // Bytes por segundo
        val blockAlign = (channels * bitsPerSample / 8).toShort() // Bytes por muestra (frame)
        val dataSize = pcm.size // Tamaño total de los datos PCM
        val chunkSize = 36 + dataSize // Tamaño total del archivo - 8 bytes (RIFF y chunkSize)

        FileOutputStream(wav).use { out ->
            // Escribe la cabecera WAV (44 bytes)
            // Todos los valores numéricos deben estar en formato Little Endian.

            // "RIFF" chunk
            out.write("RIFF".toByteArray())
            out.write(intLE(chunkSize)) // Tamaño total del archivo - 8
            out.write("WAVE".toByteArray())

            // "fmt " sub-chunk (describe el formato del audio)
            out.write("fmt ".toByteArray())
            out.write(intLE(16))                // Tamaño del sub-chunk (16 para PCM)
            out.write(shortLE(1))               // Formato de audio (1 = PCM, sin compresión)
            out.write(shortLE(channels.toShort())) // Número de canales
            out.write(intLE(sampleRate))        // Frecuencia de muestreo
            out.write(intLE(byteRate))          // Tasa de bytes (SampleRate * Channels * BitsPerSample/8)
            out.write(shortLE(blockAlign))      // Alineación de bloque (Bytes por frame)
            out.write(shortLE(bitsPerSample.toShort())) // Bits por muestra

            // "data" sub-chunk (contiene los datos de audio)
            out.write("data".toByteArray())
            out.write(intLE(dataSize))          // Tamaño de los datos (payload)

            // Escribe los datos de audio PCM crudos
            out.write(pcm)
        }
    }

    // --- Funciones de utilidad para conversión a Little Endian ---

    /** Convierte un Int a un array de 4 bytes en orden Little Endian. */
    private fun intLE(v: Int) = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array()

    /** Convierte un Short a un array de 2 bytes en orden Little Endian. */
    private fun shortLE(v: Short) = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(v).array()
}