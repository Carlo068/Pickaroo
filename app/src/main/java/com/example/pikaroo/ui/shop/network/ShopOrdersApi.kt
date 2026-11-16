package com.example.pikaroo.ui.shop.network

import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.shop.model.ShopStatusRequest
import retrofit2.http.*

interface ShopOrdersApi {
    @GET("shop/orders")
    suspend fun orders(@Header("Authorization") bearer: String): List<OrderDto>

    @PATCH("shop/orders/{id}/status")
    suspend fun update(@Header("Authorization") bearer: String, @Path("id") id: String, @Body request: ShopStatusRequest): OrderDto
}

class RemoteShopOrdersApi : ShopOrdersApi {
    override suspend fun orders(bearer: String): List<OrderDto> = throw UnsupportedOperationException("PickarooAPI todavía no está conectada")
    override suspend fun update(bearer: String, id: String, request: ShopStatusRequest): OrderDto = throw UnsupportedOperationException("PickarooAPI todavía no está conectada")
}
