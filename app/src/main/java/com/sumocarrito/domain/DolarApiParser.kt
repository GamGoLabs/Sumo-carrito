package com.sumocarrito.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Respuesta de https://ve.dolarapi.com/v1/dolares/oficial (tasa oficial del BCV):
 * {"fuente":"oficial","nombre":"Oficial","compra":null,"venta":null,"promedio":301.37,"fechaActualizacion":"2026-01-01T21:02:47.165Z"}
 * Se usa "promedio"; si faltara, "venta".
 */
object DolarApiParser {
    private fun number(json: String, key: String): BigDecimal? =
        Regex("\"$key\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)").find(json)?.groupValues?.get(1)?.toBigDecimalOrNull()

    fun parse(json: String): BcvRate? {
        val value = number(json, "promedio") ?: number(json, "venta") ?: return null
        if (value.signum() <= 0) return null
        val d = Regex("\"fechaActualizacion\"\\s*:\\s*\"(\\d{4})-(\\d{2})-(\\d{2})").find(json)
            ?.destructured?.let { (y, m, day) -> "$day/$m/$y" }
        return BcvRate(value.setScale(4, RoundingMode.HALF_UP), d, source = "DolarAPI")
    }
}
