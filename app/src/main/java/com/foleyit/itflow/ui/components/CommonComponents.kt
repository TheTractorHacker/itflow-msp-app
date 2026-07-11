package com.foleyit.itflow.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
            if (onRetry != null) {
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

@Composable
fun EmptyScreen(message: String, icon: ImageVector? = null) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                Icon(icon, contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(16.dp))
            }
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun PriorityBadge(priority: String?) {
    val color = when (priority?.lowercase()) {
        "critical" -> MaterialTheme.colorScheme.error
        "high"     -> MaterialTheme.colorScheme.errorContainer
        "medium"   -> MaterialTheme.colorScheme.tertiaryContainer
        else       -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when (priority?.lowercase()) {
        "critical" -> MaterialTheme.colorScheme.onError
        "high"     -> MaterialTheme.colorScheme.onErrorContainer
        "medium"   -> MaterialTheme.colorScheme.onTertiaryContainer
        else       -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = color, shape = MaterialTheme.shapes.small) {
        Text(
            priority ?: "Normal",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

/** Shown while the device has no internet connection, so cached/stale reads aren't mistaken for live data. */
@Composable
fun OfflineBanner() {
    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.CloudOff, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "You're offline — showing saved data",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/** Footer row for paged lists — shows a spinner while loading the next page, otherwise a tappable "Load more". */
@Composable
fun LoadMoreRow(loading: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            TextButton(onClick = onClick) { Text("Load more") }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
