package com.example.pikaroo.ui.products.model

import com.google.gson.annotations.SerializedName
import com.example.pikaroo.common.text.UiText

data class Product(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("price") val price: Double,
    // Intentamos mapear tanto imageUrl como image_url por si acaso
    @SerializedName("imageUrl", alternate = ["image_url", "image"]) val imageUrl: String,
    @SerializedName("category") val category: String
)

data class ProductResponse(
    @SerializedName("products") val products: List<Product>
)

/** Stands for "every category" in the filter; the screen shows it as text from resources. */
const val ALL_CATEGORIES = "__all__"

data class ProductsState(
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val categories: List<String> = listOf(ALL_CATEGORIES),
    val selectedCategory: String = ALL_CATEGORIES,
    val isLoading: Boolean = false,
    val error: UiText? = null
)
