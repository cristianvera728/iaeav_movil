package es.ua.iuii.iaeav.ui.results

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.io.font.constants.StandardFonts
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Genera un PDF con los resultados del análisis y lo guarda en el almacenamiento.
 *
 * @param context Contexto de la aplicación
 * @param prediction Indica si el paciente padece Alzheimer
 * @param confidence Nivel de confianza del modelo (0.0 a 1.0)
 * @param transcription Texto de la transcripción
 * @param explicability Explicación del modelo
 */
fun generarPDFResultados(
    context: Context,
    prediction: String,
    confidence: String,
    transcription: String,
    explicability: String
) {
    try {
        // Crear nombre de archivo con fecha y hora
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "Resultados_Alzheimer_${dateFormat.format(Date())}.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)

        // Inicializar PDF
        val writer = PdfWriter(file)
        val pdfDocument = PdfDocument(writer)
        val document = Document(pdfDocument)

        // Configurar fuentes
        val boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
        val regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA)

        // Colores personalizados (puedes ajustarlos según tu tema)
        val primaryColor = DeviceRgb(103, 80, 164) // Color primario similar a Material
        val secondaryColor = DeviceRgb(158, 158, 158) // Color secundario

        // --- TÍTULO PRINCIPAL ---
        val titulo = Paragraph("Resultados de la Prueba de Alzheimer")
            .setFont(boldFont)
            .setFontSize(20f)
            .setFontColor(primaryColor)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(10f)
        document.add(titulo)

        // Fecha del reporte
        val fecha = Paragraph("Fecha: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
            .setFont(regularFont)
            .setFontSize(10f)
            .setFontColor(ColorConstants.GRAY)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20f)
        document.add(fecha)

        // --- RESULTADO PRINCIPAL ---
        val predictionText = if (prediction == "positive") "padece de Alzheimer" else "no padece de Alzheimer"
        val confidencePercentage = confidence
        
        val resultado = Paragraph()
            .add(Text("El modelo considera que el paciente ")
                .setFont(regularFont)
                .setFontSize(14f))
            .add(Text(predictionText)
                .setFont(boldFont)
                .setFontSize(14f)
                .setFontColor(if (prediction == "positive") DeviceRgb(211, 47, 47) else DeviceRgb(56, 142, 60)))
            .add(Text(" con una confianza del ")
                .setFont(regularFont)
                .setFontSize(14f))
            .add(Text("$confidencePercentage%")
                .setFont(boldFont)
                .setFontSize(14f))
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(30f)
        document.add(resultado)

        // --- SECCIÓN TRANSCRIPCIÓN ---
        val tituloTranscripcion = Paragraph("Transcripción")
            .setFont(boldFont)
            .setFontSize(16f)
            .setFontColor(primaryColor)
            .setMarginBottom(5f)
        document.add(tituloTranscripcion)

        val descripcionTranscripcion = Paragraph("Transcripción de las respuestas del paciente.")
            .setFont(regularFont)
            .setFontSize(12f)
            .setFontColor(secondaryColor)
            .setMarginBottom(10f)
        document.add(descripcionTranscripcion)

        val textoTranscripcion = Paragraph(transcription)
            .setFont(regularFont)
            .setFontSize(11f)
            .setTextAlignment(TextAlignment.JUSTIFIED)
            .setMarginBottom(25f)
            .setPadding(10f)
            .setBackgroundColor(DeviceRgb(245, 245, 245))
        document.add(textoTranscripcion)

        // --- SECCIÓN EXPLICABILIDAD ---
        val tituloExplicabilidad = Paragraph("Explicabilidad")
            .setFont(boldFont)
            .setFontSize(16f)
            .setFontColor(primaryColor)
            .setMarginBottom(5f)
        document.add(tituloExplicabilidad)

        val descripcionExplicabilidad = Paragraph("Explicación de en qué se basa el modelo para generar sus resultados.")
            .setFont(regularFont)
            .setFontSize(12f)
            .setFontColor(secondaryColor)
            .setMarginBottom(10f)
        document.add(descripcionExplicabilidad)

        val textoExplicabilidad = Paragraph(explicability)
            .setFont(regularFont)
            .setFontSize(11f)
            .setTextAlignment(TextAlignment.JUSTIFIED)
            .setPadding(10f)
            .setBackgroundColor(DeviceRgb(245, 245, 245))
        document.add(textoExplicabilidad)

        // --- PIE DE PÁGINA ---
        val piePagina = Paragraph("\n\nDocumento generado automáticamente por IAEAV")
            .setFont(regularFont)
            .setFontSize(9f)
            .setFontColor(ColorConstants.GRAY)
            .setTextAlignment(TextAlignment.CENTER)
        document.add(piePagina)

        // Cerrar documento
        document.close()

        // Mostrar mensaje de éxito y abrir el PDF
        Toast.makeText(context, "PDF generado exitosamente", Toast.LENGTH_SHORT).show()
        abrirPDF(file, context)

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/**
 * Abre el PDF generado con la aplicación predeterminada del sistema.
 */
private fun abrirPDF(file: File, context: Context) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No se encontró una app para abrir PDF", Toast.LENGTH_SHORT).show()
    }
}