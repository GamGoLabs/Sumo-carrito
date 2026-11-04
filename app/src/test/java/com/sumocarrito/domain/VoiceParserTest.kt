package com.sumocarrito.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class VoiceParserTest {
    private fun d(s: String) = BigDecimal(s)
    @Test fun digitsWithCon() { val r = VoiceParser.parse("leche 2 con 50"); assertEquals("Leche", r.name); assertEquals(d("2.50"), r.price); assertEquals(null, r.quantity) }
    @Test fun wordsWithCon() { val r = VoiceParser.parse("arroz dos con cincuenta"); assertEquals("Arroz", r.name); assertEquals(d("2.50"), r.price) }
    @Test fun adjacentWords() { assertEquals(d("3.50"), VoiceParser.parse("harina tres cincuenta").price) }
    @Test fun quantityAndPrice() { val r = VoiceParser.parse("3 leche a 2.50"); assertEquals(d("3"), r.quantity); assertEquals(d("2.50"), r.price); assertEquals("Leche", r.name) }
    @Test fun compoundWord() { assertEquals(d("45"), VoiceParser.parse("queso cuarenta y cinco").price) }
    @Test fun commaDecimalAndAccents() { val r = VoiceParser.parse("Pan 1,25 dólares"); assertEquals(d("1.25"), r.price); assertEquals("Pan", r.name) }
    @Test fun veinticinco() { assertEquals(d("25"), VoiceParser.parse("jabón veinticinco").price) }
    @Test fun literalCents() { assertEquals(d("1.05"), VoiceParser.parse("refresco uno con cinco").price) }
    @Test fun wordQtyAndPrice() { val r = VoiceParser.parse("dos arroz uno con veinticinco"); assertEquals(d("2"), r.quantity); assertEquals(d("1.25"), r.price) }
    @Test fun noPrice() { assertEquals(null, VoiceParser.parse("hola").price) }
    @Test fun nameWithCon() { val r = VoiceParser.parse("pan con queso 3"); assertEquals("Pan con queso", r.name); assertEquals(d("3"), r.price) }
}
