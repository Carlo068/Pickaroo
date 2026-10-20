# Demo académica: pedidos con QR

## Probar el flujo

1. Ejecuta la variante `debug` y entra como `customer1` con `demo1234` (usuario ficticio de `docs/mock/users.json`).
2. Agrega productos al carrito y pulsa **Continuar al pago**.
3. En **Ordenar**, elige **Pickup** o **Domicilio**. Para Domicilio, escribe una dirección.
4. Pulsa **Revisar pedido** y **Confirmar pedido**. El carrito se descuenta solo después de guardar.
5. El comprobante muestra ID, QR, productos, cantidades, subtotal, envío, total, entrega y estado. El pago nuevo queda pendiente, en tienda o contra entrega.
6. Regresa a **Ordenar → Último pedido** para reabrirlo. También puedes cerrar y volver a abrir la app: las órdenes quedan en el almacenamiento mock del dispositivo.
7. Usa **Ordenar → Consultar QR** para escanear el código o escribir/pegar su ID. **Copiar ID** está en el comprobante.

Para escanear el propio pedido, muestra una captura del QR en otra pantalla y léela con el dispositivo donde guardaste la orden. Si cancelas la cámara o deniegas su permiso, puedes introducir el ID manualmente.

## Alcance

- El QR contiene únicamente el ID exacto de la orden (`order_…`). Es una consulta de lectura; no acredita pago ni marca recogida o entrega.
- Solo se consultan órdenes del usuario autenticado. Un ID desconocido o ajeno no abre datos.
- Los datos viven en el mock local: otra instalación no recibe las órdenes. Borrar datos o desinstalar elimina esa persistencia.
- Se usa la tienda de ejemplo `shop_1` (Pickaroo Centro). Pickup muestra un horario de ejemplo calculado al confirmar; Domicilio usa dirección y tarifa fija de la tienda. No hay mapas, cobertura ni cupos en checkout.
- La generación/lectura usa [ZXing](https://github.com/zxing/zxing) y el lector de cámara [ZXing Android Embedded](https://github.com/journeyapps/zxing-android-embedded).

## Verificación

```bash
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest --max-workers=2
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.example.pikaroo.ui.order --max-workers=2
```

Las pruebas unitarias cubren persistencia, autorización, reintentos sin duplicados, conservación del carrito, recuperación del comprobante y decodificación del QR. `QrOrderFlowTest` genera órdenes Pickup/Domicilio, decodifica el QR realmente renderizado, reabre el comprobante y consulta el mismo ID después de simular la cancelación del lector. La captura física con cámara se verifica manualmente en el dispositivo.
