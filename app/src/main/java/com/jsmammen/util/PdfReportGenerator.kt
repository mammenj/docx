package com.jsmammen.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.jsmammen.data.model.Visit
import com.jsmammen.ui.util.formatTimestamp
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object PdfReportGenerator {

    fun generate(
        context: Context,
        title: String,
        patients: List<com.jsmammen.data.model.Patient>,
        visitsByPatient: Map<Long, List<Visit>>,
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        val paint = Paint().apply {
            textSize = 11f
            color = Color.BLACK
        }
        val bold = Paint(paint).apply { typeface = Typeface.DEFAULT_BOLD }
        val titlePaint = Paint(bold).apply { textSize = 16f }

        val margin = 40f
        val maxWidth = 595f - 2 * margin
        var y = margin

        fun newPage() {
            document.finishPage(page)
            page = document.startPage(pageInfo)
            canvas = page.canvas
            y = margin
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > 842f - margin) newPage()
        }

        fun drawWrapped(text: String, paintToUse: Paint, indent: Float = 0f) {
            if (text.isBlank()) return
            var remaining = text.trim()
            while (remaining.isNotEmpty()) {
                val width = maxWidth - indent
                val count = paintToUse.breakText(remaining, true, width, null)
                val safeCount = if (count <= 0) 1 else count
                val line = remaining.substring(0, safeCount)
                ensureSpace(14f)
                canvas.drawText(line, margin + indent, y, paintToUse)
                y += 14f
                remaining = remaining.substring(safeCount).trimStart()
            }
        }

        canvas.drawText(title, margin, y, titlePaint)
        y += 20f
        drawWrapped("Generated ${formatTimestamp(System.currentTimeMillis())}", paint)
        drawWrapped("Patients: ${patients.size}", paint)
        y += 8f

        patients.forEach { patient ->
            ensureSpace(40f)
            canvas.drawText(patient.name, margin, y, bold)
            y += 14f
            drawWrapped("UID: ${patient.uid}", paint)
            patient.idNumber?.let { drawWrapped("ID: $it", paint) }
            patient.ageYears?.let { drawWrapped("Age: $it", paint) }
            patient.sex?.let { drawWrapped("Sex: $it", paint) }
            patient.phone?.let { drawWrapped("Phone: $it", paint) }

            val visits = visitsByPatient[patient.id].orEmpty()
            if (visits.isNotEmpty()) {
                drawWrapped("Visits:", bold, 0f)
                visits.forEach { visit ->
                    drawWrapped("- ${formatTimestamp(visit.recordedAt)}", paint, 4f)
                    if (visit.symptoms.isNotBlank()) {
                        drawWrapped("  Symptoms: ${visit.symptoms}", paint, 4f)
                    }
                    if (visit.notes.isNotBlank()) {
                        drawWrapped("  Notes: ${visit.notes}", paint, 4f)
                    }
                    val vitals = vitalsSummary(visit)
                    if (vitals.isNotEmpty()) {
                        drawWrapped("  Vitals: ${vitals.joinToString(", ")}", paint, 4f)
                    }
                }
            }
            y += 10f
        }

        document.finishPage(page)
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.cacheDir
        val file = File(dir, "OutpatientReport_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun vitalsSummary(visit: Visit): List<String> = buildList {
        if (visit.systolic != null && visit.diastolic != null) {
            add("BP ${visit.systolic}/${visit.diastolic} mmHg")
        }
        if (visit.glucoseValue != null && visit.glucoseUnit != null) {
            add("Glucose ${formatMetric(visit.glucoseValue)} ${visit.glucoseUnit.label}")
        }
        visit.pulse?.let { add("Pulse $it bpm") }
        visit.temperatureC?.let { add("Temp ${formatMetric(it)} C") }
        visit.spo2?.let { add("SpO2 $it%") }
        visit.weightKg?.let { add("Weight ${formatMetric(it)} kg") }
    }

    private fun formatMetric(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString()
        else String.format(Locale.getDefault(), "%.1f", value)
}
