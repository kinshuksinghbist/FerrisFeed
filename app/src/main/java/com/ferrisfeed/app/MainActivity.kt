package com.ferrisfeed.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.ferrisfeed.coreui.FerrisFeedTheme
import com.ferrisfeed.coreui.FerrisMotion
import com.ferrisfeed.coreui.LocalBottomBarInset
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
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(transparent, transparent),
            navigationBarStyle = SystemBarStyle.auto(transparent, transparent),
        )
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
 * Feed + Path app shell with floating glass nav bar, persistent top stat bar,
 * and custom route transitions (Spec v2 + P6 32).
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

    val statsViewModel: StatsViewModel = hiltViewModel()
    val userStats by statsViewModel.stats.collectAsState()

    val statusBarsTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomInset = 64.dp + 12.dp + navBarsBottom

    val isTopicFeed = currentTop is Route.TopicFeed
    val isReelDetail = currentTop is Route.ReelDetail
    val isDetailRoute = isTopicFeed || isReelDetail

    CompositionLocalProvider(LocalBottomBarInset provides bottomInset) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavDisplay(
                modifier = Modifier.fillMaxSize(),
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                transitionSpec = {
                    val initialKey = initialState as? Route
                    val targetKey = targetState as? Route
                    if ((initialKey is Route.Feed && targetKey is Route.Path) ||
                        (initialKey is Route.Path && targetKey is Route.Feed)
                    ) {
                        (fadeIn(animationSpec = tween(200)) + scaleIn(animationSpec = tween(200), initialScale = 0.96f)) togetherWith
                            (fadeOut(animationSpec = tween(200)) + scaleOut(animationSpec = tween(200), targetScale = 1.04f))
                    } else {
                        (slideInHorizontally(
                            animationSpec = tween(350, easing = FastOutSlowInEasing),
                            initialOffsetX = { (it * 0.3f).toInt() },
                        ) + fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing))) togetherWith
                            (slideOutHorizontally(
                                animationSpec = tween(350, easing = FastOutSlowInEasing),
                                targetOffsetX = { (-it * 0.3f).toInt() },
                            ) + fadeOut(animationSpec = tween(350, easing = FastOutSlowInEasing)))
                    }
                },
                popTransitionSpec = {
                    (slideInHorizontally(
                        animationSpec = tween(350, easing = FastOutSlowInEasing),
                        initialOffsetX = { (-it * 0.3f).toInt() },
                    ) + fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(350, easing = FastOutSlowInEasing),
                            targetOffsetX = { (it * 0.3f).toInt() },
                        ) + fadeOut(animationSpec = tween(350, easing = FastOutSlowInEasing)))
                },
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
                        else -> error("Unknown route $key")
                    }
                },
            )

            // Persistent top stat bar or topic back bar
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = statusBarsTop + 8.dp)
            ) {
                if (isTopicFeed) {
                    val topicId = (currentTop as Route.TopicFeed).topicId
                    val prettyTitle = topicId
                        .replace('_', ' ')
                        .replace('-', ' ')
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    BackChipBar(
                        title = prettyTitle,
                        onBack = { backStack.removeLastOrNull() },
                    )
                } else if (isReelDetail) {
                    BackChipBar(
                        title = "Reel",
                        onBack = { backStack.removeLastOrNull() },
                    )
                } else {
                    StatBar(
                        stats = userStats,
                        title = if (selectedTab is Route.Path) "Path" else null,
                    )
                }
            }

            // Floating glass navigation bar
            AnimatedVisibility(
                visible = !isDetailRoute,
                enter = slideInVertically(animationSpec = FerrisMotion.QuickOffset) { it } + fadeIn(animationSpec = FerrisMotion.Quick),
                exit = slideOutVertically(animationSpec = FerrisMotion.QuickOffset) { it } + fadeOut(animationSpec = FerrisMotion.Quick),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navBarsBottom + 12.dp)
            ) {
                FerrisNavBar(
                    selectedRoute = selectedTab,
                    onSelectRoute = { route ->
                        if (selectedTab != route) {
                            while (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                            if (backStack.isNotEmpty()) {
                                backStack[0] = route
                            } else {
                                backStack.add(route)
                            }
                        }
                    },
                )
            }
        }
    }
}

/**
 * Feed tab fragment: Hilt-provided ViewModel. [topicFilter] narrows the queue
 * to one roadmap topic; null is the full 70/20/10 mix.
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
