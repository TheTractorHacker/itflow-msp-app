package com.foleyit.itflow.ui.screens.assets
import androidx.compose.foundation.layout.*; import androidx.compose.foundation.lazy.LazyColumn; import androidx.compose.material.icons.Icons; import androidx.compose.material.icons.outlined.*; import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Modifier; import androidx.compose.ui.platform.LocalClipboardManager; import androidx.compose.ui.text.AnnotatedString; import androidx.compose.ui.text.font.FontWeight; import androidx.compose.ui.unit.dp; import com.foleyit.itflow.data.api.ApiClient; import com.foleyit.itflow.ui.components.ErrorScreen; import com.foleyit.itflow.ui.components.LoadingScreen; import kotlinx.coroutines.launch
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AssetDetailScreen(id: Int) {
    var state by remember { mutableStateOf<Result<com.foleyit.itflow.data.api.AssetDetail>?>(null) }; val scope = rememberCoroutineScope(); val clipboard = LocalClipboardManager.current
    LaunchedEffect(Unit) { scope.launch { state = runCatching { ApiClient.service().getAsset(id) } } }
    when { state == null -> LoadingScreen(); state!!.isFailure -> ErrorScreen(state!!.exceptionOrNull()?.message ?: ""); else -> { val a = state!!.getOrThrow()
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text(a.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                a.type?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                listOfNotNull(
                    a.client?.let { "Client" to it }, a.make?.let { "Make" to it }, a.model?.let { "Model" to it },
                    a.os?.let { "OS" to it }, a.status?.let { "Status" to it }, a.location?.let { "Location" to it }, a.assignedTo?.let { "Assigned To" to it }
                ).forEach { (k, v) -> Row(Modifier.padding(bottom = 6.dp)) { Text(k, modifier = Modifier.width(110.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall); Text(v, fontWeight = FontWeight.Medium) } }
                a.serial?.takeIf { it.isNotBlank() }?.let { serial ->
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Outlined.QrCode, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                        Text(serial, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, modifier = Modifier.weight(1f))
                        IconButton(onClick = { clipboard.setText(AnnotatedString(serial)) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.ContentCopy, "Copy", Modifier.size(18.dp)) }
                    }
                }
            } } }
        }
    } }
}
