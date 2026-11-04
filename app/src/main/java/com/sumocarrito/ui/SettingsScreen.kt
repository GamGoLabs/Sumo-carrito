package com.sumocarrito.ui
 
import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.sumocarrito.ads.AdIds
import com.sumocarrito.billing.Skus
import com.sumocarrito.domain.InputSanitizer
import com.sumocarrito.export.Exporter
import java.math.BigDecimal
 
@Composable
fun SettingsScreen(vm: CartViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val s by vm.uiState.collectAsState()
    val pro by vm.isPro.collectAsState()
    val history by vm.history.collectAsState()
    val pct = BigDecimal(100)
 
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onBack) { Text("← Volver") }
        val hc by vm.highContrast.collectAsState()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(hc, { vm.setHighContrast(it) })
            Spacer(Modifier.width(10.dp)); Text("Modo alto contraste (negro/amarillo)")
        }
        Text("Impuestos", style = MaterialTheme.typography.titleMedium)
        var vat by remember { mutableStateOf(s.config.vatRate.multiply(pct).stripTrailingZeros().toPlainString()) }
        var igtf by remember { mutableStateOf(s.config.igtfRate.multiply(pct).stripTrailingZeros().toPlainString()) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(vat, { vat = it; InputSanitizer.parseDecimal(it)?.let { v -> vm.setConfig(s.config.copy(vatRate = v.divide(pct))) } },
                label = { Text("IVA %") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(igtf, { igtf = it; InputSanitizer.parseDecimal(it)?.let { v -> vm.setConfig(s.config.copy(igtfRate = v.divide(pct))) } },
                label = { Text("IGTF %") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(s.config.pricesIncludeVat, { vm.setConfig(s.config.copy(pricesIncludeVat = it)) })
            Spacer(Modifier.width(10.dp)); Text("Los precios del anaquel ya incluyen IVA")
        }
 
        HorizontalDivider()
        Text(if (pro) "SumoCarrito Pro ✔" else "SumoCarrito Pro", style = MaterialTheme.typography.titleMedium)
        if (!pro) {
            Text("Historial por fecha y comercio, exportar a PDF/Excel y sin anuncios.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ vm.billing.launch(ctx as Activity, Skus.MONTHLY) }, Modifier.weight(1f)) { Text("${vm.billing.priceOf(Skus.MONTHLY) ?: "$0.99"}/mes") }
                Button({ vm.billing.launch(ctx as Activity, Skus.LIFETIME) }, Modifier.weight(1f)) { Text("${vm.billing.priceOf(Skus.LIFETIME) ?: "$4.99"} único") }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ Exporter.share(ctx, Exporter.csv(ctx, history), "text/csv") }, enabled = history.isNotEmpty()) { Text("Exportar Excel/CSV") }
                OutlinedButton({ Exporter.share(ctx, Exporter.pdf(ctx, history), "application/pdf") }, enabled = history.isNotEmpty()) { Text("Exportar PDF") }
            }
            LazyColumn(Modifier.weight(1f)) {
                items(history, key = { h -> h.id }) { h ->
                    ListItem(headlineContent = { Text(h.store) },
                        supportingContent = { Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(h.epochMillis))) },
                        trailingContent = { Text("$ ${h.totalUsd.toPlainString()}") })
                }
            }
        }
 
        Spacer(Modifier.weight(if (pro) 0.001f else 1f))
        // Banner SOLO aquí (nunca en la pantalla de conteo) y SOLO si no es Pro.
        if (!pro) AndroidView(factory = { c ->
            AdView(c).apply { setAdSize(AdSize.BANNER); adUnitId = AdIds.BANNER; loadAd(AdRequest.Builder().build()) }
        }, modifier = Modifier.fillMaxWidth())
    }
}
 
