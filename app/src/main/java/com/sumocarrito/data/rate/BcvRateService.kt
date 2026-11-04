package com.sumocarrito.data.rate

import com.sumocarrito.domain.BcvRate
import com.sumocarrito.domain.BcvRateParser
import com.sumocarrito.domain.DolarApiParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

/**
 * Obtiene la tasa oficial USD/Bs, siempre con validación TLS normal (no se acepta ningún certificado dudoso):
 *  1) https://www.bcv.org.ve/            (fuente oficial; el BCV suele tener problemas de certificado)
 *  2) https://ve.dolarapi.com/...        (servicio que republica la tasa oficial del BCV en JSON)
 * Solo se hace una petición GET pública; no se envía ningún dato del usuario.
 */
@Singleton
class BcvRateService @Inject constructor() {

    suspend fun fetch(): Result<BcvRate> = withContext(Dispatchers.IO) {
        val direct = try { BcvRateParser.parse(get("https://www.bcv.org.ve/", 6_000)) } catch (e: Exception) { null }
        if (direct != null) return@withContext Result.success(direct)

        val viaApi = try { DolarApiParser.parse(get("https://ve.dolarapi.com/v1/dolares/oficial", 8_000)) } catch (e: Exception) { null }
        if (viaApi != null) return@withContext Result.success(viaApi)

        Result.failure(IllegalStateException(
            "No se pudo obtener la tasa (sin conexión o fuentes no disponibles). Se mantiene la última; puedes escribirla a mano."))
    }

    private fun get(url: String, timeoutMs: Int): String {
        val c = URL(url).openConnection() as HttpsURLConnection
        try {
            c.connectTimeout = timeoutMs; c.readTimeout = timeoutMs
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) SumoCarrito")
            c.setRequestProperty("Accept", "application/json, text/html")
            if (c.responseCode != 200) throw java.io.IOException("HTTP ${c.responseCode}")
            return c.inputStream.use { it.readBytes() }.take(3_000_000).toByteArray().toString(Charsets.UTF_8)
        } finally { c.disconnect() }
    }
}
