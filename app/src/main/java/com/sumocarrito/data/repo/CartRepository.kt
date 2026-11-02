package com.sumocarrito.data.repo

import com.sumocarrito.data.local.CartItemDao
import com.sumocarrito.data.local.CartItemEntity
import com.sumocarrito.domain.CartLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject

class CartRepository @Inject constructor(private val dao: CartItemDao) {
    fun observeLines(): Flow<List<CartLine>> = dao.observeAll().map { list ->
        list.map { CartLine(it.id, it.name, it.unitPrice, it.quantity, it.vatExempt) }
    }
    suspend fun add(name: String, price: BigDecimal, qty: BigDecimal, exempt: Boolean) =
        dao.insert(CartItemEntity(name = name.ifBlank { "Producto" }, unitPrice = price, quantity = qty, vatExempt = exempt))
    suspend fun setExempt(id: Long, exempt: Boolean) = dao.setExempt(id, exempt)
    suspend fun remove(id: Long) = dao.delete(id)
    suspend fun clear() = dao.clear()
}
