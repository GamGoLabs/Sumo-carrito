package com.sumocarrito.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class BcvRateParserTest {
    private val html = """
        <div id="euro" class="col-sm-6"><span>EUR</span><strong> 42,41767538 </strong></div>
        <div id="dolar" class="col-sm-6 col-xs-6">
          <div class="row recuadrotsmc"><div class="col-sm-6 col-xs-6"><span> USD</span></div>
          <div class="col-sm-6 col-xs-6 centrado"><strong> 39,18130000 </strong></div></div></div>
        <div class="pull-right dinpro center">Fecha Valor: <span class="date-display-single" content="2024-10-23">Martes, 22 Octubre 2024</span></div>
    """.trimIndent()

    @Test fun parsesBlock() {
        val r = BcvRateParser.parse(html)!!
        assertEquals(BigDecimal("39.1813"), r.rate)
        assertEquals("Martes, 22 Octubre 2024", r.dateLabel)
    }
    @Test fun parsesPlainText() {
        val r = BcvRateParser.parse("EUR\n42,41767538\nUSD\n39,18130000\nFecha Valor: Martes, 22 Octubre 2024")!!
        assertEquals(BigDecimal("39.1813"), r.rate)
    }
    @Test fun thousandsSeparator() { assertEquals(BigDecimal("1234.5000"), BcvRateParser.parse("USD 1.234,5")!!.rate) }
    @Test fun garbageReturnsNull() { assertEquals(null, BcvRateParser.parse("<html>nada</html>")) }
    @Test fun plausibility() {
        assertEquals(true, BcvRateParser.plausible(BigDecimal("40.5"), BigDecimal("39.18")))
        assertEquals(false, BcvRateParser.plausible(BigDecimal("80"), BigDecimal("39.18")))
        assertEquals(false, BcvRateParser.plausible(BigDecimal("10"), BigDecimal("39.18")))
        assertEquals(true, BcvRateParser.plausible(BigDecimal("39"), BigDecimal("1.00")))   // sin referencia previa
        assertEquals(false, BcvRateParser.plausible(BigDecimal("0"), BigDecimal("39")))
    }
}
