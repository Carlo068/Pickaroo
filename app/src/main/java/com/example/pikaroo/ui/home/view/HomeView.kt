package com.example.pikaroo.ui.home.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.pikaroo.ui.products.model.Product
import com.example.pikaroo.ui.products.viewmodel.ProductsViewModel
import com.example.pikaroo.ui.theme.PikarooOrange
import com.example.pikaroo.ui.theme.PikarooTextGray
import java.text.Normalizer
import java.util.Locale

@Composable
fun HomeView(
    viewModel: ProductsViewModel = viewModel(),
    onOpenProducts: (String) -> Unit
) {
    val state = viewModel.state
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Inicio usa el catálogo completo, sin heredar el filtro de Productos.
    val query = searchQuery.trim()
    val visibleProducts = state.products.filter { product ->
        query.isEmpty() ||
                product.name.contains(query, ignoreCase = true) ||
                product.category.contains(query, ignoreCase = true)
    }

    val categories = state.products
        .map { it.category }
        .distinct()
        .sorted()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Barra superior fija
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Buscar productos...",
                        maxLines = 1
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = PikarooTextGray
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(32.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF8F8F8),
                    unfocusedContainerColor = Color(0xFFF8F8F8),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    // Pendiente: conectar con el carrito.
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingBag,
                    contentDescription = "Carrito",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        HorizontalDivider(color = Color(0xFFF0F0F0))

        // Todo el contenido inferior se desplaza junto.
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 20.dp,
                bottom = 24.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Portafolios",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(inicioPortafolios) { portfolio ->
                        HomePortfolioCard(portfolio)
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                HomeSectionHeader(
                    title = "Categorías",
                    action = "Ver todas",
                    onClick = { onOpenProducts("Todos") }
                )
            }

            // Tres categorías por fila, sin una cuadrícula vertical anidada.
            items(categories.chunked(3)) { categoryRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryRow.forEach { category ->
                        HomeCategoryCard(
                            category = category,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenProducts(category) }
                        )
                    }

                    repeat(3 - categoryRow.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                HomeSectionHeader(
                    title = "Productos",
                    action = "Ver todos",
                    onClick = { onOpenProducts("Todos") }
                )
            }

            when {
                state.isLoading -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = PikarooOrange
                            )
                        }
                    }
                }

                state.error != null -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "No se pudieron cargar los productos: ${state.error}",
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                visibleProducts.isEmpty() -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = if (query.isEmpty()) {
                                "No hay productos disponibles"
                            } else {
                                "No se encontraron productos"
                            },
                            modifier = Modifier.padding(vertical = 24.dp),
                            color = PikarooTextGray
                        )
                    }
                }

                else -> {
                    items(visibleProducts) { product ->
                        HomeProductCard(product = product)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    action: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )

        TextButton(onClick = onClick) {
            Text(
                text = action,
                color = PikarooOrange
            )
        }
    }
}

@Composable
private fun HomeCategoryCard(
    category: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE6E6E6))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 132.dp)
                .padding(horizontal = 6.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(
                        color = Color(0xFFFFF1E9),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = inicioCategoryEmoji(category),
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = category,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun inicioCategoryEmoji(category: String): String {
    val normalized = Normalizer
        .normalize(category, Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase(Locale.ROOT)

    return when {
        "fruta" in normalized -> "🍎"
        "verdura" in normalized || "vegetal" in normalized -> "🥬"
        "lacteo" in normalized || "leche" in normalized -> "🥛"
        "carne" in normalized -> "🥩"
        "pan" in normalized -> "🍞"
        "bebida" in normalized -> "☕"
        "limpieza" in normalized -> "🧼"
        "snack" in normalized || "botana" in normalized -> "🍿"
        "dulce" in normalized -> "🍬"
        "congelado" in normalized -> "🧊"
        "mascota" in normalized -> "🐾"
        else -> "🛍️"
    }
}

// Contenido de ejemplo: reemplaza estos textos por tus portafolios.
private data class HomePortfolio(
    val title: String,
    val description: String,
    val emoji: String,
    val startColor: Color,
    val endColor: Color
)

private val inicioPortafolios = listOf(
    HomePortfolio(
        title = "Compra semanal",
        description = "Ideas para llenar tu despensa",
        emoji = "🛒",
        startColor = Color(0xFFFF6200),
        endColor = Color(0xFFFF9800)
    ),
    HomePortfolio(
        title = "Frescos del día",
        description = "Color y variedad para tu mesa",
        emoji = "🥑",
        startColor = Color(0xFF159957),
        endColor = Color(0xFF41BB79)
    ),
    HomePortfolio(
        title = "Para compartir",
        description = "Ideas para cada ocasión",
        emoji = "🥐",
        startColor = Color(0xFF7953C3),
        endColor = Color(0xFFA47BE0)
    )
)

@Composable
private fun HomePortfolioCard(portfolio: HomePortfolio) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.width(280.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            portfolio.startColor,
                            portfolio.endColor
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Text(
                text = portfolio.emoji,
                fontSize = 36.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = portfolio.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 23.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = portfolio.description,
                color = Color.White,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun HomeProductCard(product: Product) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(270.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Text(
                    text = product.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = product.category,
                    fontSize = 12.sp,
                    color = PikarooTextGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${product.price}",
                        modifier = Modifier.weight(1f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    IconButton(
                        onClick = {
                            // Pendiente: agregar al carrito.
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(PikarooOrange, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Añadir ${product.name}",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}