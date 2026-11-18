package com.example.pikaroo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.pikaroo.ui.auth.data.SessionRepository
import com.example.pikaroo.ui.auth.model.RoleMode

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

/** Muestra [content] solo para una sesión cuyo rol admite [mode]. */
@Composable
fun RequireRoleMode(
    session: SessionRepository,
    mode: RoleMode,
    onDenied: () -> Unit,
    content: @Composable () -> Unit
) {
    if (session.isLoggedIn() && mode in session.availableModes()) {
        content()
    } else {
        LaunchedEffect(Unit) { onDenied() }
    }
}
