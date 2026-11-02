package com.foleyit.itflow.ui.util

import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Wraps a scrolling list in pull-to-refresh backed by [PagedListController.pullRefresh]. The list
 * stays visible while it reloads; a failed reload leaves the old items and says so once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PullToRefreshList(
    controller: PagedListController<T>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val state = controller.state
    val context = LocalContext.current
    LaunchedEffect(state.pullRefreshFailed) {
        if (state.pullRefreshFailed) {
            Toast.makeText(context, "Couldn't refresh. Check your connection and try again.", Toast.LENGTH_SHORT).show()
            controller.clearPullRefreshFailed()
        }
    }
    PullToRefreshBox(
        isRefreshing = state.isPullRefreshing,
        // The API client caches GETs for 5 minutes; a user-requested refresh must reach the server.
        onRefresh = {
            controller.pullRefresh(prepare = {
                withContext(Dispatchers.IO) { com.foleyit.itflow.data.api.ApiClient.clearHttpCache() }
            })
        },
        modifier = modifier
    ) {
        content()
    }
}
