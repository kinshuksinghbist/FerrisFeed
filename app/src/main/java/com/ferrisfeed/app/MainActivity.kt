package com.ferrisfeed.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.ferrisfeed.coreui.FerrisFeedTheme
import com.ferrisfeed.coreui.LocalBottomBarInset
import com.ferrisfeed.data.Curriculum
import com.ferrisfeed.feed.FeedScope
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
    data object Profile : Route

    /** One lesson (search hit, saved lesson, deep link) played inside its topic. */
    @Serializable
    data class ReelDetail(val reelId: String) : Route

    /** A curriculum topic in lesson order, reached from Path or You. */
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
                val app: AppViewModel = hiltViewModel()
                val learner by app.learner.collectAsState()
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    val profile = learner
                    when {
                        // DataStore not answered yet: keep the splash colour for a frame.
                        profile == null -> Unit
                        !profile.onboarded -> OnboardingScreen(onDone = app::completeOnboarding)
                        else -> FerrisFeedNavHost(initialReelId = deepLinkedReel)
                    }
                }
            }
        }
    }
}

private fun Route.isTab(): Boolean = this is Route.Feed || this is Route.Path || this is Route.Profile

/**
 * Learn · Path · You, with topic and lesson feeds pushed on top. The nav bar
 * is a solid bar under the content (it hides on pushed feeds, which get a
 * back button in their own top bar instead).
 */
@Composable
fun FerrisFeedNavHost(initialReelId: String?) {
    val backStack = rememberNavBackStack(Route.Feed)
    LaunchedEffect(initialReelId) {
        if (initialReelId != null) backStack.add(Route.ReelDetail(initialReelId))
    }
    val top = backStack.lastOrNull() as? Route ?: Route.Feed
    val selectedTab: Route = (backStack.firstOrNull() as? Route) ?: Route.Feed
    val showBar = top.isTab()
    val systemNavInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    Column(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalBottomBarInset provides if (showBar) 0.dp else systemNavInset) {
            NavDisplay(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                backStack = backStack,
                onBack = { pop() },
                transitionSpec = {
                    val from = initialState as? Route
                    val to = targetState as? Route
                    if (from?.isTab() == true && to?.isTab() == true) {
                        fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                    } else {
                        (slideInHorizontally(tween(260)) { it / 5 } + fadeIn(tween(260))) togetherWith fadeOut(tween(160))
                    }
                },
                popTransitionSpec = {
                    fadeIn(tween(200)) togetherWith (slideOutHorizontally(tween(220)) { it / 5 } + fadeOut(tween(180)))
                },
                entryProvider = { key ->
                    when (key) {
                        is Route.Feed -> NavEntry(key) {
                            FeedEntry(scope = FeedScope.Main, onBack = null, title = null)
                        }
                        is Route.TopicFeed -> NavEntry(key) {
                            FeedEntry(
                                scope = FeedScope.Topic(key.topicId),
                                onBack = { pop() },
                                title = Curriculum.titleOf(key.topicId),
                            )
                        }
                        is Route.ReelDetail -> NavEntry(key) {
                            FeedEntry(scope = FeedScope.Lesson(key.reelId), onBack = { pop() }, title = null)
                        }
                        is Route.Path -> NavEntry(key) {
                            val vm: PathViewModel = hiltViewModel()
                            val state by vm.state.collectAsState()
                            LaunchedEffect(Unit) { vm.refresh() }
                            PathScreen(
                                state = state,
                                onSelectTrack = vm::selectTrack,
                                onOpenTopic = { topic -> backStack.add(Route.TopicFeed(topic.key)) },
                                onSearchOpen = vm::setSearchOpen,
                                onQueryChanged = vm::onQueryChanged,
                                onResultClick = { result -> backStack.add(Route.ReelDetail(result.id)) },
                            )
                        }
                        is Route.Profile -> NavEntry(key) {
                            val vm: ProfileViewModel = hiltViewModel()
                            val state by vm.state.collectAsState()
                            ProfileScreen(
                                state = state,
                                onOpenLesson = { id -> backStack.add(Route.ReelDetail(id)) },
                                onOpenTopic = { topic -> backStack.add(Route.TopicFeed(topic)) },
                                onRemoveSaved = vm::removeSaved,
                                onChangeFocus = vm::setFocus,
                            )
                        }
                        else -> NavEntry(key) { }
                    }
                },
            )
        }
        AnimatedVisibility(
            visible = showBar,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            FerrisNavBar(
                selected = selectedTab,
                onSelect = { route ->
                    while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    if (backStack.isEmpty()) backStack.add(route) else backStack[0] = route
                },
            )
        }
    }
}

/**
 * Every feed route shares one [FeedViewModel] (learnings §22), so each entry
 * states its scope; the view model rebuilds only when the scope changes.
 */
@Composable
private fun FeedEntry(scope: FeedScope, onBack: (() -> Unit)?, title: String?) {
    val viewModel: FeedViewModel = hiltViewModel()
    LaunchedEffect(scope) { viewModel.setScope(scope) }
    FeedScreen(viewModel = viewModel, onBack = onBack, title = title)
}
