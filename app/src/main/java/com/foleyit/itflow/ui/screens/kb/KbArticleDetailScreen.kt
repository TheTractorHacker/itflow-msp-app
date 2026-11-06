package com.foleyit.itflow.ui.screens.kb

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.data.api.KbArticleDetail
import com.foleyit.itflow.ui.components.ErrorScreen
import com.foleyit.itflow.ui.components.LoadingScreen
import kotlinx.coroutines.launch
import com.foleyit.itflow.ui.util.userMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KbArticleDetailScreen(id: Int, navController: NavController) {
    var state by remember { mutableStateOf<Result<KbArticleDetail>?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(id) {
        state = runCatching { ApiClient.service().getKbArticle(id) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state?.getOrNull()?.title ?: "Article") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val result = state) {
            null -> Box(Modifier.fillMaxSize().padding(padding)) { LoadingScreen() }
            else -> result.fold(
                onSuccess = { article ->
                    ArticleContent(article, Modifier.fillMaxSize().padding(padding))
                },
                onFailure = {
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        ErrorScreen(userMessage(it)) {
                            scope.launch { state = runCatching { ApiClient.service().getKbArticle(id) } }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ArticleContent(article: KbArticleDetail, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val baseUrl = ApiClient.serverUrl
    val scheme = MaterialTheme.colorScheme
    fun css(c: androidx.compose.ui.graphics.Color) = "#%06X".format(0xFFFFFF and c.toArgb())
    val text = css(scheme.onSurface); val bg = css(scheme.surface); val muted = css(scheme.onSurfaceVariant)
    val link = css(scheme.primary); val line = css(scheme.outlineVariant); val codeBg = css(scheme.surfaceVariant)

    Column(modifier) {
        article.categoryName?.let { category ->
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        category,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = {
                WebView(context).apply {
                    settings.javaScriptEnabled = false
                    // Reflow to the screen width instead of zooming a desktop-width page out; wide tables and code scroll sideways inside themselves.
                    settings.loadWithOverviewMode = false
                    settings.useWideViewPort = false
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    overScrollMode = android.view.View.OVER_SCROLL_NEVER
                    isVerticalScrollBarEnabled = false
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            openExternalUrl(context, request.url)
                            return true
                        }
                    }
                }
            },
            update = { webView ->
                val html = """
                    <html><head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        html { -webkit-text-size-adjust: 100%; }
                        body { margin: 0; padding: 4px 18px 40px; background: $bg; color: $text;
                               font: 16px/1.65 -apple-system, Roboto, 'Segoe UI', sans-serif; word-wrap: break-word; overflow-wrap: anywhere; }
                        h1, h2, h3, h4 { line-height: 1.25; margin: 1.4em 0 .5em; font-weight: 650; }
                        h1 { font-size: 1.55em; } h2 { font-size: 1.3em; } h3 { font-size: 1.1em; }
                        p { margin: 0 0 1em; } ul, ol { padding-left: 1.4em; margin: 0 0 1em; } li { margin: .25em 0; }
                        a { color: $link; }
                        img, video, iframe { max-width: 100%; height: auto; border-radius: 8px; }
                        blockquote { margin: 1em 0; padding: .1em 1em; border-left: 3px solid $line; color: $muted; }
                        hr { border: 0; border-top: 1px solid $line; margin: 1.5em 0; }
                        pre, code { background: $codeBg; border-radius: 6px; font-family: monospace; font-size: .9em; }
                        code { padding: .1em .35em; }
                        pre { padding: 12px; overflow-x: auto; white-space: pre; } pre code { padding: 0; background: none; }
                        table { display: block; overflow-x: auto; border-collapse: collapse; max-width: 100%; margin: 0 0 1em; }
                        td, th { border: 1px solid $line; padding: 6px 10px; text-align: left; }
                        th { background: $codeBg; }
                    </style>
                    </head><body>${article.content ?: ""}</body></html>
                """.trimIndent()
                webView.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", null)
            }
        )

        if (!article.attachments.isNullOrEmpty()) {
            HorizontalDivider()
            Column(Modifier.padding(12.dp)) {
                Text("Attachments", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                article.attachments.forEach { att ->
                    val url = if (att.url.startsWith("http")) att.url else "$baseUrl${att.url}"
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.AttachFile, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(att.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { openExternalUrl(context, Uri.parse(url)) }) {
                            Icon(Icons.AutoMirrored.Outlined.OpenInNew, "Open")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Opens a URL sourced from KB article HTML/attachments in an external app. Restricted to http(s)
 * only — article content originates from the trusted backend today, but this is defense-in-depth
 * against a stored-HTML/URL injection issue rejecting schemes like `intent://`, `file://`, `javascript:`.
 */
private fun openExternalUrl(context: Context, uri: Uri?) {
    if (uri == null || uri.scheme?.lowercase() !in setOf("http", "https")) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        android.widget.Toast.makeText(context, "No app found to open this link", android.widget.Toast.LENGTH_SHORT).show()
    }
}
