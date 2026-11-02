package com.sumocarrito.data.rate

import com.sumocarrito.domain.BcvRate
import com.sumocarrito.domain.BcvRateParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLException

/**
 * Lee la tasa oficial de https://www.bcv.org.ve/ . Valida el certificado de forma normal
 * (no se desactiva la seguridad TLS): si el BCV presenta un certificado inválido, la
 * consulta falla y el usuario puede escribir la tasa a mano.
 */
@Singleton
class BcvRateService @Inject constructor() {
    suspend fun fetch(): Result<BcvRate> = withContext(Dispatchers.IO) {
        try {
            val c = URL("https://www.bcv.org.ve/").openConnection() as HttpsURLConnection
            c.connectTimeout = 10_000; c.readTimeout = 10_000
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) SumoCarrito")
            val html = c.inputStream.use { it.readBytes().take(3_000_000).toByteArray().toString(Charsets.UTF_8) }
            c.disconnect()
            BcvRateParser.parse(html)?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("No se encontró la tasa en la página del BCV (¿cambió su formato?)"))
        } catch (e: SSLException) {
            Result.failure(IllegalStateException("No se pudo verificar el certificado seguro del BCV. Ingresa la tasa a mano."))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Sin conexión o BCV no responde. Se mantiene la última tasa."))
        }
    }
}
