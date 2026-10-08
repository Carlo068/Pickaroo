package com.example.pikaroo.ui.cart

import com.example.pikaroo.ui.cart.data.CartStorage
import com.example.pikaroo.ui.cart.model.CartItem

class FakeCartStorage(initial: List<CartItem> = emptyList()) : CartStorage {
    var saved: List<CartItem> = initial
        private set
    var saveCount = 0
        private set

    override fun load(): List<CartItem> = saved

    override fun save(items: List<CartItem>) {
        saved = items
        saveCount++
    }
}
