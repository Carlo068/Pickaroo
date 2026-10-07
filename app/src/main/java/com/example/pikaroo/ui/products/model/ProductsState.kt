package com.example.pikaroo.ui.productos.model

import com.google.gson.annotations.SerializedName

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

data class ProductosState(
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val categories: List<String> = listOf("Todos"),
    val selectedCategory: String = "Todos",
    val isLoading: Boolean = false,
    val error: String? = null
)
