package com.example.pikaroo.ui.orders.network

import com.example.pikaroo.ui.orders.model.DriverLocationDto
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.orders.model.OrderEventDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

/** Pedidos del cliente en PickarooAPI (docs/PLAN.md §5). */
interface OrdersApi {

    @GET("orders")
    suspend fun listMine(@Header("Authorization") bearer: String): List<OrderDto>

    @GET("orders/{id}")
    suspend fun get(@Header("Authorization") bearer: String, @Path("id") id: String): OrderDto

    @GET("orders/{id}/driver-location")
    suspend fun driverLocation(@Header("Authorization") bearer: String, @Path("id") id: String): DriverLocationDto

    @GET("orders/{id}/events")
    suspend fun events(@Header("Authorization") bearer: String, @Path("id") id: String): List<OrderEventDto>
}

class RemoteOrdersApi : OrdersApi {
    private fun notConnected(): Nothing =
        throw UnsupportedOperationException("PickarooAPI todavía no está conectada")

    override suspend fun listMine(bearer: String) = notConnected()
    override suspend fun get(bearer: String, id: String) = notConnected()
    override suspend fun events(bearer: String, id: String) = notConnected()
    override suspend fun driverLocation(bearer: String, id: String) = notConnected()
}
