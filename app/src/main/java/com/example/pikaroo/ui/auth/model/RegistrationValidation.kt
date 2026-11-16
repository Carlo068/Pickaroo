package com.example.pikaroo.ui.auth.model

import com.example.pikaroo.R
import com.example.pikaroo.common.text.UiText

private val EMAIL = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")

fun RegisterState.validationError(): UiText? = when {
    listOf(name, username, email, password, confirmPassword).any { it.isBlank() } -> UiText.Res(R.string.register_error_required)
    username.trim().length !in 3..64 || username.trim().any { it.isWhitespace() } -> UiText.Res(R.string.register_error_username)
    !EMAIL.matches(email.trim()) -> UiText.Res(R.string.register_error_email)
    password.length < 6 -> UiText.Res(R.string.register_error_password_short)
    password != confirmPassword -> UiText.Res(R.string.register_error_password_mismatch)
    else -> null
}

fun registrationErrorMessage(code: String?): UiText = UiText.Res(when (code) {
    "USERNAME_TAKEN" -> R.string.register_error_username_taken
    "EMAIL_TAKEN" -> R.string.register_error_email_taken
    "INVALID_NAME" -> R.string.register_error_name
    "INVALID_USERNAME" -> R.string.register_error_username
    "INVALID_EMAIL" -> R.string.register_error_email
    "WEAK_PASSWORD" -> R.string.register_error_password_short
    else -> R.string.register_error_generic
})
