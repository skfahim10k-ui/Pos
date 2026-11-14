package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object QrBarcodeRenderer {

    fun generateQrBitmap(content: String, sizePx: Int = 480): Bitmap? {
        if (content.isBlank()) return null
        return renderMatrix(
            content = content,
            format = BarcodeFormat.QR_CODE,
            width = sizePx,
            height = sizePx,
            hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )
        )
    }

    fun generateBarcodeBitmap(content: String, widthPx: Int = 640, heightPx: Int = 180): Bitmap? {
        if (content.isBlank()) return null
        return renderMatrix(
            content = content,
            format = BarcodeFormat.CODE_128,
            width = widthPx,
            height = heightPx,
            hints = mapOf(EncodeHintType.MARGIN to 2)
        )
    }

    private fun renderMatrix(
        content: String,
        format: BarcodeFormat,
        width: Int,
        height: Int,
        hints: Map<EncodeHintType, Any>
    ): Bitmap? = try {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(content, format, width, height, hints)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    } catch (_: Exception) {
        null
    }
}
