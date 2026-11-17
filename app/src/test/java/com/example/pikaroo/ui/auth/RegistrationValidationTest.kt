package com.example.pikaroo.ui.auth

import com.example.pikaroo.R
import com.example.pikaroo.common.text.UiText
import com.example.pikaroo.ui.auth.model.RegisterState
import com.example.pikaroo.ui.auth.model.validationError
import com.example.pikaroo.ui.auth.model.registrationErrorMessage
import org.junit.Assert.*
import org.junit.Test

class RegistrationValidationTest {
    private val valid = RegisterState(username = "ana", name = "Ana Pérez", email = "ana@example.test", password = "secret1", confirmPassword = "secret1")

    @Test fun `every missing field blocks submission`() {
        for (state in listOf(valid.copy(name = " "), valid.copy(username = ""), valid.copy(email = ""), valid.copy(password = ""), valid.copy(confirmPassword = ""))) {
            assertEquals(UiText.Res(R.string.register_error_required), state.validationError())
        }
    }
    @Test fun `username bounds and internal whitespace are rejected`() {
        for (username in listOf("an", "a".repeat(65), "a na", "a\tna")) {
            assertEquals(UiText.Res(R.string.register_error_username), valid.copy(username = username).validationError())
        }
    }
    @Test fun `email requires local part domain and dot`() {
        for (email in listOf("bad", "a@", "@example.test", "a@b", "a b@example.test")) {
            assertEquals(UiText.Res(R.string.register_error_email), valid.copy(email = email).validationError())
        }
    }
    @Test fun `short and mismatched passwords have distinct messages`() {
        assertEquals(UiText.Res(R.string.register_error_password_short), valid.copy(password = "12345", confirmPassword = "12345").validationError())
        assertEquals(UiText.Res(R.string.register_error_password_mismatch), valid.copy(confirmPassword = "different").validationError())
    }
    @Test fun `valid fields permit surrounding spaces without trimming passwords`() {
        assertNull(valid.copy(username = " ana ", name = " Ana ", email = " ana@example.test ").validationError())
        assertEquals(UiText.Res(R.string.register_error_password_mismatch), valid.copy(password = " secret1 ").validationError())
    }
    @Test fun `server duplicate errors are explained separately`() {
        assertEquals(UiText.Res(R.string.register_error_username_taken), registrationErrorMessage("USERNAME_TAKEN"))
        assertEquals(UiText.Res(R.string.register_error_email_taken), registrationErrorMessage("EMAIL_TAKEN"))
        assertEquals(UiText.Res(R.string.register_error_generic), registrationErrorMessage(null))
    }
}
