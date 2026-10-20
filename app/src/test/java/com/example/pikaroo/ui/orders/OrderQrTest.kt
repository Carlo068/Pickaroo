package com.example.pikaroo.ui.orders

import com.example.pikaroo.ui.orders.qr.encodeOrderQr
import com.example.pikaroo.ui.orders.qr.parseOrderQr
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.*
import org.junit.Test

class OrderQrTest {
    @Test fun `generated QR decodes to the exact order id including the uuid`() {
        for (id in listOf("order_1001", "order_c5364737-14ee-4af3-bf4b-6ec697d50bae")) {
            val matrix = encodeOrderQr(id)
            val pixels = IntArray(matrix.width * matrix.height) { index ->
                if (matrix[index % matrix.width, index / matrix.width]) 0xff000000.toInt() else 0xffffffff.toInt()
            }
            val image = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(matrix.width, matrix.height, pixels)))
            assertEquals(id, QRCodeReader().decode(image).text)
        }
    }

    @Test fun `only raw supported ids are accepted and whitespace is trimmed`() {
        assertEquals("order_1001", parseOrderQr("  order_1001\n"))
        for (raw in listOf("", "https://example.com/order_1001", "order_../private", "order_" + "a".repeat(101))) {
            assertNull(parseOrderQr(raw))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid stored identifiers cannot become misleading QR codes`() {
        encodeOrderQr("not an order")
    }
}
