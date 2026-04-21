package com.geminiapps.wifitethering.ui.home

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.EnumMap

object QrGenerator {
    /**
     * Generates a WiFi QR code using the mecard format:
     * WIFI:S:<SSID>;T:<WPA|nopass>;P:<password>;;
     *
     * Special characters in SSID/password must be escaped with a backslash: \ ; , " :
     */
    fun generateWifiQrCode(ssid: String, password: String?): Bitmap? {
        val securityType = if (password.isNullOrEmpty()) "nopass" else "WPA"
        val qrContent = buildString {
            append("WIFI:S:")
            append(escape(ssid))
            append(";T:")
            append(securityType)
            if (!password.isNullOrEmpty()) {
                append(";P:")
                append(escape(password))
            }
            append(";;")
        }
        return try {
            val size = 512
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.MARGIN] = 1

            val bitMatrix = QRCodeWriter().encode(qrContent, BarcodeFormat.QR_CODE, size, size, hints)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\"", "\\\"")
            .replace(":", "\\:")
}
