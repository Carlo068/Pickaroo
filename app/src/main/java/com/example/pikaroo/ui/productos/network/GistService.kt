package com.example.pikaroo.ui.productos.network

import com.example.pikaroo.ui.productos.model.ProductResponse
import retrofit2.http.GET

interface GistService {
    @GET("JSalim07/2ee45b58670fa2aa510b8e1c9e668759/raw/e4e534b05dc58c8f1ff30ebec1ee47db070b7b09/pickaroo-products.json")
    suspend fun getProducts(): ProductResponse
}
