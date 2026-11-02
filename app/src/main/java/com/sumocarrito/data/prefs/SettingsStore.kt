package com.sumocarrito.data.prefs

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/** Preferencias cifradas (AES256) con clave en Android Keystore. Nada sale del dispositivo. */
@Singleton
class SettingsStore @Inject constructor(@ApplicationContext ctx: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        ctx, "secure_prefs",
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var rate: String get() = prefs.getString("rate", "1.00")!!; set(v) = prefs.edit { putString("rate", v) }
    var budget: String get() = prefs.getString("budget", "0")!!; set(v) = prefs.edit { putString("budget", v) }
    var vatRate: String get() = prefs.getString("vat", "0.16")!!; set(v) = prefs.edit { putString("vat", v) }
    var igtfRate: String get() = prefs.getString("igtf", "0.03")!!; set(v) = prefs.edit { putString("igtf", v) }
    var pricesIncludeVat: Boolean get() = prefs.getBoolean("incl", false); set(v) = prefs.edit { putBoolean("incl", v) }
    var rateDate: String get() = prefs.getString("rate_date", "")!!; set(v) = prefs.edit { putString("rate_date", v) }
    var highContrast: Boolean get() = prefs.getBoolean("hc", false); set(v) = prefs.edit { putBoolean("hc", v) }
    var isPro: Boolean get() = prefs.getBoolean("pro", false); set(v) = prefs.edit { putBoolean("pro", v) }

    /** Passphrase aleatoria de 32 bytes para SQLCipher, generada una sola vez. */
    fun dbPassphrase(): ByteArray {
        val existing = prefs.getString("db_key", null)
        if (existing != null) return Base64.decode(existing, Base64.NO_WRAP)
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit { putString("db_key", Base64.encodeToString(key, Base64.NO_WRAP)) }
        return key
    }
}
