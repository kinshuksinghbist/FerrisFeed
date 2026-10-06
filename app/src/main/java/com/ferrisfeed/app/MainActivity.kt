package com.ferrisfeed.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.ferrisfeed.coreui.FerrisFeedTheme
import com.ferrisfeed.feed.FeedScreen
import com.ferrisfeed.feed.FeedViewModel
import com.ferrisfeed.path.PathScreen
import com.ferrisfeed.path.PathViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Feed : Route

    @Serializable
    data object Path : Route

    @Serializable
    data class ReelDetail(val reelId: String) : Route

    /** Topic-only feed reached from a roadmap node (quiz + info mixed). */
    @Serializable
    data class TopicFeed(val topicId: String) : Route
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deepLinkedReel = intent?.data
            ?.takeIf { it.scheme == "ferrisfeed" && it.host == "reel" }
            ?.lastPathSegment
        setContent {
            FerrisFeedTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FerrisFeedNavHost(initialReelId = deepLinkedReel)
                }
            }
        }
    }
}

/**
 * Feed + Path only (Spec v2, S7): the Search tab and its route are gone —
 * search lives at the top of the Path screen — and tapping a roadmap node
 * opens a topic-filtered feed.
 */
@Composable
fun FerrisFeedNavHost(initialReelId: String?) {
    val backStack = rememberNavBackStack(Route.Feed)
    LaunchedEffect(initialReelId) {
        if (initialReelId != null) backStack.add(Route.ReelDetail(initialReelId))
    }
    val currentTop = backStack.lastOrNull()
    val selectedTab: Route = when (currentTop) {
        is Route.Path -> Route.Path
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
                        FeedEntry(focusedReelId = null, topicFilter = null)
                    }
                    is Route.TopicFeed -> NavEntry(key) {
                        FeedEntry(focusedReelId = null, topicFilter = key.topicId)
                    }
                    is Route.ReelDetail -> NavEntry(key) {
                        FeedEntry(focusedReelId = key.reelId, topicFilter = null)
                    }
                    is Route.Path -> NavEntry(key) {
                        val viewModel: PathViewModel = hiltViewModel()
                        val state by viewModel.state.collectAsState()
                        PathScreen(
                            state = state,
                            onQueryChanged = viewModel::onQueryChanged,
                            onResultClick = { result -> backStack.add(Route.ReelDetail(result.id)) },
                            onTopicClick = { node -> backStack.add(Route.TopicFeed(node.id)) },
                        )
                    }
                    // NavDisplay hands back a plain NavKey, so the branch list
                    // is not provably exhaustive without this.
                    else -> error("Unknown route $key")
                }
            }
        )
    }
}

/**
 * Feed tab fragment: Hilt-provided ViewModel. [topicFilter] narrows the queue
 * to one roadmap topic; null is the full 70/20/10 mix.
 *
 * The topic rebuild is awaited before focusing a reel: the queue has to exist
 * before `focusReel` can find the target in it.
 */
@Composable
private fun FeedEntry(focusedReelId: String?, topicFilter: String?) {
    val viewModel: FeedViewModel = hiltViewModel()
    LaunchedEffect(topicFilter, focusedReelId) {
        viewModel.setTopicFilter(topicFilter)?.join()
        if (focusedReelId != null) viewModel.focusReel(focusedReelId)
    }
    FeedScreen(viewModel = viewModel)
}

private fun <T> MutableList<T>.clear() {
    while (isNotEmpty()) removeAt(0)
}
