package com.example.pikaroo.ui.orders.view

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pikaroo.ui.orders.viewmodel.OrderLookupUiState
import com.example.pikaroo.ui.orders.viewmodel.OrderLookupViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun OrderLookupView(onBack: () -> Unit, onFound: (String) -> Unit) {
    val model: OrderLookupViewModel = viewModel(factory = OrderLookupViewModel.Factory)
    val state by model.state.collectAsState()
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        val raw = result.contents
        if (raw == null) model.scanCancelled() else model.scanned(raw) { onFound(it.id) }
    }
    OrderLookupContent(state, model::updateInput,
        onLookup = { model.lookup { onFound(it.id) } },
        onScan = {
            scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Escanea el QR de tu pedido").setBeepEnabled(false).setOrientationLocked(false))
        },
        onBack = onBack)
}

@Composable
fun OrderLookupContent(
    state: OrderLookupUiState,
    onInput: (String) -> Unit,
    onLookup: () -> Unit,
    onScan: () -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
            Text("Consultar pedido", style = MaterialTheme.typography.titleLarge)
        }
        Text("Escanea el QR o pega el ID. Se consultan tus pedidos guardados en este dispositivo.")
        OutlinedButton(onClick = onScan, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
            Text("Escanear QR")
        }
        state.notice?.let { Text(it) }
        OutlinedTextField(
            value = state.input, onValueChange = onInput, enabled = !state.isLoading,
            label = { Text("ID de la orden") }, placeholder = { Text("order_…") },
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("order_lookup_id")
        )
        Button(onClick = onLookup, enabled = !state.isLoading && state.input.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("order_lookup_search")) {
            Text(if (state.isLoading) "Buscando…" else "Buscar pedido")
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("order_lookup_error")) }
    }
}
