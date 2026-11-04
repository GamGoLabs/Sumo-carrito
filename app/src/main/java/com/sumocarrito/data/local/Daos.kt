package com.sumocarrito.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CartItemDao {
    @Query("SELECT * FROM cart_items ORDER BY id DESC")
    fun observeAll(): Flow<List<CartItemEntity>>

    @Insert suspend fun insert(item: CartItemEntity): Long
    @Query("UPDATE cart_items SET vatExempt = :exempt WHERE id = :id")
    suspend fun setExempt(id: Long, exempt: Boolean)
    @Query("DELETE FROM cart_items WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM cart_items") suspend fun clear()
}

@Dao
interface PurchaseDao {
    @Insert suspend fun insert(p: PurchaseEntity): Long
    @Query("SELECT * FROM purchase_history ORDER BY epochMillis DESC")
    fun observeAll(): Flow<List<PurchaseEntity>>
}
