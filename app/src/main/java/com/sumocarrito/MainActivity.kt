package com.sumocarrito

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.sumocarrito.ads.AdManager
import com.sumocarrito.ui.*
import com.sumocarrito.ui.theme.SumoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: CartViewModel by viewModels()
    @Inject lateinit var ads: AdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ads.init()
        vm.billing.start()

        lifecycleScope.launch {
            vm.events.collect { if (it is UiEvent.BudgetExceeded) vibrate() }
        }

        setContent {
            val hc by vm.highContrast.collectAsState()
            SumoTheme(hc) {
                Surface {
                    var screen by remember { mutableStateOf("home") }
                    val pro by vm.isPro.collectAsState()
                    if (screen == "home") HomeScreen(vm, { screen = "settings" }) { store ->
                        vm.finishPurchase(store) { if (!pro) ads.showInterstitial(this@MainActivity) {} }
                    } else SettingsScreen(vm) { screen = "home" }
                }
            }
        }
    }

    override fun onResume() { super.onResume(); lifecycleScope.launch { vm.billing.refresh() } }

    private fun vibrate() {
        val v = if (Build.VERSION.SDK_INT >= 31)
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") (getSystemService(VIBRATOR_SERVICE) as Vibrator)
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 120, 250), -1))
    }
}
