package com.sumocarrito.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumocarrito.domain.*
import com.sumocarrito.ui.theme.*
import java.math.BigDecimal

private fun BigDecimal.m() = setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
private val Mono = FontFamily.Monospace
private val MonoStyle = TextStyle(fontFamily = Mono, fontSize = 18.sp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: CartViewModel, onSettings: () -> Unit, onFinish: (store: String) -> Unit) {
    val s by vm.uiState.collectAsState()
    val t = s.totals
    var showAdd by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    var store by remember { mutableStateOf("") }

    val hc = LocalHighContrast.current
    val pal = statusPalette()
    val rateStatus by vm.rateStatus.collectAsState()
    val rateTick by vm.rateTick.collectAsState()
    val statusColor = when (t?.budgetStatus) {
        BudgetStatus.GREEN -> pal.green; BudgetStatus.YELLOW -> pal.amber; BudgetStatus.RED -> pal.red
        else -> MaterialTheme.colorScheme.outline
    }

    Scaffold(
        // Barra inferior FIJA: siempre visible el "A pagar en caja" y el botón de agregar (zona del pulgar).
        bottomBar = {
            Surface(tonalElevation = 8.dp, shadowElevation = 12.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("A PAGAR EN CAJA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        if (t != null && t.payUsd.signum() > 0) Text("$ ${t.payUsd.m()}", fontFamily = Mono, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        if (t != null && t.payBs.signum() > 0) Text("Bs ${t.payBs.m()}", fontFamily = Mono, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        if (t == null || (t.payUsd.signum() == 0 && t.payBs.signum() == 0)) Text("$ 0.00", fontFamily = Mono, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Button({ showAdd = true }, Modifier.height(64.dp)) {
                        Text("+ AGREGAR", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(horizontal = 12.dp).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // ---- Presupuesto + tasa ----
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                var b by remember { mutableStateOf(if (s.budget.signum() > 0) s.budget.toPlainString() else "") }
                OutlinedTextField(b, { b = it; vm.setBudget(it) }, label = { Text("Presupuesto USD") },
                    singleLine = true, modifier = Modifier.weight(1f), textStyle = MonoStyle)
                var r by remember(rateTick) { mutableStateOf(s.rate.toPlainString()) }
                OutlinedTextField(r, { r = it; vm.setRate(it) }, label = { Text("Tasa Bs/$") },
                    singleLine = true, modifier = Modifier.weight(1f), textStyle = MonoStyle)
                TextButton({ vm.refreshRate() }, contentPadding = PaddingValues(4.dp)) { Text(if (rateStatus.loading) "…" else "BCV", fontWeight = FontWeight.Bold) }
                TextButton(onSettings, contentPadding = PaddingValues(4.dp)) { Text("⚙", fontSize = 20.sp) }
            }
            val rateMsg = rateStatus.error ?: if (rateStatus.label.isNotBlank()) "Tasa BCV · ${rateStatus.label}" else null
            rateMsg?.let { Text(it, fontSize = 12.sp, color = if (rateStatus.error != null) pal.amber else MaterialTheme.colorScheme.onSurface) }

            // ---- Total en vivo (semáforo) ----
            Card(colors = CardDefaults.cardColors(containerColor = if (hc) Color.Black else statusColor.copy(alpha = 0.18f)),
                border = if (hc) BorderStroke(4.dp, statusColor) else null,
                elevation = CardDefaults.cardElevation(6.dp), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("TOTAL", fontSize = 12.sp)
                    Text("$ ${t?.totalUsd?.m() ?: "0.00"}", fontSize = 40.sp, fontFamily = Mono, fontWeight = FontWeight.Bold, color = statusColor)
                    Text("Bs ${t?.totalBs?.m() ?: "0.00"}", fontSize = 22.sp, fontFamily = Mono)
                    if (s.budget.signum() > 0 && t != null) {
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (t.budgetUsedPct.toFloat() / 100f).coerceIn(0f, 1f) },
                            color = statusColor, modifier = Modifier.fillMaxWidth().height(10.dp))
                        Text("${t.budgetUsedPct.m()}% del presupuesto" + if (t.budgetStatus == BudgetStatus.RED) "  ¡EXCEDIDO!" else "",
                            fontFamily = Mono, color = statusColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ---- Zona desplazable: productos, método de pago y desglose ----
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(s.lines, key = { it.id }) { l ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(l.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text("${l.quantity.stripTrailingZeros().toPlainString()} × $${l.unitPrice.m()}", fontFamily = Mono)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (l.vatExempt) "Exento" else "IVA", fontSize = 11.sp)
                                Switch(l.vatExempt, { vm.toggleExempt(l) })
                            }
                            IconButton({ vm.removeItem(l.id) }) { Icon(Icons.Default.Delete, "Eliminar") }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Método de pago", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(s.payment is PaymentSelection.CashUsd, { vm.setPayment(PaymentSelection.CashUsd()) }, { Text("Efectivo $") })
                            FilterChip(s.payment is PaymentSelection.DigitalBs, { vm.setPayment(PaymentSelection.DigitalBs) }, { Text("Pago Móvil/Débito") })
                            FilterChip(s.payment is PaymentSelection.Mixed, { vm.setPayment(PaymentSelection.Mixed(BigDecimal.ZERO)) }, { Text("Mixto") })
                        }
                        (s.payment as? PaymentSelection.Mixed)?.let {
                            var c by remember { mutableStateOf("") }
                            OutlinedTextField(c, { c = it; vm.setPayment(PaymentSelection.Mixed(InputSanitizer.parseDecimal(it) ?: BigDecimal.ZERO)) },
                                label = { Text("Parte en efectivo USD") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                        (s.payment as? PaymentSelection.CashUsd)?.let {
                            var c by remember { mutableStateOf("") }
                            OutlinedTextField(c, { c = it; vm.setPayment(PaymentSelection.CashUsd(InputSanitizer.parseDecimal(it))) },
                                label = { Text("Pago con billete de USD (vuelto)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                if (t != null) item {
                    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row2("Base exenta", "$ ${t.exemptBase.m()}")
                            Row2("Base gravada", "$ ${t.taxableBase.m()}")
                            Row2("IVA ${s.config.vatRate.multiply(BigDecimal(100)).stripTrailingZeros().toPlainString()}%", "$ ${t.vat.m()}")
                            Row2("IGTF ${s.config.igtfRate.multiply(BigDecimal(100)).stripTrailingZeros().toPlainString()}%", "$ ${t.igtf.m()}")
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                            if (t.payUsd.signum() > 0) Row2("A PAGAR EN CAJA (USD)", "$ ${t.payUsd.m()}", big = true)
                            if (t.payBs.signum() > 0) Row2("A PAGAR EN CAJA (Bs)", "Bs ${t.payBs.m()}", big = true)
                            t.changeUsd?.let { Row2("Vuelto", "$ ${it.m()}") }
                            TextButton({ confirmFinish = true }, Modifier.align(Alignment.End), enabled = s.lines.isNotEmpty()) { Text("Finalizar compra") }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) AddItemSheet({ showAdd = false }) { n, p, q, e -> vm.addItem(n, p, q, e) }

    if (confirmFinish) AlertDialog(
        onDismissRequest = { confirmFinish = false },
        title = { Text("Finalizar compra") },
        text = { OutlinedTextField(store, { store = it.take(40) }, label = { Text("Establecimiento (Pro: se guarda)") }, singleLine = true) },
        confirmButton = { TextButton({ confirmFinish = false; onFinish(store); store = "" }) { Text("Finalizar y limpiar") } },
        dismissButton = { TextButton({ confirmFinish = false }) { Text("Cancelar") } })
}

@Composable
private fun Row2(l: String, v: String, big: Boolean = false) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(l, fontSize = if (big) 16.sp else 14.sp, fontWeight = if (big) FontWeight.Bold else FontWeight.Normal)
    Text(v, fontFamily = Mono, fontSize = if (big) 20.sp else 14.sp, fontWeight = if (big) FontWeight.Bold else FontWeight.Normal)
}
