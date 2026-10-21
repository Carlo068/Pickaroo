package com.example.pikaroo.ui.driver.network

import com.example.pikaroo.ui.driver.model.AvailableOrderDto
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.driver.model.DriverLocationRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/** Endpoints del repartidor en PickarooAPI (docs/PLAN.md §5). */
interface DriverApi {

    @GET("driver/orders/available")
    suspend fun available(@Header("Authorization") bearer: String): List<AvailableOrderDto>

    /** La entrega que el repartidor lleva ahora; 404 NO_ACTIVE_DELIVERY si no tiene ninguna. */
    @GET("driver/orders/current")
    suspend fun current(@Header("Authorization") bearer: String): OrderDto

    /** 409 ORDER_NOT_AVAILABLE si otro repartidor lo tomó primero; 409 ALREADY_DELIVERING si ya lleva una entrega. */
    @POST("driver/orders/{id}/accept")
    suspend fun accept(@Header("Authorization") bearer: String, @Path("id") id: String): OrderDto

    /** DRIVER_ASSIGNED -> OUT_FOR_DELIVERY. 403 NOT_YOUR_DELIVERY, 409 INVALID_STATUS. */
    @POST("driver/orders/{id}/pickup")
    suspend fun pickup(@Header("Authorization") bearer: String, @Path("id") id: String): OrderDto

    /** OUT_FOR_DELIVERY -> DELIVERED. 403 NOT_YOUR_DELIVERY, 409 INVALID_STATUS. */
    @POST("driver/orders/{id}/deliver")
    suspend fun deliver(@Header("Authorization") bearer: String, @Path("id") id: String): OrderDto

    /**
     * Última posición del repartidor para su entrega en curso (el cliente la lee en
     * GET orders/{id}/driver-location). 400 INVALID_LOCATION, 403 NOT_YOUR_DELIVERY, 409 INVALID_STATUS.
     */
    @POST("driver/location")
    suspend fun reportLocation(@Header("Authorization") bearer: String, @Body body: DriverLocationRequest)
}

class RemoteDriverApi : DriverApi {
    private fun notConnected(): Nothing =
        throw UnsupportedOperationException("PickarooAPI todavía no está conectada")

    override suspend fun available(bearer: String) = notConnected()
    override suspend fun current(bearer: String) = notConnected()
    override suspend fun accept(bearer: String, id: String) = notConnected()
    override suspend fun pickup(bearer: String, id: String) = notConnected()
    override suspend fun deliver(bearer: String, id: String) = notConnected()
    override suspend fun reportLocation(bearer: String, body: DriverLocationRequest) = notConnected()
}
