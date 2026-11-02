package com.sumocarrito.export

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.sumocarrito.data.local.PurchaseEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object Exporter {
    private fun fmt(ms: Long) = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(ms))
    private fun dir(ctx: Context) = File(ctx.cacheDir, "exports").apply { mkdirs() }

    /** CSV con BOM UTF-8 y separador ';' para que Excel (config. regional ES) lo abra directo. */
    fun csv(ctx: Context, list: List<PurchaseEntity>): File {
        val f = File(dir(ctx), "sumocarrito_historial.csv")
        f.writeText("\uFEFFFecha;Establecimiento;Total USD;Tasa\n" + list.joinToString("\n") {
            "${fmt(it.epochMillis)};${it.store.replace(';', ',')};${it.totalUsd.toPlainString()};${it.exchangeRate.toPlainString()}"
        })
        return f
    }

    fun pdf(ctx: Context, list: List<PurchaseEntity>): File {
        val doc = PdfDocument(); val paint = Paint().apply { textSize = 12f }
        var page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create()); var y = 50f; var n = 1
        page.canvas.drawText("SumoCarrito - Historial de compras", 40f, y, paint.apply { textSize = 16f }); paint.textSize = 12f; y += 30f
        for (p in list) {
            if (y > 800f) { doc.finishPage(page); n++; page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, n).create()); y = 50f }
            page.canvas.drawText("${fmt(p.epochMillis)}   ${p.store}   $${p.totalUsd.toPlainString()}   (tasa ${p.exchangeRate.toPlainString()})", 40f, y, paint); y += 20f
        }
        doc.finishPage(page)
        val f = File(dir(ctx), "sumocarrito_historial.pdf"); f.outputStream().use { doc.writeTo(it) }; doc.close(); return f
    }

    fun share(ctx: Context, f: File, mime: String) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = mime; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Exportar"))
    }
}
