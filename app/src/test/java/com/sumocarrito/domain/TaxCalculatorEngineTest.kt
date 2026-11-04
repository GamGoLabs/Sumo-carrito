package com.sumocarrito.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class TaxCalculatorEngineTest {
    private val engine = TaxCalculatorEngine()
    private val cfg = TaxConfig()
    private val rate = BigDecimal("40.00")
    private fun d(s: String) = BigDecimal(s)

    private val lines = listOf(
        CartLine(1, "Leche", d("1.50"), d("2"), true),      // exento 3.00
        CartLine(2, "Jabón", d("2.00"), d("1"), false)      // gravado 2.00 -> IVA 0.32
    )

    @Test fun cashUsd_appliesIgtf() {
        val t = engine.calculate(lines, cfg, rate, PaymentSelection.CashUsd(d("10")), d("0"))
        assertEquals(d("5.32"), t.totalUsd)
        assertEquals(d("0.16"), t.igtf)
        assertEquals(d("5.48"), t.payUsd)
        assertEquals(d("4.52"), t.changeUsd)
        assertEquals(d("212.80"), t.totalBs)
    }

    @Test fun digital_noIgtf() {
        val t = engine.calculate(lines, cfg, rate, PaymentSelection.DigitalBs, d("0"))
        assertEquals(0, t.igtf.compareTo(BigDecimal.ZERO))
        assertEquals(d("212.80"), t.payBs)
    }

    @Test fun mixed_igtfOnlyOnCashPortion() {
        val t = engine.calculate(lines, cfg, rate, PaymentSelection.Mixed(d("2.00")), d("0"))
        assertEquals(d("0.06"), t.igtf)
        assertEquals(d("2.06"), t.payUsd)
        assertEquals(d("132.80"), t.payBs)   // (5.32-2.00)*40
    }

    @Test fun pricesIncludeVat_extractsBase() {
        val c = cfg.copy(pricesIncludeVat = true)
        val t = engine.calculate(listOf(CartLine(1, "X", d("11.60"), d("1"), false)), c, rate, PaymentSelection.DigitalBs, d("0"))
        assertEquals(d("10.00"), t.taxableBase)
        assertEquals(d("1.60"), t.vat)
        assertEquals(d("11.60"), t.totalUsd)
    }

    @Test fun budgetTrafficLight() {
        assertEquals(BudgetStatus.GREEN, engine.budgetState(d("16.99"), d("20")).first)
        assertEquals(BudgetStatus.YELLOW, engine.budgetState(d("17.00"), d("20")).first)
        assertEquals(BudgetStatus.YELLOW, engine.budgetState(d("19.99"), d("20")).first)
        assertEquals(BudgetStatus.RED, engine.budgetState(d("20.00"), d("20")).first)
        assertEquals(BudgetStatus.NONE, engine.budgetState(d("5"), d("0")).first)
    }

    @Test fun sanitizer() {
        assertEquals(d("1.50"), InputSanitizer.parseDecimal("1,50"))
        assertEquals(d("1234.56"), InputSanitizer.parseDecimal("1.234,56"))
        assertEquals(d("1234.56"), InputSanitizer.parseDecimal("1,234.56"))
    }
}
