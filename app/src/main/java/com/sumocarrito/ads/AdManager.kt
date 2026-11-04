package com.sumocarrito.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

object AdIds {   // IDs de PRUEBA de Google. Sustituir por los reales en producción.
    const val BANNER = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
}

@Singleton
class AdManager @Inject constructor(@ApplicationContext private val ctx: Context) {
    private var interstitial: InterstitialAd? = null

    fun init() { MobileAds.initialize(ctx); preload() }

    private fun preload() {
        InterstitialAd.load(ctx, AdIds.INTERSTITIAL, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
                override fun onAdFailedToLoad(e: LoadAdError) { interstitial = null }
            })
    }

    /** Solo se llama al finalizar y limpiar una compra, y solo si el usuario NO es Pro. */
    fun showInterstitial(activity: Activity, then: () -> Unit) {
        val ad = interstitial ?: return then()
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { interstitial = null; preload(); then() }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { interstitial = null; then() }
        }
        ad.show(activity)
    }
}
