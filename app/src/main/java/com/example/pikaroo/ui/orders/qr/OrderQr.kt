package com.example.pikaroo.ui.orders.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

private val orderIdPattern = Regex("order_[A-Za-z0-9-]{1,100}")

/** El payload es únicamente el ID; nunca contiene dirección, datos de sesión ni URLs. */
fun parseOrderQr(raw: String): String? = raw.trim().takeIf { orderIdPattern.matches(it) }

fun encodeOrderQr(orderId: String, size: Int = 512): BitMatrix {
    require(parseOrderQr(orderId) == orderId) { "Invalid order ID" }
    return QRCodeWriter().encode(orderId, BarcodeFormat.QR_CODE, size, size,
        mapOf(EncodeHintType.MARGIN to 4))
}
