package com.example.pikaroo.ui.cart.data

import android.content.Context
import com.example.pikaroo.common.preferences.AppPreferences
import com.example.pikaroo.ui.auth.data.SessionPreferences
import com.example.pikaroo.ui.cart.model.CartItem
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken

interface CartStorage {
    fun load(): List<CartItem>
    fun save(items: List<CartItem>)
}

object CartJson {
    private val gson = Gson()

    // Gson no respeta la nulabilidad de Kotlin: se lee a un tipo con campos
    // opcionales y se descartan los elementos incompletos.
    private data class StoredCartItem(
        val productId: String?,
        val name: String?,
        val price: Double?,
        val imageUrl: String?,
        val quantity: Int?
    )

    private val listType = object : TypeToken<List<StoredCartItem?>>() {}.type

    fun encode(items: List<CartItem>): String = gson.toJson(items)

    fun decode(json: String): List<CartItem> {
        if (json.isBlank()) return emptyList()
        val stored: List<StoredCartItem?> = try {
            gson.fromJson(json, listType) ?: emptyList()
        } catch (_: JsonParseException) {
            return emptyList()
        }
        return stored.mapNotNull { item ->
            val id = item?.productId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val name = item.name ?: return@mapNotNull null
            val price = item.price ?: return@mapNotNull null
            val quantity = item.quantity?.takeIf { it > 0 } ?: return@mapNotNull null
            CartItem(id, name, price, item.imageUrl.orEmpty(), quantity)
        }
    }
}

/**
 * Carrito persistido en SharedPreferences (vía [AppPreferences]), una clave por
 * usuario para que los carritos no se mezclen entre cuentas.
 */
class CartPreferences(
    context: Context,
    username: String = SessionPreferences(context).username()
) : CartStorage {

    private val preferences = AppPreferences(context)
    private val key = keyFor(username)

    override fun load(): List<CartItem> = CartJson.decode(preferences.getString(key))

    override fun save(items: List<CartItem>) {
        preferences.putString(key, CartJson.encode(items))
    }

    companion object {
        fun keyFor(username: String) = "cart_$username"
    }
}
