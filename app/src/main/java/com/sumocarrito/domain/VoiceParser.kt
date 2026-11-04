package com.sumocarrito.domain

import java.math.BigDecimal
import java.text.Normalizer

data class VoiceItem(val name: String, val price: BigDecimal?, val quantity: BigDecimal?)

/**
 * Convierte lo dictado en español a producto/precio/cantidad. Ejemplos:
 *  "leche 2 con 50" -> Leche, 2.50          "arroz dos con cincuenta" -> 2.50
 *  "harina tres cincuenta" -> 3.50          "3 leche a 2.50" -> cantidad 3, 2.50
 *  "queso cuarenta y cinco" -> 45           "refresco uno con cinco" -> 1.05 (centavos literales)
 * El resultado SIEMPRE se muestra para confirmar antes de agregarlo al carrito.
 */
object VoiceParser {
    private val units = mapOf("cero" to 0, "un" to 1, "uno" to 1, "una" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4,
        "cinco" to 5, "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10, "once" to 11, "doce" to 12,
        "trece" to 13, "catorce" to 14, "quince" to 15, "dieciseis" to 16, "diecisiete" to 17, "dieciocho" to 18,
        "diecinueve" to 19, "veinte" to 20, "veintiun" to 21, "veintiuno" to 21, "veintidos" to 22, "veintitres" to 23,
        "veinticuatro" to 24, "veinticinco" to 25, "veintiseis" to 26, "veintisiete" to 27, "veintiocho" to 28, "veintinueve" to 29)
    private val tens = mapOf("treinta" to 30, "cuarenta" to 40, "cincuenta" to 50, "sesenta" to 60,
        "setenta" to 70, "ochenta" to 80, "noventa" to 90)
    private val connectors = setOf("con", "coma", "punto")
    private val filler = setOf("a", "por", "de", "del", "el", "la", "los", "las", "dolar", "dolares", "usd", "bs",
        "bolivares", "bolivar", "x", "cada", "cuesta", "vale", "precio")

    private sealed interface Tok
    private data class Num(val v: BigDecimal, val digits: String, val isInt: Boolean) : Tok
    private data class Word(val w: String) : Tok

    fun parse(spoken: String): VoiceItem {
        val toks = merge(tokenize(spoken))
        // Grupos numéricos con su posición
        val numbers = toks.filterIsInstance<Num>()
        val price = numbers.lastOrNull()?.v
        val qty = if (numbers.size >= 2) numbers.first().v else null
        val name = toks.filterIsInstance<Word>().map { it.w }.filter { it !in filler }
            .joinToString(" ").trim()
            .replaceFirstChar { it.uppercase() }
        return VoiceItem(name, price, qty)
    }

    private fun tokenize(s: String): List<Tok> {
        val norm = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
        val raw = Regex("""\d+(?:[.,]\d+)?|[a-z]+""").findAll(norm).map { it.value }.toList()
        val out = mutableListOf<Tok>()
        var i = 0
        while (i < raw.size) {
            val t = raw[i]
            if (t[0].isDigit()) {
                val clean = t.replace(',', '.')
                out += Num(BigDecimal(clean), clean, !clean.contains('.')); i++; continue
            }
            val wn = wordNumber(raw, i)
            if (wn != null) {
                val (value, used) = wn
                out += Num(BigDecimal(value), value.toString(), true); i += used
            } else { out += Word(t); i++ }
        }
        return out
    }

    /** Número en palabras 0..199 ("cuarenta y cinco", "ciento diez"). Devuelve (valor, palabras usadas). */
    private fun wordNumber(w: List<String>, i: Int): Pair<Int, Int>? {
        var pos = i; var total = 0
        if (w[pos] == "cien" || w[pos] == "ciento") { total = 100; pos++; if (w[i] == "cien") return total to 1 }
        val t = w.getOrNull(pos)
        when {
            t != null && tens.containsKey(t) -> {
                total += tens.getValue(t); pos++
                if (w.getOrNull(pos) == "y" && units[w.getOrNull(pos + 1)] in 1..9) { total += units.getValue(w[pos + 1]); pos += 2 }
            }
            t != null && units.containsKey(t) -> { total += units.getValue(t); pos++ }
            else -> if (pos == i) return null
        }
        return if (pos > i) total to (pos - i) else null
    }

    /** Une "2 con 50", "3 coma 25" y "tres cincuenta" en un solo precio decimal. */
    private fun merge(t: List<Tok>): List<Tok> {
        val out = mutableListOf<Tok>()
        var i = 0
        while (i < t.size) {
            val a = t[i]
            if (a is Num && a.isInt) {
                val b = t.getOrNull(i + 1); val c = t.getOrNull(i + 2)
                if (b is Word && b.w in connectors && c is Num && c.isInt) {
                    out += decimal(a, c); i += 3; continue
                }
                if (b is Num && b.isInt && b.v >= BigDecimal(10) && b.v < BigDecimal(100) && a.v < BigDecimal(1000)) {
                    out += decimal(a, b); i += 2; continue
                }
            }
            out += a; i++
        }
        return out
    }

    private fun decimal(whole: Num, frac: Num): Num {
        val f = frac.v.toInt()
        val cents = if (frac.digits.length <= 1 || f < 10) "0$f".takeLast(2).let { if (frac.digits.startsWith("0")) frac.digits.padEnd(2, '0') else it } else frac.digits.take(2)
        val s = "${whole.v.toPlainString()}.$cents"
        return Num(BigDecimal(s), s, false)
    }
}
