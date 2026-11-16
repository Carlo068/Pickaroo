# PIC-61: perfil del usuario autenticado

La pestaña Usuario consulta `GET me` a través de `UserRepository` y `AuthorizedCalls`, que gestiona la renovación de sesión. El encabezado muestra el nombre, correo y roles que devuelve el servicio; ya no usa el perfil fijo de María González.

Ejemplos mock (contraseña `demo1234`): `customer1` muestra Cliente Demo, customer1@pickaroo.test y Cliente. `owner1`, al cambiar a modo cliente, muestra Dueño Demo Uno, owner1@pickaroo.test y Dueño · Cliente.

La pantalla tiene carga y reintento, no inventa datos cuando falla la primera consulta y elimina el perfil mostrado si la sesión vence. La vista previa de Compose utiliza datos explícitos de ejemplo sin iniciar peticiones.

Este cambio solo conecta el encabezado del perfil. Editar nombre, correo o contraseña, direcciones y listas de compra siguen fuera del alcance de PIC-61. Es un PR independiente basado en main.

## Validación

- 271 pruebas unitarias aprobadas sobre la base de main, incluyendo carga, errores, reintentos concurrentes y expiración de sesión del perfil.
- 18 pruebas instrumentadas aprobadas: 3 de perfil y 15 de navegación por roles. Comprueban los datos reales de customer1 y owner1, sus roles distintos y la recuperación de un fallo de GET me.
- Compilan aplicación y APK de pruebas.
