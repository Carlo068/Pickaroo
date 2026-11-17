package com.example.pikaroo.ui.user.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.semantics.Role
import com.example.pikaroo.common.locale.AppLanguage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pikaroo.ui.auth.model.RoleMode
import com.example.pikaroo.ui.auth.view.ModeSwitchButtons
import com.example.pikaroo.ui.cart.view.CartIconButton
import com.example.pikaroo.ui.theme.PikarooOrange
import com.example.pikaroo.ui.theme.PikarooTheme
import com.example.pikaroo.ui.user.viewmodel.UserViewModel
import com.example.pikaroo.ui.user.model.UserState
import com.example.pikaroo.ui.auth.model.MeResponse
import com.example.pikaroo.ui.auth.model.UserDto
import com.example.pikaroo.ui.auth.model.RoleAssignment
import androidx.compose.ui.res.stringResource
import com.example.pikaroo.R
import androidx.compose.ui.res.pluralStringResource
import androidx.annotation.StringRes
import com.example.pikaroo.common.text.asString

object UserTestTags {
    const val LANGUAGE_ROW = "user_language"
    const val LANGUAGE_DIALOG = "language_dialog"
    fun languageOption(language: AppLanguage) = "language_option_${language.name}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserView(
    onLogout: () -> Unit = {},
    otherModes: List<RoleMode> = emptyList(),
    onSwitchMode: (RoleMode) -> Unit = {},
    onOpenOrders: () -> Unit = {},
    cartItemCount: Int = 0,
    onOpenCart: () -> Unit = {},
    viewModel: UserViewModel = viewModel(factory = UserViewModel.Factory)
) {
    LaunchedEffect(Unit) { viewModel.refreshLanguage() }
    UserContent(
        state = viewModel.state,
        onRetry = viewModel::refresh,
        onOpenLanguageDialog = viewModel::openLanguageDialog,
        onSelectLanguage = viewModel::selectLanguage,
        onDismissLanguageDialog = viewModel::dismissLanguageDialog,
        onLogout = onLogout,
        otherModes = otherModes,
        onSwitchMode = onSwitchMode,
        onOpenOrders = onOpenOrders,
        cartItemCount = cartItemCount,
        onOpenCart = onOpenCart
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserContent(
    state: UserState,
    onRetry: () -> Unit = {},
    onOpenLanguageDialog: () -> Unit = {},
    onSelectLanguage: (AppLanguage) -> Unit = {},
    onDismissLanguageDialog: () -> Unit = {},
    onLogout: () -> Unit = {},
    otherModes: List<RoleMode> = emptyList(),
    onSwitchMode: (RoleMode) -> Unit = {},
    onOpenOrders: () -> Unit = {},
    cartItemCount: Int = 0,
    onOpenCart: () -> Unit = {}
) {
    if (state.showLanguageDialog) {
        LanguageDialog(
            selected = state.language,
            onSelect = onSelectLanguage,
            onDismiss = onDismissLanguageDialog
        )
    }
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    placeholder = { Text(stringResource(R.string.user_search_lists_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                CartIconButton(itemCount = cartItemCount, onClick = onOpenCart)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Profile Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = Color.LightGray
                    ) {
                        // Placeholder for image
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.BottomEnd),
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.padding(4.dp),
                            tint = Color.Gray
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    val profile = state.profile
                    if (profile != null) {
                        Text(profile.user.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("profile_name"))
                        Text(profile.user.email, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.testTag("profile_email"))
                        Text(
                            profile.roles.map { assignment ->
                                roleLabelRes(assignment.role)?.let { stringResource(it) } ?: assignment.role
                            }.distinct().joinToString(" · "),
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.testTag("profile_roles")
                        )
                    } else {
                        Text(stringResource(if (state.isLoading) R.string.user_profile_loading else R.string.user_profile_placeholder), fontSize = 20.sp)
                    }
                }
            }
            if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("profile_loading"))
            state.error?.let {
                Text(it.asString(), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("profile_error"))
                TextButton(onClick = onRetry, enabled = !state.isLoading, modifier = Modifier.testTag("profile_retry")) {
                    Text(stringResource(R.string.user_profile_retry))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Cards
            ProfileActionCard(
                icon = Icons.Default.Receipt,
                iconTint = Color(0xFFF15A24),
                title = stringResource(R.string.user_orders_title),
                subtitle = stringResource(R.string.user_orders_subtitle),
                onClick = onOpenOrders,
                modifier = Modifier.testTag("user_orders")
            )
            Spacer(modifier = Modifier.height(8.dp))
            ProfileActionCard(
                icon = Icons.Default.Person,
                iconTint = Color(0xFFF15A24),
                title = stringResource(R.string.user_edit_profile_title),
                subtitle = stringResource(R.string.user_edit_profile_subtitle)
            )
            Spacer(modifier = Modifier.height(8.dp))
            ProfileActionCard(
                icon = Icons.Default.Lock,
                iconTint = Color(0xFFF15A24),
                title = stringResource(R.string.user_change_password_title),
                subtitle = stringResource(R.string.user_change_password_subtitle)
            )
            Spacer(modifier = Modifier.height(8.dp))
            ProfileActionCard(
                icon = Icons.Default.Email,
                iconTint = Color(0xFFF15A24),
                title = stringResource(R.string.user_change_email_title),
                subtitle = stringResource(R.string.user_change_email_subtitle)
            )

            Spacer(modifier = Modifier.height(8.dp))
            ProfileActionCard(
                icon = Icons.Default.Language,
                iconTint = Color(0xFFF15A24),
                title = stringResource(R.string.user_language_title),
                subtitle = stringResource(state.language.labelRes()),
                onClick = onOpenLanguageDialog,
                modifier = Modifier.testTag(UserTestTags.LANGUAGE_ROW)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = Color(0xFFF15A24)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.user_lists_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = stringResource(R.string.user_lists_subtitle),
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 32.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Shopping List Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.user_list_weekly_name),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = pluralStringResource(R.plurals.user_list_products_count, 3, 3),
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE57373))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    ShoppingListItem(stringResource(R.string.user_list_item_apples), stringResource(R.string.user_list_category_fruit), "$2.99")
                    ShoppingListItem(stringResource(R.string.user_list_item_bread), stringResource(R.string.user_list_category_bakery), "$3.50")
                    ShoppingListItem(stringResource(R.string.user_list_item_milk), stringResource(R.string.user_list_category_dairy), "$1.99")

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = stringResource(R.string.user_list_total), fontWeight = FontWeight.Medium)
                        Text(text = "$8.48", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.user_list_add_all))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (otherModes.isNotEmpty()) {
                ModeSwitchButtons(modes = otherModes, onSwitch = onSwitchMode)
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF15A24)),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.common_logout), fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/** The role names shown under the profile; null for a role the app does not know (shown as sent). */
@StringRes
private fun roleLabelRes(role: String): Int? = when (role) {
    "customer" -> R.string.user_role_customer
    "owner" -> R.string.user_role_owner
    "staff" -> R.string.user_role_staff
    "driver" -> R.string.user_role_driver
    else -> null
}

private fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.SYSTEM -> R.string.user_language_system
    AppLanguage.SPANISH -> R.string.user_language_spanish
    AppLanguage.ENGLISH -> R.string.user_language_english
}

@Composable
private fun LanguageDialog(selected: AppLanguage, onSelect: (AppLanguage) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(UserTestTags.LANGUAGE_DIALOG),
        title = { Text(stringResource(R.string.user_language_title)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = language == selected, role = Role.RadioButton, onClick = { onSelect(language) })
                            .padding(vertical = 4.dp)
                            .testTag(UserTestTags.languageOption(language)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = language == selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = PikarooOrange)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(language.labelRes()))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = PikarooOrange)) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
fun ProfileActionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Text(text = subtitle, color = Color.Gray, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun ShoppingListItem(name: String, category: String, price: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF5F5F5)
        ) {
            // Placeholder for product image
            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.padding(12.dp), tint = Color.LightGray)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(text = category, color = Color.Gray, fontSize = 12.sp)
        }
        Text(text = price, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun UserViewPreview() {
    PikarooTheme {
        UserContent(UserState(profile = MeResponse(
            UserDto("preview", "cliente", "Cliente Demo", "cliente@pickaroo.test"),
            listOf(RoleAssignment("customer")), emptyList()
        )))
    }
}
