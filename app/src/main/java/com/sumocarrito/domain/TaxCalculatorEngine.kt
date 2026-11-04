package com.sumocarrito.domain

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Motor de cálculo. Reglas de redondeo (para coincidir con la factura):
 *  - Cada línea se redondea a 2 decimales (HALF_UP).
 *  - IVA = base gravada acumulada * tasa, redondeado una sola vez.
 *  - IGTF = monto en divisa * tasa, redondeado una sola vez.
 * Nunca se usa Double/Float.
 */
class TaxCalculatorEngine @Inject constructor() {

    private val money = 2
    private val rm = RoundingMode.HALF_UP
    private val hundred = BigDecimal(100)

    fun calculate(
        lines: List<CartLine>,
        config: TaxConfig,
        exchangeRate: BigDecimal,   // Bs por 1 USD
        payment: PaymentSelection,
        budget: BigDecimal
    ): CartTotals {
        var exempt = BigDecimal.ZERO
        var taxable = BigDecimal.ZERO

        lines.forEach { l ->
            val gross = l.unitPrice.multiply(l.quantity)
            if (l.vatExempt) {
                exempt = exempt.add(gross.setScale(money, rm))
            } else {
                val base = if (config.pricesIncludeVat)
                    gross.divide(BigDecimal.ONE.add(config.vatRate), 10, rm) else gross
                taxable = taxable.add(base.setScale(money, rm))
            }
        }

        val vat = taxable.multiply(config.vatRate).setScale(money, rm)
        val total = exempt.add(taxable).add(vat)
        val rate = exchangeRate.max(BigDecimal.ZERO)

        var igtf = BigDecimal.ZERO.setScale(money)
        var payUsd = BigDecimal.ZERO.setScale(money)
        var payBs = BigDecimal.ZERO.setScale(money)
        var change: BigDecimal? = null

        when (payment) {
            is PaymentSelection.CashUsd -> {
                igtf = total.multiply(config.igtfRate).setScale(money, rm)
                payUsd = total.add(igtf)
                payment.tendered?.let {
                    change = it.subtract(payUsd).max(BigDecimal.ZERO).setScale(money, rm)
                }
            }
            PaymentSelection.DigitalBs -> {
                payBs = total.multiply(rate).setScale(money, rm)
            }
            is PaymentSelection.Mixed -> {
                val cash = payment.cashUsd.max(BigDecimal.ZERO).min(total).setScale(money, rm)
                igtf = cash.multiply(config.igtfRate).setScale(money, rm)
                payUsd = cash.add(igtf)
                payBs = total.subtract(cash).multiply(rate).setScale(money, rm)
            }
        }

        val (status, pct) = budgetState(total, budget)
        return CartTotals(
            exemptBase = exempt, taxableBase = taxable, vat = vat, totalUsd = total,
            totalBs = total.multiply(rate).setScale(money, rm),
            igtf = igtf, payUsd = payUsd, payBs = payBs, changeUsd = change,
            budgetStatus = status, budgetUsedPct = pct
        )
    }

    /** Semáforo: verde <85%, amarillo 85–99.99%, rojo >=100%. */
    fun budgetState(spent: BigDecimal, budget: BigDecimal): Pair<BudgetStatus, BigDecimal> {
        if (budget.signum() <= 0) return BudgetStatus.NONE to BigDecimal.ZERO.setScale(2)
        val pct = spent.multiply(hundred).divide(budget, 2, rm)
        val status = when {
            spent >= budget -> BudgetStatus.RED
            pct >= BigDecimal("85") -> BudgetStatus.YELLOW
            else -> BudgetStatus.GREEN
        }
        return status to pct
    }
}
