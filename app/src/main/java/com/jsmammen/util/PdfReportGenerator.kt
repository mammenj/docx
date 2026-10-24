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

    private val columns = listOf(
        Column("Date", 86f),
        Column("BP (mmHg)", 62f),
        Column("Glucose", 72f),
        Column("Pulse", 52f),
        Column("Temp (C)", 52f),
        Column("SpO2", 48f),
        Column("Weight (kg)", 60f),
    )

    private data class Column(val title: String, val width: Float)

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
        val pageWidth = 595f
        val pageHeight = 842f
        val margin = 40f
        val maxWidth = pageWidth - 2 * margin

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), pageNumber).create())
        var canvas = page.canvas
        var y = margin

        val paint = Paint().apply { textSize = 9.5f; color = Color.BLACK }
        val bold = Paint(paint).apply { typeface = Typeface.DEFAULT_BOLD }
        val headingPaint = Paint(bold).apply { textSize = 14f; color = Color.rgb(15, 76, 129) }
        val smallPaint = Paint(paint).apply { textSize = 8.5f; color = Color.rgb(96, 112, 138) }
        val tableHeaderPaint = Paint(bold).apply { textSize = 8.5f; color = Color.WHITE }
        val tablePaint = Paint(paint).apply { textSize = 8.5f }

        fun drawHeader() {
            y = margin
            canvas.drawText(hospitalName, margin, y, headingPaint)
            y += 15f
            canvas.drawText(doctorName, margin, y, bold)
            y += 11f
            if (doctorContact.isNotBlank()) {
                canvas.drawText(doctorContact, margin, y, smallPaint)
                y += 11f
            }
            val linePaint = Paint().apply { color = Color.rgb(15, 76, 129); strokeWidth = 1f }
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 14f
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
                ensureSpace(12f)
                canvas.drawText(line, margin + indent, y, paintToUse)
                y += 12f
                remaining = remaining.substring(safeCount).trimStart()
            }
        }

        drawHeader()

        canvas.drawText(title, margin, y, Paint(bold).apply { textSize = 12f })
        y += 15f
        drawWrapped("Generated ${formatTimestamp(System.currentTimeMillis())}", smallPaint)
        drawWrapped("Patients: ${patients.size}", smallPaint)
        y += 6f

        patients.forEach { patient ->
            ensureSpace(60f)

            // Patient header block
            canvas.drawText(patient.name, margin, y, Paint(bold).apply { textSize = 11.5f })
            y += 13f
            val meta = listOfNotNull(
                "UID: ${patient.uid}",
                patient.idNumber?.let { "ID: $it" },
                patient.ageYears?.let { "Age: $it" },
                patient.sex?.let { "Sex: $it" },
                patient.phone?.let { "Phone: $it" },
            ).joinToString("   |   ")
            drawWrapped(meta, smallPaint)
            y += 4f

            val visits = visitsByPatient[patient.id].orEmpty()

            // Table header
            if (visits.isNotEmpty()) {
                ensureSpace(16f)
                val headerBg = Paint().apply { color = Color.rgb(15, 76, 129) }
                canvas.drawRect(margin, y - 10f, margin + columns.sumOf { it.width.toDouble() }.toFloat(), y + 4f, headerBg)
                var x = margin
                columns.forEach { col ->
                    canvas.drawText(col.title, x + 2f, y, tableHeaderPaint)
                    x += col.width
                }
                y += 6f
                val linePaint = Paint().apply { color = Color.rgb(200, 210, 225); strokeWidth = 0.5f }
                canvas.drawLine(margin, y, margin + columns.sumOf { it.width.toDouble() }.toFloat(), y, linePaint)
                y += 10f

                visits.forEach { visit ->
                    ensureSpace(14f)
                    x = margin
                    val cells = listOf(
                        formatTimestamp(visit.recordedAt).take(16),
                        if (visit.systolic != null && visit.diastolic != null) "${visit.systolic}/${visit.diastolic}" else "-",
                        if (visit.glucoseValue != null && visit.glucoseUnit != null) "${formatMetric(visit.glucoseValue)} ${visit.glucoseUnit.label}" else "-",
                        visit.pulse?.toString() ?: "-",
                        visit.temperatureC?.let { formatMetric(it) } ?: "-",
                        visit.spo2?.toString() ?: "-",
                        visit.weightKg?.let { formatMetric(it) } ?: "-",
                    )
                    cells.forEachIndexed { index, cell ->
                        val col = columns[index]
                        val text = truncate(tablePaint, cell, col.width - 4f)
                        canvas.drawText(text, x + 2f, y, tablePaint)
                        x += col.width
                    }
                    y += 13f
                }
                y += 4f
            } else {
                drawWrapped("No visits in this report.", smallPaint)
                y += 4f
            }

            // Notes and symptoms below the table
            val visitsWithText = visits.filter { it.symptoms.isNotBlank() || it.notes.isNotBlank() }
            if (visitsWithText.isNotEmpty()) {
                drawWrapped("Visit notes:", bold)
                visitsWithText.forEach { visit ->
                    drawWrapped("- ${formatTimestamp(visit.recordedAt)}", bold, 4f)
                    if (visit.symptoms.isNotBlank()) {
                        drawWrapped("  Symptoms: ${visit.symptoms}", paint, 4f)
                    }
                    if (visit.notes.isNotBlank()) {
                        drawWrapped("  Notes: ${visit.notes}", paint, 4f)
                    }
                }
            }

            // Divider between patients
            val dividerPaint = Paint().apply { color = Color.rgb(220, 228, 240); strokeWidth = 0.5f }
            canvas.drawLine(margin, y + 2f, pageWidth - margin, y + 2f, dividerPaint)
            y += 10f
        }

        drawFooter()
        document.finishPage(page)

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.cacheDir
        val file = File(dir, "OutpatientReport_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun truncate(paint: Paint, text: String, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + "...") > maxWidth) {
            end--
        }
        return if (end <= 0) "" else text.substring(0, end) + "..."
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
