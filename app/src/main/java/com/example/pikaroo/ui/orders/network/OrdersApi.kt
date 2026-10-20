package com.example.pikaroo.ui.orders.network

import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.orders.model.OrderEventDto
import com.example.pikaroo.ui.orders.model.CreateOrderRequest
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

/** Pedidos del cliente en PickarooAPI (docs/PLAN.md §5). */
interface OrdersApi {
    @POST("orders")
    suspend fun create(@Header("Authorization") bearer: String, @Body request: CreateOrderRequest): OrderDto


    @GET("orders")
    suspend fun listMine(@Header("Authorization") bearer: String): List<OrderDto>

    @GET("orders/{id}")
    suspend fun get(@Header("Authorization") bearer: String, @Path("id") id: String): OrderDto

    @GET("orders/{id}/events")
    suspend fun events(@Header("Authorization") bearer: String, @Path("id") id: String): List<OrderEventDto>
}

class RemoteOrdersApi : OrdersApi {
    private fun notConnected(): Nothing =
        throw UnsupportedOperationException("PickarooAPI todavía no está conectada")

    override suspend fun create(bearer: String, request: CreateOrderRequest): OrderDto = notConnected()

    override suspend fun listMine(bearer: String) = notConnected()
    override suspend fun get(bearer: String, id: String) = notConnected()
    override suspend fun events(bearer: String, id: String) = notConnected()
}
