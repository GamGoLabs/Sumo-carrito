package com.sumocarrito.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumocarrito.domain.VoiceParser

/** Teclado numérico propio: nunca se abre el teclado del sistema, así que no tapa la pantalla. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemSheet(onDismiss: () -> Unit, onAdd: (name: String, price: String, qty: String, exempt: Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1") }
    var exempt by remember { mutableStateOf(false) }
    var editingQty by remember { mutableStateOf(false) }
    var voiceMsg by remember { mutableStateOf<String?>(null) }
    val (listening, toggleVoice) = rememberVoiceInput(
        onText = { spoken ->
            val v = VoiceParser.parse(spoken)
            if (v.name.isNotBlank()) name = v.name.take(40)
            v.price?.let { price = it.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() }
            v.quantity?.let { qty = it.stripTrailingZeros().toPlainString() }
            voiceMsg = if (v.price == null) "Escuché: \"$spoken\". No detecté el precio, usa el teclado." else "Escuché: \"$spoken\" — revisa y toca AGREGAR."
        },
        onError = { voiceMsg = it })

    fun press(k: String) {
        val cur = if (editingQty) qty else price
        val maxDec = if (editingQty) 3 else 2
        val next = when (k) {
            "⌫" -> cur.dropLast(1)
            "." -> if ('.' in cur) cur else (if (cur.isEmpty()) "0." else "$cur.")
            else -> {
                val dec = cur.substringAfter('.', "")
                if ('.' in cur && dec.length >= maxDec || cur.length >= 9) cur
                else if (cur == "0") k else cur + k
            }
        }
        if (editingQty) qty = next else price = next
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(name, { name = it.take(40) }, label = { Text("Producto (opcional)") },
                    singleLine = true, modifier = Modifier.weight(1f))
                if (listening) Button(toggleVoice, Modifier.height(56.dp)) { Text("● Escuchando…") }
                else FilledTonalButton(toggleVoice, Modifier.height(56.dp)) { Text("🎤 Dictar") }
            }
            voiceMsg?.let { Text(it, fontSize = 13.sp) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldBox("Precio USD", if (price.isEmpty()) "0.00" else price, !editingQty, Modifier.weight(2f)) { editingQty = false }
                FieldBox("Cantidad", qty.ifEmpty { "1" }, editingQty, Modifier.weight(1f)) { editingQty = true }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(exempt, { exempt = it })
                Spacer(Modifier.width(12.dp))
                Text(if (exempt) "Exento de IVA" else "Con IVA", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(".", "0", "⌫")).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { k ->
                        FilledTonalButton({ press(k) }, Modifier.weight(1f).height(64.dp)) { Text(k, fontSize = 28.sp) }
                    }
                }
            }
            Button(
                onClick = { onAdd(name, price, qty, exempt); name = ""; price = ""; qty = "1"; editingQty = false },
                enabled = price.toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth().height(60.dp)
            ) { Text("AGREGAR", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun FieldBox(label: String, value: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OutlinedCard(onClick, modifier,
        border = androidx.compose.foundation.BorderStroke(if (active) 3.dp else 1.dp,
            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, fontSize = 12.sp)
            Text(value, fontSize = 30.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}
