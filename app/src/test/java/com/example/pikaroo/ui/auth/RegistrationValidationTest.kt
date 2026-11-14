package com.example.pikaroo.ui.auth

import com.example.pikaroo.ui.auth.model.RegisterState
import com.example.pikaroo.ui.auth.model.validationError
import com.example.pikaroo.ui.auth.model.registrationErrorMessage
import org.junit.Assert.*
import org.junit.Test

class RegistrationValidationTest {
    private val valid = RegisterState(username = "ana", name = "Ana Pérez", email = "ana@example.test", password = "secret1", confirmPassword = "secret1")

    @Test fun `every missing field blocks submission`() {
        for (state in listOf(valid.copy(name = " "), valid.copy(username = ""), valid.copy(email = ""), valid.copy(password = ""), valid.copy(confirmPassword = ""))) {
            assertEquals("Completa todos los campos", state.validationError())
        }
    }
    @Test fun `username bounds and internal whitespace are rejected`() {
        for (username in listOf("an", "a".repeat(65), "a na", "a\tna")) {
            assertEquals("El usuario debe tener entre 3 y 64 caracteres, sin espacios", valid.copy(username = username).validationError())
        }
    }
    @Test fun `email requires local part domain and dot`() {
        for (email in listOf("bad", "a@", "@example.test", "a@b", "a b@example.test")) {
            assertEquals("Ingresa un correo electrónico válido", valid.copy(email = email).validationError())
        }
    }
    @Test fun `short and mismatched passwords have distinct messages`() {
        assertEquals("La contraseña debe tener al menos 6 caracteres", valid.copy(password = "12345", confirmPassword = "12345").validationError())
        assertEquals("Las contraseñas no coinciden", valid.copy(confirmPassword = "different").validationError())
    }
    @Test fun `valid fields permit surrounding spaces without trimming passwords`() {
        assertNull(valid.copy(username = " ana ", name = " Ana ", email = " ana@example.test ").validationError())
        assertEquals("Las contraseñas no coinciden", valid.copy(password = " secret1 ").validationError())
    }
    @Test fun `server duplicate errors are explained separately`() {
        assertEquals("El nombre de usuario ya está en uso", registrationErrorMessage("USERNAME_TAKEN"))
        assertEquals("El correo electrónico ya está en uso", registrationErrorMessage("EMAIL_TAKEN"))
        assertEquals("No se pudo crear la cuenta. Intenta de nuevo.", registrationErrorMessage(null))
    }
}
