package com.jsmammen.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.jsmammen.data.model.Visit
import com.jsmammen.data.settings.ClinicSettings
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
        val settings = ClinicSettings(context)
        val hospitalName = settings.hospitalName.ifBlank { "Hospital / Clinic" }
        val doctorName = settings.doctorName.ifBlank { "Doctor" }
        val doctorContact = settings.doctorContact

        val document = PdfDocument()
        val margin = 40f
        val pageWidth = 595f
        val pageHeight = 842f
        val maxWidth = pageWidth - 2 * margin

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), pageNumber).create())
        var canvas = page.canvas
        var y = margin

        val paint = Paint().apply {
            textSize = 10.5f
            color = Color.BLACK
        }
        val bold = Paint(paint).apply { typeface = Typeface.DEFAULT_BOLD }
        val headingPaint = Paint(bold).apply { textSize = 15f; color = Color.rgb(15, 76, 129) }
        val smallPaint = Paint(paint).apply { textSize = 9f; color = Color.rgb(96, 112, 138) }

        fun drawHeader() {
            y = margin
            canvas.drawText(hospitalName, margin, y, headingPaint)
            y += 16f
            canvas.drawText(doctorName, margin, y, bold)
            y += 12f
            if (doctorContact.isNotBlank()) {
                canvas.drawText(doctorContact, margin, y, smallPaint)
                y += 12f
            }
            val linePaint = Paint().apply {
                color = Color.rgb(15, 76, 129)
                strokeWidth = 1f
            }
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 16f
        }

        fun drawFooter() {
            val footerY = pageHeight - margin + 4f
            canvas.drawText(hospitalName, margin, footerY, smallPaint)
            val pageLabel = "Page $pageNumber"
            val textWidth = smallPaint.measureText(pageLabel)
            canvas.drawText(pageLabel, pageWidth - margin - textWidth, footerY, smallPaint)
            val confLabel = "Confidential - Patient Data"
            val confWidth = smallPaint.measureText(confLabel)
            canvas.drawText(confLabel, (pageWidth - confWidth) / 2f, footerY, smallPaint)
        }

        fun newPage() {
            drawFooter()
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), pageNumber).create())
            canvas = page.canvas
            drawHeader()
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - margin - 20f) newPage()
        }

        fun drawWrapped(text: String, paintToUse: Paint, indent: Float = 0f) {
            if (text.isBlank()) return
            var remaining = text.trim()
            while (remaining.isNotEmpty()) {
                val width = maxWidth - indent
                val count = paintToUse.breakText(remaining, true, width, null)
                val safeCount = if (count <= 0) 1 else count
                val line = remaining.substring(0, safeCount)
                ensureSpace(13f)
                canvas.drawText(line, margin + indent, y, paintToUse)
                y += 13f
                remaining = remaining.substring(safeCount).trimStart()
            }
        }

        drawHeader()

        canvas.drawText(title, margin, y, Paint(bold).apply { textSize = 13f })
        y += 16f
        drawWrapped("Generated ${formatTimestamp(System.currentTimeMillis())}", smallPaint)
        drawWrapped("Patients: ${patients.size}", smallPaint)
        y += 6f

        patients.forEach { patient ->
            ensureSpace(30f)
            canvas.drawText(patient.name, margin, y, bold)
            y += 13f
            drawWrapped("UID: ${patient.uid}", paint)
            patient.idNumber?.let { drawWrapped("ID: $it", paint) }
            patient.ageYears?.let { drawWrapped("Age: $it", paint) }
            patient.sex?.let { drawWrapped("Sex: $it", paint) }
            patient.phone?.let { drawWrapped("Phone: $it", paint) }

            val visits = visitsByPatient[patient.id].orEmpty()
            if (visits.isNotEmpty()) {
                drawWrapped("Visits:", bold)
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
            y += 8f
        }

        drawFooter()
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
