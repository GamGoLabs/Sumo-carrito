package com.sumocarrito.data.rate

import android.annotation.SuppressLint
import com.sumocarrito.data.prefs.SettingsStore
import com.sumocarrito.domain.BcvRate
import com.sumocarrito.domain.BcvRateParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.X509TrustManager

/** El certificado del BCV no pasó la validación normal de Android (o cambió desde la última vez). */
class BcvCertException(message: String) : Exception(message)

/**
 * Lee la tasa de https://www.bcv.org.ve/ .
 *  1) Siempre intenta primero con la validación TLS normal del sistema.
 *  2) Si falla y el usuario lo autorizó ([lenient]), usa "huella fijada en el primer uso": se guarda el
 *     SHA-256 del certificado del BCV y desde entonces SOLO se acepta ese mismo certificado.
 *     Solo se descarga una página pública; no se envía ningún dato del usuario.
 */
@Singleton
class BcvRateService @Inject constructor(private val prefs: SettingsStore) {

    suspend fun fetch(lenient: Boolean): Result<BcvRate> = withContext(Dispatchers.IO) {
        try {
            val html = try {
                download(null)
            } catch (e: SSLException) {
                if (!lenient) throw BcvCertException(
                    "El certificado de seguridad del BCV no pudo verificarse. Puedes autorizar una conexión con huella fijada o escribir la tasa a mano.")
                download(pinningFactory())
            }
            BcvRateParser.parse(html)?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("No se encontró la tasa en la página del BCV (¿cambió su formato?)"))
        } catch (e: BcvCertException) {
            Result.failure(e)
        } catch (e: SSLException) {
            Result.failure(BcvCertException("El certificado del BCV cambió desde la última vez. Autoriza de nuevo si confías en él, o escribe la tasa a mano."))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Sin conexión o el BCV no responde. Se mantiene la última tasa."))
        }
    }

    private fun download(factory: javax.net.ssl.SSLSocketFactory?): String {
        val c = URL("https://www.bcv.org.ve/").openConnection() as HttpsURLConnection
        try {
            c.connectTimeout = 10_000; c.readTimeout = 10_000
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) SumoCarrito")
            if (factory != null) {
                c.sslSocketFactory = factory
                c.hostnameVerifier = HostnameVerifier { host, _ -> host == "www.bcv.org.ve" || host == "bcv.org.ve" }
            }
            return c.inputStream.use { it.readBytes().take(3_000_000).toByteArray().toString(Charsets.UTF_8) }
        } finally { c.disconnect() }
    }

    @SuppressLint("CustomX509TrustManager")
    private fun pinningFactory(): javax.net.ssl.SSLSocketFactory {
        val tm = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
                throw CertificateException("no se usa")
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                if (chain.isEmpty()) throw CertificateException("cadena vacía")
                val fp = MessageDigest.getInstance("SHA-256").digest(chain[0].encoded)
                    .joinToString("") { "%02x".format(it) }
                val pinned = prefs.bcvPin
                if (pinned == null) prefs.bcvPin = fp            // primer uso (autorizado por el usuario)
                else if (pinned != fp) throw CertificateException("huella distinta a la fijada")
            }
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        return SSLContext.getInstance("TLS").apply { init(null, arrayOf(tm), null) }.socketFactory
    }
}
