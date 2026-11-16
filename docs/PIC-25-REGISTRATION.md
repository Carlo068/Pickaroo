# PIC-25: registro funcional

La pestaña Registrarse solicita nombre, nombre de usuario, correo, contraseña y confirmación. Envía `auth/register` y guarda la sesión devuelta antes de navegar al modo cliente. El mock existente asigna exclusivamente el rol customer y persiste el usuario, por lo que puede cerrar sesión y volver a entrar con sus credenciales.

Se validan campos requeridos, usuario de 3 a 64 caracteres sin espacios, formato de correo, contraseña de al menos 6 caracteres (igual al contrato mock) y confirmación coincidente. Los conflictos de usuario o correo duplicado se muestran por separado. Un fallo conserva los campos para corregirlos o reintentar; el registro no se reintenta automáticamente, evitando repetir una petición que podría haber llegado al servidor.

Los campos y el envío se deshabilitan mientras se crea la cuenta. El formulario es desplazable para acceder a todos los campos y al botón con el teclado abierto. Recuperar contraseña sigue fuera del alcance de esta tarea.

Rama independiente basada en main: no incluye los cambios de tienda de PIC-49 o PIC-50.

## Validación

- 273 pruebas unitarias aprobadas sobre la base de main, incluyendo validaciones de registro.
- 24 pruebas instrumentadas aprobadas: 9 de registro y 15 de navegación por roles. Se prueban cada mensaje de validación, usuario y correo duplicados, recuperación de un fallo y registro seguido de cierre de sesión e inicio con la cuenta nueva.
- Compilan aplicación y APK de pruebas.
