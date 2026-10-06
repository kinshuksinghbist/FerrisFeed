package com.ferrisfeed.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.ferrisfeed.coreui.FerrisFeedTheme
import com.ferrisfeed.data.ReelDao
import com.ferrisfeed.data.ReelEntity
import com.ferrisfeed.data.SearchFilters as DataSearchFilters
import com.ferrisfeed.data.SearchRepository
import com.ferrisfeed.feed.FeedScreen
import com.ferrisfeed.feed.FeedViewModel
import com.ferrisfeed.path.PathScreen
import com.ferrisfeed.path.SearchResult
import com.ferrisfeed.path.SearchScreen
import com.ferrisfeed.path.defaultPathNodes
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Feed : Route

    @Serializable
    data object Path : Route

    @Serializable
    data object Search : Route

    @Serializable
    data class ReelDetail(val reelId: String) : Route
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var reelDao: ReelDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deepLinkedReel = intent?.data
            ?.takeIf { it.scheme == "ferrisfeed" && it.host == "reel" }
            ?.lastPathSegment
        setContent {
            FerrisFeedTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FerrisFeedNavHost(
                        initialReelId = deepLinkedReel,
                        reelDao = reelDao,
                    )
                }
            }
        }
    }
}

@Composable
fun FerrisFeedNavHost(initialReelId: String?, reelDao: ReelDao) {
    val backStack = rememberNavBackStack(Route.Feed)
    androidx.compose.runtime.LaunchedEffect(initialReelId) {
        if (initialReelId != null) backStack.add(Route.ReelDetail(initialReelId))
    }
    val currentTop = backStack.lastOrNull()
    val selectedTab: Route = when (currentTop) {
        is Route.Path -> Route.Path
        is Route.Search -> Route.Search
        else -> Route.Feed
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == Route.Feed,
                    onClick = {
                        backStack.clear()
                        backStack.add(Route.Feed)
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == Route.Feed) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Feed"
                        )
                    },
                    label = { Text("Feed") }
                )
                NavigationBarItem(
                    selected = selectedTab == Route.Path,
                    onClick = {
                        backStack.clear()
                        backStack.add(Route.Path)
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == Route.Path) Icons.Filled.Timeline else Icons.Outlined.Timeline,
                            contentDescription = "Path"
                        )
                    },
                    label = { Text("Path") }
                )
                NavigationBarItem(
                    selected = selectedTab == Route.Search,
                    onClick = {
                        backStack.clear()
                        backStack.add(Route.Search)
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == Route.Search) Icons.Filled.Search else Icons.Outlined.Search,
                            contentDescription = "Search"
                        )
                    },
                    label = { Text("Search") }
                )
            }
        }
    ) { innerPadding ->
        NavDisplay(
            modifier = Modifier.padding(innerPadding),
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = { key ->
                when (key) {
                    is Route.Feed -> NavEntry(key) {
                        FeedEntry(focusedReelId = null)
                    }
                    is Route.Path -> NavEntry(key) {
                        PathScreen(
                            nodes = remember { defaultPathNodes() },
                            onNodeClick = { backStack.add(Route.Search) },
                        )
                    }
                    is Route.Search -> NavEntry(key) {
                        SearchEntry(reelDao = reelDao) { reelId ->
                            backStack.add(Route.ReelDetail(reelId))
                        }
                    }
                    is Route.ReelDetail -> NavEntry(key) {
                        FeedEntry(focusedReelId = key.reelId)
                    }
                    else -> error("Unknown route $key")
                }
            }
        )
    }
}

/** Feed tab fragment: Hilt-provided ViewModel. */
@Composable
private fun FeedEntry(focusedReelId: String?) {
    val viewModel: FeedViewModel = hiltViewModel()
    FeedScreen(
        viewModel = viewModel,
        focusedReelId = focusedReelId,
    )
}

/** Search tab fragment: Room FTS via :data, mapped to path UI models. */
@Composable
private fun SearchEntry(reelDao: ReelDao, onOpenReel: (String) -> Unit) {
    val repository = remember(reelDao) { SearchRepository(reelDao) }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    val scope = rememberCoroutineScope()
    SearchScreen(
        results = results,
        onQueryChanged = { query, filters ->
            scope.launch {
                results = repository.search(
                    query = query,
                    filters = DataSearchFilters(
                        track = filters.track,
                        minLevel = filters.level ?: 1,
                        maxLevel = filters.level ?: 3,
                        hasCode = filters.hasCode,
                        hasQuiz = filters.hasQuiz,
                    ),
                ).map { it.toSearchResult() }
            }
        },
        onResultClick = { result -> onOpenReel(result.id) },
    )
}

private fun ReelEntity.toSearchResult(): SearchResult = SearchResult(
    id = id,
    track = track,
    level = level,
    hook = hook,
    takeaway = takeaway,
    hasCode = hasCode,
    hasQuiz = hasQuiz,
    snippet = bodyMd.take(140),
)

private fun <T> MutableList<T>.clear() {
    while (isNotEmpty()) removeAt(0)
}
