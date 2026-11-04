package com.sumocarrito.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class DolarApiParserTest {
    @Test fun parsesPromedio() {
        val r = DolarApiParser.parse("""{"fuente":"oficial","nombre":"Oficial","compra":null,"venta":null,"promedio":301.37,"fechaActualizacion":"2026-01-01T21:02:47.165Z"}""")!!
        assertEquals(BigDecimal("301.3700"), r.rate)
        assertEquals("01/01/2026", r.dateLabel)
        assertEquals("DolarAPI", r.source)
    }
    @Test fun fallsBackToVenta() { assertEquals(BigDecimal("510.7873"), DolarApiParser.parse("""{"compra":null,"venta":510.7873,"promedio":null}""")!!.rate) }
    @Test fun integerValue() { assertEquals(BigDecimal("400.0000"), DolarApiParser.parse("""{"promedio":400}""")!!.rate) }
    @Test fun invalidReturnsNull() {
        assertEquals(null, DolarApiParser.parse("""{"promedio":null,"venta":null}"""))
        assertEquals(null, DolarApiParser.parse("<html>error</html>"))
        assertEquals(null, DolarApiParser.parse("""{"promedio":0}"""))
    }
}
