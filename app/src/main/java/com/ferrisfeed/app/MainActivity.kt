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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.ferrisfeed.coreui.theme.FerrisFeedTheme
import com.ferrisfeed.feature.feed.FeedScreen
import com.ferrisfeed.feature.path.PathScreen
import com.ferrisfeed.feature.path.SearchScreen
import dagger.hilt.android.AndroidEntryPoint
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

@Composable
fun FerrisFeedNavHost(initialReelId: String?) {
    val backStack = rememberNavBackStack(
        buildList<NavKey> {
            add(Route.Feed)
            if (initialReelId != null) add(Route.ReelDetail(initialReelId))
        }
    )
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
                        FeedScreen(
                            onOpenReel = { reelId -> backStack.add(Route.ReelDetail(reelId)) },
                            onOpenPath = { backStack.add(Route.Path) }
                        )
                    }
                    is Route.Path -> NavEntry(key) {
                        PathScreen(
                            onOpenReel = { reelId -> backStack.add(Route.ReelDetail(reelId)) }
                        )
                    }
                    is Route.Search -> NavEntry(key) {
                        SearchScreen(
                            onOpenReel = { reelId -> backStack.add(Route.ReelDetail(reelId)) }
                        )
                    }
                    is Route.ReelDetail -> NavEntry(key) {
                        FeedScreen(
                            focusedReelId = key.reelId,
                            onOpenReel = { reelId -> backStack.add(Route.ReelDetail(reelId)) },
                            onOpenPath = { backStack.add(Route.Path) }
                        )
                    }
                    else -> error("Unknown route $key")
                }
            }
        )
    }
}

private fun <T> MutableList<T>.clear() {
    while (isNotEmpty()) removeAt(0)
}
