package com.geminiapps.wifitethering.ui.home

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.EnumMap

object QrGenerator {
    /**
     * Generates a WiFi QR code string in the format:
     * WIFI:S:<SSID>;T:<WPA|WEP|nopass>;P:<password>;;
     */
    fun generateWifiQrCode(ssid: String, password: String?, securityType: String = "WPA"): Bitmap? {
        val qrContent = "WIFI:S:$ssid;T:$securityType;P:${password ?: ""};;"
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
            e.printStackTrace()
            null
        }
    }
}
