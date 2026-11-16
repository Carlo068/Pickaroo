package com.example.pikaroo.common.text

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * Text that is shown to the user but decided outside a composable (ViewModels, state classes,
 * mappers). It holds a string resource id (plus arguments) instead of the translated text, so the
 * logic stays testable on the JVM and the screen resolves it in the current language.
 *
 * Arguments of [Res] and [Plural] may themselves be [UiText]; they are resolved first.
 */
sealed interface UiText {

    /** A string resource, with optional format arguments. */
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText {
        constructor(@StringRes id: Int, vararg args: Any) : this(id, args.toList())
    }

    /** A plurals resource; [count] picks the quantity and is also the first format argument. */
    data class Plural(@PluralsRes val id: Int, val count: Int, val args: List<Any> = listOf(count)) : UiText

    /** Text that is data, not copy (for example a message that came from the server or an exception). */
    data class Raw(val value: String) : UiText

    fun asString(context: Context): String = when (this) {
        is Raw -> value
        is Res -> context.getString(id, *resolveArgs(args, context))
        is Plural -> context.resources.getQuantityString(id, count, *resolveArgs(args, context))
    }
}

private fun resolveArgs(args: List<Any>, context: Context): Array<Any> =
    Array(args.size) { index -> (args[index] as? UiText)?.asString(context) ?: args[index] }

/** Resolves the text in the language of the current composition. */
@Composable
fun UiText.asString(): String {
    LocalConfiguration.current // re-resolve when the language changes
    return asString(LocalContext.current)
}
