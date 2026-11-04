package com.sumocarrito.domain

import java.math.BigDecimal
import java.math.RoundingMode

data class BcvRate(val rate: BigDecimal, val dateLabel: String?)

/** Extrae la tasa USD de la portada de bcv.org.ve (formato venezolano: 39,18130000). */
object BcvRateParser {
    // Estrategia 1: bloque <div id="dolar"> ... <strong> 39,18130000 </strong>
    private val byBlock = Regex("""id\s*=\s*["']dolar["'][\s\S]{0,800}?<strong>\s*([0-9.]*[0-9],[0-9]+)\s*</strong>""", RegexOption.IGNORE_CASE)
    // Estrategia 2 (texto plano / maquetación distinta): "USD" seguido del número
    private val byText = Regex("""USD[^0-9]{0,200}?([0-9]{1,3}(?:\.[0-9]{3})*,[0-9]+)""")
    private val dateRx = Regex("""([A-Za-zÁÉÍÓÚáéíóú]+,\s*\d{1,2}\s+[A-Za-zÁÉÍÓÚáéíóú]+\s+\d{4})""")

    fun parse(html: String): BcvRate? {
        val raw = byBlock.find(html)?.groupValues?.get(1) ?: byText.find(html)?.groupValues?.get(1) ?: return null
        val value = raw.replace(".", "").replace(',', '.').toBigDecimalOrNull() ?: return null
        if (value.signum() <= 0) return null
        return BcvRate(value.setScale(4, RoundingMode.HALF_UP), date(html))
    }

    /** Defensa contra datos falsos: la tasa nueva no puede alejarse >25% de la anterior (si hay referencia). */
    fun plausible(new: BigDecimal, previous: BigDecimal): Boolean {
        if (new.signum() <= 0) return false
        if (previous <= BigDecimal.ONE) return true
        val ratio = new.divide(previous, 4, RoundingMode.HALF_UP)
        return ratio >= BigDecimal("0.75") && ratio <= BigDecimal("1.25")
    }

    private fun date(html: String): String? {
        val i = html.indexOf("Fecha Valor", ignoreCase = true)
        if (i < 0) return null
        val chunk = html.substring(i, minOf(html.length, i + 600)).replace(Regex("<[^>]+>"), " ")
        return dateRx.find(chunk)?.groupValues?.get(1)?.replace(Regex("\\s+"), " ")
    }
}
