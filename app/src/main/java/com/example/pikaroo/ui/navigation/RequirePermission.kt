package com.example.pikaroo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.pikaroo.ui.auth.data.SessionRepository

/** Muestra [content] solo si la sesión tiene [permission]; si no, avisa con [onDenied] y no dibuja nada. */
@Composable
fun RequirePermission(
    session: SessionRepository,
    permission: String,
    onDenied: () -> Unit,
    content: @Composable () -> Unit
) {
    if (session.can(permission)) {
        content()
    } else {
        LaunchedEffect(Unit) { onDenied() }
    }
}
