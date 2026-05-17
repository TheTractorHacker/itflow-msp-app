package com.foleyit.itflow.ui.screens.worksheets

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.foleyit.itflow.data.api.*
import com.foleyit.itflow.ui.components.ErrorScreen
import com.foleyit.itflow.ui.components.LoadingScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignWorksheetScreen(worksheetId: Int, navController: NavController) {
    var worksheet by remember { mutableStateOf<Result<WorksheetDetail>?>(null) }
    var signedName by remember { mutableStateOf("") }
    var paths by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var canvasSize by remember { mutableStateOf(android.util.Size(0, 0)) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        worksheet = runCatching { ApiClient.service().getWorksheet(worksheetId) }
    }

    fun clearSignature() {
        paths = emptyList()
        currentPath = emptyList()
    }

    fun captureSignature(): String? {
        if (paths.isEmpty() && currentPath.isEmpty()) return null
        val w = canvasSize.width.takeIf { it > 0 } ?: 600
        val h = canvasSize.height.takeIf { it > 0 } ?: 200
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 4f
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            isAntiAlias = true
        }
        (paths + if (currentPath.size > 1) listOf(currentPath) else emptyList()).forEach { pts ->
            if (pts.size < 2) return@forEach
            val path = android.graphics.Path()
            path.moveTo(pts[0].x, pts[0].y)
            pts.drop(1).forEach { path.lineTo(it.x, it.y) }
            canvas.drawPath(path, paint)
        }
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
        return "data:image/png;base64," + Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    fun sign() {
        if (signedName.isBlank()) { error = "Name is required"; return }
        val allPaths = paths + if (currentPath.size > 1) listOf(currentPath) else emptyList()
        if (allPaths.isEmpty()) { error = "Please draw your signature"; return }
        loading = true; error = null
        scope.launch {
            try {
                val sig = captureSignature() ?: run { error = "Failed to capture signature"; loading = false; return@launch }
                withContext(Dispatchers.IO) {
                    ApiClient.service().signWorksheet(worksheetId, SignRequest(signedName.trim(), sig))
                }
                navController.popBackStack()
            } catch (e: Exception) {
                error = "Failed to sign: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign Worksheet") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        when {
            worksheet == null -> LoadingScreen()
            worksheet!!.isFailure -> ErrorScreen(worksheet!!.exceptionOrNull()?.message ?: "")
            else -> {
                val ws = worksheet!!.getOrThrow()
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Worksheet info
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(ws.templateName ?: "Worksheet",
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (ws.signed) {
                                    Spacer(Modifier.height(8.dp))
                                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Outlined.CheckCircle, null, Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Signed by ${ws.signedName} on ${ws.signedAt?.take(10) ?: ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Fields (read-only view)
                    if (ws.fields.isNotEmpty()) {
                        items(ws.fields.filter { it.type != "signature" }) { field ->
                            when (field.type) {
                                "heading" -> Text(field.name, style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                                else -> Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(field.name, style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.height(4.dp))
                                        Text(field.value ?: "—", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }

                    if (!ws.signed) {
                        // Name field
                        item {
                            OutlinedTextField(
                                value = signedName, onValueChange = { signedName = it },
                                label = { Text("Full Name") },
                                leadingIcon = { Icon(Icons.Outlined.Person, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        // Signature canvas
                        item {
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text("Signature", style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(onClick = ::clearSignature) {
                                        Icon(Icons.Outlined.Clear, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Clear")
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .background(Color.White)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                                ) {
                                    // Hint text
                                    if (paths.isEmpty() && currentPath.isEmpty()) {
                                        Text("Draw your signature here",
                                            modifier = Modifier.align(Alignment.Center),
                                            color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .pointerInput(Unit) {
                                                detectDragGestures(
                                                    onDragStart = { offset ->
                                                        currentPath = listOf(offset)
                                                        canvasSize = android.util.Size(size.width, size.height)
                                                    },
                                                    onDrag = { change, _ ->
                                                        currentPath = currentPath + change.position
                                                    },
                                                    onDragEnd = {
                                                        if (currentPath.size > 1) paths = paths + listOf(currentPath)
                                                        currentPath = emptyList()
                                                    }
                                                )
                                            }
                                    ) {
                                        val paint = androidx.compose.ui.graphics.Paint().apply {
                                            color = Color.Black
                                            strokeWidth = 4f
                                            style = PaintingStyle.Stroke
                                            strokeCap = StrokeCap.Round
                                            strokeJoin = StrokeJoin.Round
                                        }
                                        (paths + if (currentPath.size > 1) listOf(currentPath) else emptyList())
                                            .forEach { pts ->
                                                if (pts.size < 2) return@forEach
                                                drawPath(
                                                    path = androidx.compose.ui.graphics.Path().apply {
                                                        moveTo(pts[0].x, pts[0].y)
                                                        pts.drop(1).forEach { lineTo(it.x, it.y) }
                                                    },
                                                    color = Color.Black,
                                                    style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                                )
                                            }
                                    }
                                }
                            }
                        }

                        // Error
                        if (error != null) {
                            item {
                                Text(error!!, color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        // Sign button
                        item {
                            Button(
                                onClick = ::sign,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                enabled = !loading
                            ) {
                                if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                else { Icon(Icons.Outlined.Draw, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Sign Worksheet") }
                            }
                        }
                    }
                }
            }
        }
    }
}
