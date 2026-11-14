package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import com.example.data.model.Sale
import com.example.data.model.ShopProfile
import com.example.ui.i18n.formatDateTime
import com.example.ui.i18n.formatQty
import com.example.ui.i18n.formatTaka
import java.io.FileOutputStream

object CashMemoExporter {

    fun buildBilingualMemoText(
        sale: Sale,
        shop: ShopProfile,
        mode: String // "bn", "en", "both"
    ): String {
        val sb = StringBuilder()
        sb.appendLine("================================")
        sb.appendLine(shop.businessName.ifBlank { "Fahim Siam Traders" })
        if (shop.phone.isNotBlank()) sb.appendLine("Phone / ফোন: ${shop.phone}")
        if (shop.address.isNotBlank()) sb.appendLine("Address / ঠিকানা: ${shop.address}")
        sb.appendLine("--------------------------------")
        when (mode) {
            "bn" -> {
                sb.appendLine("ক্যাশ মেমো নং: ${sale.memoNumber}")
                sb.appendLine("তারিখ: ${formatDateTime(sale.saleEpochMs)}")
                sb.appendLine("ক্রেতা: ${sale.customerName}")
                if (sale.customerPhone.isNotBlank()) sb.appendLine("মোবাইল: ${sale.customerPhone}")
                sb.appendLine("--------------------------------")
                sb.appendLine("পণ্য | পরিমাণ × দর = মোট")
            }
            "en" -> {
                sb.appendLine("CASH MEMO: ${sale.memoNumber}")
                sb.appendLine("Date: ${formatDateTime(sale.saleEpochMs)}")
                sb.appendLine("Customer: ${sale.customerName}")
                if (sale.customerPhone.isNotBlank()) sb.appendLine("Phone: ${sale.customerPhone}")
                sb.appendLine("--------------------------------")
                sb.appendLine("Item | Qty × Rate = Total")
            }
            else -> {
                sb.appendLine("ক্যাশ মেমো / CASH MEMO: ${sale.memoNumber}")
                sb.appendLine("তারিখ / Date: ${formatDateTime(sale.saleEpochMs)}")
                sb.appendLine("ক্রেতা / Customer: ${sale.customerName}")
                if (sale.customerPhone.isNotBlank()) sb.appendLine("ফোন / Phone: ${sale.customerPhone}")
                sb.appendLine("--------------------------------")
                sb.appendLine("পণ্য / Item | পরিমাণ / Qty × দর = মোট")
            }
        }
        sb.appendLine("--------------------------------")
        sale.items.forEachIndexed { idx, item ->
            val title = when (mode) {
                "bn" -> item.productNameBn.ifBlank { item.productName }
                "en" -> item.productName
                else -> if (item.productNameBn.isNotBlank()) "${item.productNameBn} (${item.productName})" else item.productName
            }
            sb.appendLine("${idx + 1}. $title")
            sb.appendLine("   ${formatQty(item.qty)} ${item.unit} × ${formatTaka(item.unitPrice)} = ${formatTaka(item.lineTotal)}")
        }
        sb.appendLine("--------------------------------")
        when (mode) {
            "bn" -> {
                sb.appendLine("উপমোট: ${formatTaka(sale.subtotal)}")
                if (sale.discount > 0) sb.appendLine("ছাড়: -${formatTaka(sale.discount)}")
                sb.appendLine("সর্বমোট বিল: ${formatTaka(sale.totalAmount)}")
                sb.appendLine("জমা / পরিশোধ (${sale.paymentMethod}): ${formatTaka(sale.paidAmount)}")
                if (sale.dueAmount > 0) sb.appendLine("বাকি (Due): ${formatTaka(sale.dueAmount)}")
                sb.appendLine("--------------------------------")
                sb.appendLine(shop.receiptFooterBn.ifBlank { "ধন্যবাদ, আবার আসবেন!" })
            }
            "en" -> {
                sb.appendLine("Subtotal: ${formatTaka(sale.subtotal)}")
                if (sale.discount > 0) sb.appendLine("Discount: -${formatTaka(sale.discount)}")
                sb.appendLine("Net Total: ${formatTaka(sale.totalAmount)}")
                sb.appendLine("Paid (${sale.paymentMethod}): ${formatTaka(sale.paidAmount)}")
                if (sale.dueAmount > 0) sb.appendLine("Due Balance: ${formatTaka(sale.dueAmount)}")
                sb.appendLine("--------------------------------")
                sb.appendLine(shop.receiptFooterEn.ifBlank { "Thank you for your business!" })
            }
            else -> {
                sb.appendLine("উপমোট / Subtotal: ${formatTaka(sale.subtotal)}")
                if (sale.discount > 0) sb.appendLine("ছাড় / Discount: -${formatTaka(sale.discount)}")
                sb.appendLine("সর্বমোট / Net Total: ${formatTaka(sale.totalAmount)}")
                sb.appendLine("পরিশোধ / Paid (${sale.paymentMethod}): ${formatTaka(sale.paidAmount)}")
                if (sale.dueAmount > 0) sb.appendLine("বাকি / Due: ${formatTaka(sale.dueAmount)}")
                sb.appendLine("--------------------------------")
                sb.appendLine(shop.receiptFooterBn.ifBlank { "ধন্যবাদ, আবার আসবেন!" })
                sb.appendLine(shop.receiptFooterEn.ifBlank { "Thank you for your business!" })
            }
        }
        sb.appendLine("================================")
        return sb.toString()
    }

    fun shareCashMemoText(context: Context, sale: Sale, shop: ShopProfile, mode: String) {
        val text = buildBilingualMemoText(sale, shop, mode)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${shop.businessName} - Memo ${sale.memoNumber}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Cash Memo / ক্যাশ মেমো শেয়ার করুন"))
    }

    fun printCashMemo(context: Context, sale: Sale, shop: ShopProfile, mode: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "${shop.businessName} Memo ${sale.memoNumber}"
        val adapter = object : PrintDocumentAdapter() {
            private var pdfDoc: PrintedPdfDocument? = null

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                pdfDoc = PrintedPdfDocument(context, newAttributes)
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("CashMemo_${sale.memoNumber}.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                val doc = pdfDoc ?: return
                val page = doc.startPage(0)
                drawMemoOnCanvas(page.canvas, sale, shop, mode)
                doc.finishPage(page)
                try {
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        doc.writeTo(out)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                } finally {
                    doc.close()
                    pdfDoc = null
                }
            }
        }
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }

    private fun drawMemoOnCanvas(canvas: Canvas, sale: Sale, shop: ShopProfile, mode: String) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 11f
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 40f
        val left = 36f
        val right = canvas.width - 36f

        canvas.drawText(shop.businessName.ifBlank { "Fahim Siam Traders" }, left, y, titlePaint)
        y += 18f
        if (shop.phone.isNotBlank() || shop.address.isNotBlank()) {
            canvas.drawText("${shop.phone}  ${shop.address}".trim(), left, y, bodyPaint)
            y += 16f
        }
        canvas.drawLine(left, y, right, y, linePaint)
        y += 18f

        val lines = buildBilingualMemoText(sale, shop, mode).lines()
        lines.drop(4).forEach { line ->
            val paint = if (line.contains("Total") || line.contains("সর্বমোট") || line.contains("MEMO") || line.contains("মেমো")) boldPaint else bodyPaint
            if (line.startsWith("---")) {
                canvas.drawLine(left, y - 4f, right, y - 4f, linePaint)
                y += 10f
            } else {
                canvas.drawText(line, left, y, paint)
                y += 15f
            }
        }
    }
}
