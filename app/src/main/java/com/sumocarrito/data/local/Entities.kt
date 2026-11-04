package com.sumocarrito.data.local

import androidx.room.*
import java.math.BigDecimal

/** BigDecimal como String: Room no lo soporta y Double corrompe decimales. */
class BigDecimalConverter {
    @TypeConverter fun fromString(v: String?): BigDecimal? = v?.toBigDecimalOrNull()
    @TypeConverter fun toString(v: BigDecimal?): String? = v?.toPlainString()
}

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unitPrice: BigDecimal,
    val quantity: BigDecimal,
    val vatExempt: Boolean
)

/** Se usa en la entrega 5 (Pro): historial por fecha y establecimiento. */
@Entity(tableName = "purchase_history")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochMillis: Long,
    val store: String,
    val totalUsd: BigDecimal,
    val exchangeRate: BigDecimal,
    val itemsJson: String
)
