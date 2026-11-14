package com.example.pikaroo.ui.auth.model

private val EMAIL = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")

fun RegisterState.validationError(): String? = when {
    listOf(name, username, email, password, confirmPassword).any { it.isBlank() } -> "Completa todos los campos"
    username.trim().length !in 3..64 || username.trim().any { it.isWhitespace() } -> "El usuario debe tener entre 3 y 64 caracteres, sin espacios"
    !EMAIL.matches(email.trim()) -> "Ingresa un correo electrónico válido"
    password.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
    password != confirmPassword -> "Las contraseñas no coinciden"
    else -> null
}

fun registrationErrorMessage(code: String?): String = when (code) {
    "USERNAME_TAKEN" -> "El nombre de usuario ya está en uso"
    "EMAIL_TAKEN" -> "El correo electrónico ya está en uso"
    "INVALID_NAME" -> "Ingresa tu nombre"
    "INVALID_USERNAME" -> "El usuario debe tener entre 3 y 64 caracteres, sin espacios"
    "INVALID_EMAIL" -> "Ingresa un correo electrónico válido"
    "WEAK_PASSWORD" -> "La contraseña debe tener al menos 6 caracteres"
    else -> "No se pudo crear la cuenta. Intenta de nuevo."
}
