# PIC-49: pedidos de la sucursal

Entrar con `staff1` o `owner1` (contraseña `demo1234`) muestra la cola de Pickaroo Centro. `owner2` solo ve su propia sucursal. La lista se actualiza cada cinco segundos mientras la pantalla está visible.

Cada pedido avanza de recibido a aceptado, en preparación y listo. En pickup, confirmar la entrega al cliente cierra el pedido como recogido; la validación del código queda para PIC-50. Los pedidos a domicilio permanecen listos hasta que los tome un repartidor, que conserva el control de su ruta y entrega. No se cambia el estado del pago.

El mock conserva los pedidos y añade un evento por transición. El seguimiento del cliente lee los mismos datos. Rechaza accesos de otra sucursal, saltos, retrocesos y solicitudes basadas en un estado antiguo.

Los contratos `ShopOrdersApi` / `ShopOrdersRepository` siguen la arquitectura existente; el backend remoto aún no está conectado. No se incorporan los cambios locales de Firebase de la otra rama.

## Validación

- 277 pruebas unitarias aprobadas, incluyendo la matriz completa de transiciones y permisos de sucursal.
- 18 pruebas instrumentadas aprobadas: `ShopOrdersFlowTest` y `RoleNavigationTest`.
- Compilación de aplicación y APK de pruebas correcta.
- Emulador API 36.1: 2 núcleos, gráficos `host`, arranque con `-feature -Vulkan -no-snapshot-load` para evitar los bloqueos gráficos observados en este Mac.
