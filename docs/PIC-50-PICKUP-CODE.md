# PIC-50: verificar el código de recogida

En modo tienda (`staff1` o `owner1`, contraseña `demo1234`), un pedido pickup listo requiere el código que el cliente ve en su comprobante o seguimiento. El botón Confirmar permanece deshabilitado si el campo está vacío. Un código incorrecto mantiene el diálogo abierto y no cambia ni el pedido ni sus eventos. Se aceptan minúsculas y espacios alrededor del código.

La comprobación se realiza en el servicio mock, no solo en la interfaz: también se rechaza un intento sin código enviado directamente al contrato. Se conservan los permisos por sucursal, la validación del estado anterior y la protección frente a repeticiones. El ID del QR identifica la orden; no sustituye al código de recogida. Escanear el código queda fuera de este alcance.

Este cambio parte de PIC-49. El PR usa su rama como base para mostrar únicamente los cambios de PIC-50.

## Validación

- 279 pruebas unitarias aprobadas, incluyendo código ausente, vacío, incorrecto, normalización y rechazo de reutilización.
- 3 pruebas de interfaz de tienda aprobadas; el flujo prueba primero un código incorrecto y después el válido, y consulta el estado final como cliente.
- Aplicación y APK de pruebas compilan correctamente.
