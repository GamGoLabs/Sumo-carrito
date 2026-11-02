package com.sumocarrito.domain

import java.math.BigDecimal

/** Configuración fiscal. Todo editable: las tasas cambian por decreto. */
data class TaxConfig(
    val vatRate: BigDecimal = BigDecimal("0.16"),   // IVA general
    val igtfRate: BigDecimal = BigDecimal("0.03"),  // IGTF sobre pagos en divisa
    /** true = el precio del anaquel YA incluye IVA (caso común en tiendas). */
    val pricesIncludeVat: Boolean = false
)

/** Línea del carrito, independiente de Room (capa de dominio pura). */
data class CartLine(
    val id: Long,
    val name: String,
    val unitPrice: BigDecimal,   // USD
    val quantity: BigDecimal,    // admite kg: 0.750
    val vatExempt: Boolean
)

sealed interface PaymentSelection {
    /** Todo en efectivo USD. [tendered] = billete con el que pagas (para el vuelto). */
    data class CashUsd(val tendered: BigDecimal? = null) : PaymentSelection
    /** Pago Móvil o débito: todo en Bs, sin IGTF. */
    data object DigitalBs : PaymentSelection
    /** Mixto: [cashUsd] se paga en divisa (genera IGTF), el resto en Bs. */
    data class Mixed(val cashUsd: BigDecimal) : PaymentSelection
}

enum class BudgetStatus { NONE, GREEN, YELLOW, RED }

data class CartTotals(
    val exemptBase: BigDecimal,
    val taxableBase: BigDecimal,
    val vat: BigDecimal,
    val totalUsd: BigDecimal,        // base + IVA (lo que cuenta contra el presupuesto)
    val totalBs: BigDecimal,         // referencia: totalUsd * tasa
    val igtf: BigDecimal,            // en USD
    val payUsd: BigDecimal,          // USD a entregar en caja (incluye IGTF)
    val payBs: BigDecimal,           // Bs por Pago Móvil / débito
    val changeUsd: BigDecimal?,      // vuelto si pagas con billete mayor
    val budgetStatus: BudgetStatus,
    val budgetUsedPct: BigDecimal    // ej. 87.50
)
