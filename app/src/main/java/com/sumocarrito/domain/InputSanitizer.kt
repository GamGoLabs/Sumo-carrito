package com.sumocarrito.domain

import java.math.BigDecimal

/** Normaliza entrada ("1,50", "1.50", "1.234,56") a BigDecimal seguro. El último separador es el decimal. */
object InputSanitizer {
    fun parseDecimal(raw: String): BigDecimal? {
        val s = raw.trim().filter { it.isDigit() || it == '.' || it == ',' }
        if (s.isEmpty()) return null
        val lastSep = s.lastIndexOfAny(charArrayOf('.', ','))
        val normalized = if (lastSep < 0) s else {
            val intPart = s.substring(0, lastSep).filter { it.isDigit() }
            val frac = s.substring(lastSep + 1).filter { it.isDigit() }
            if (frac.isEmpty()) intPart else "${intPart.ifEmpty { "0" }}.$frac"
        }
        return normalized.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 }
    }
}
