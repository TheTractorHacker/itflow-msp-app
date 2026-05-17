package com.foleyit.itflow.ui.screens.worksheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.foleyit.itflow.data.api.*
import com.foleyit.itflow.ui.components.LoadingScreen
import com.foleyit.itflow.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillWorksheetScreen(worksheetId: Int, navController: NavController) {
    var worksheet by remember { mutableStateOf<WorksheetDetail?>(null) }
    var fieldValues by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        worksheet = runCatching { ApiClient.service().getWorksheet(worksheetId) }.getOrNull()
        worksheet?.fields?.forEach { f ->
            if (!f.value.isNullOrEmpty()) fieldValues = fieldValues + (f.id to f.value)
        }
        loading = false
    }

    fun saveAndSign() {
        saving = true
        scope.launch {
            try {
                val responses = fieldValues.map { (fid, v) -> FieldResponse(fid, v) }
                withContext(Dispatchers.IO) {
                    ApiClient.service().saveResponses(worksheetId, SaveResponsesRequest(responses))
                }
                // Navigate to sign screen
                navController.navigate(Screen.SignWorksheet.go(worksheetId)) {
                    popUpTo(Screen.FillWorksheet.go(worksheetId)) { inclusive = true }
                }
            } catch (e: Exception) {
                snackbar.showSnackbar("Failed: ${e.message}")
            } finally {
                saving = false
            }
        }
    }

    fun saveOnly() {
        saving = true
        scope.launch {
            try {
                val responses = fieldValues.map { (fid, v) -> FieldResponse(fid, v) }
                withContext(Dispatchers.IO) {
                    ApiClient.service().saveResponses(worksheetId, SaveResponsesRequest(responses))
                }
                snackbar.showSnackbar("Worksheet saved")
                navController.popBackStack()
            } catch (e: Exception) { snackbar.showSnackbar("Failed: ${e.message}") } finally { saving = false }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(worksheet?.templateName ?: "Fill Worksheet") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) } }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 4.dp) {
                Row(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = ::saveOnly, modifier = Modifier.weight(1f), enabled = !saving) { Text("Save") }
                    Button(onClick = ::saveAndSign, modifier = Modifier.weight(1f), enabled = !saving) {
                        Icon(Icons.Outlined.Draw, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Save & Sign")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (loading) { LoadingScreen(); return@Scaffold }
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            worksheet?.fields?.let { fields ->
                items(fields) { field ->
                    when (field.type) {
                        "heading" -> Text(field.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        "checkbox" -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = fieldValues[field.id] == "true",
                                onCheckedChange = { fieldValues = fieldValues + (field.id to it.toString()) })
                            Spacer(Modifier.width(8.dp))
                            Text(field.name)
                        }
                        "select" -> {
                            val options = field.options?.split(",")?.map { it.trim() } ?: emptyList()
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                                OutlinedTextField(
                                    value = fieldValues[field.id] ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(field.name) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    options.forEach { opt ->
                                        DropdownMenuItem(text = { Text(opt) }, onClick = {
                                            fieldValues = fieldValues + (field.id to opt); expanded = false
                                        })
                                    }
                                }
                            }
                        }
                        "textarea" -> OutlinedTextField(
                            value = fieldValues[field.id] ?: "",
                            onValueChange = { fieldValues = fieldValues + (field.id to it) },
                            label = { Text(field.name) },
                            modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6
                        )
                        "signature" -> Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("${field.name} — will be captured on the Sign screen",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        else -> OutlinedTextField(
                            value = fieldValues[field.id] ?: "",
                            onValueChange = { fieldValues = fieldValues + (field.id to it) },
                            label = { Text(field.name) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                    }
                }
            }
        }
    }
}
