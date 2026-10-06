package com.ferrisfeed.feed

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * "Reel of the Day" Glance widget + streak counter.
 *
 * STUB STATUS: real structure, wired end-to-end except the final DI line.
 * To enable: add `androidx.glance:glance-appwidget` to
 * `:feature-feed/build.gradle.kts`, register [ReelOfDayWidgetReceiver] in
 * the manifest with an `appwidget-provider` XML, and bind the real
 * [WidgetReelProvider] to Room in `FeedModule`. With the NoOp provider the
 * widget renders a static fallback and never crashes.
 *
 * Layout: track pill + hook (max 2 lines) + streak flame count. Tap opens
 * the app deep link `ferrisfeed://reel/<id>` at the exact reel.
 */
class ReelOfDayWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                val hook = prefs[stringPreferencesKey(KEY_HOOK)]
                    ?: "Why does this function not compile?"
                val track = prefs[stringPreferencesKey(KEY_TRACK)] ?: "rust"
                val streak = prefs[stringPreferencesKey(KEY_STREAK)] ?: "0"
                val reelId = prefs[stringPreferencesKey(KEY_REEL_ID)] ?: ""
                WidgetContent(
                    context = context,
                    hook = hook,
                    track = track,
                    streakDays = streak,
                    reelId = reelId,
                )
            }
        }
    }

    companion object {
        const val KEY_HOOK = "widget_hook"
        const val KEY_TRACK = "widget_track"
        const val KEY_STREAK = "widget_streak"
        const val KEY_REEL_ID = "widget_reel_id"

        suspend fun refresh(context: Context, id: GlanceId, reel: WidgetReel, streakDays: Int) {
            updateAppWidgetState(context, id) { prefs ->
                prefs[stringPreferencesKey(KEY_HOOK)] = reel.hook
                prefs[stringPreferencesKey(KEY_TRACK)] = reel.track
                prefs[stringPreferencesKey(KEY_STREAK)] = streakDays.toString()
                prefs[stringPreferencesKey(KEY_REEL_ID)] = reel.id
            }
            ReelOfDayWidget().update(context, id)
        }
    }
}

@Composable
private fun WidgetContent(
    context: Context,
    hook: String,
    track: String,
    streakDays: String,
    reelId: String,
) {
    Column(
        modifier = androidx.glance.GlanceModifier
            .fillMaxSize()
            .padding(16.dp)
            // NB: Glance actions must be activity-class/component based so the
            // launcher host can serialize them; raw Intents are not accepted.
            // Deep-link-to-reel payload rides along once the receiver +
            // appwidget-provider XML land (see class KDoc).
            .clickable(
                actionStartActivity(
                    ComponentName(context.packageName, MAIN_ACTIVITY_CLASS),
                ),
            ),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = track.uppercase(),
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Accents.Orange),
            )
            Spacer(modifier = androidx.glance.GlanceModifier.defaultWeight())
            Text(
                text = "\uD83D\uDD25 $streakDays",
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
            )
        }
        Spacer(modifier = androidx.glance.GlanceModifier.height(6.dp))
        Text(
            text = hook,
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
            maxLines = 3,
        )
        Spacer(modifier = androidx.glance.GlanceModifier.height(4.dp))
        Text(
            text = "FerrisFeed \u00B7 Reel of the Day",
            style = TextStyle(fontSize = 11.sp, color = Accents.Muted),
        )
    }
}

/** Minimal data the widget needs; resolved by [WidgetReelProvider]. */
data class WidgetReel(
    val id: String,
    val hook: String,
    val track: String,
)

/**
 * Production implementation queries Room for the Daily Mix head (or the
 * lowest-stability due card) on a 6h WorkManager tick. Tests use [NoOp].
 */
interface WidgetReelProvider {
    suspend fun reelOfDay(): WidgetReel

    object NoOp : WidgetReelProvider {
        override suspend fun reelOfDay(): WidgetReel = WidgetReel(
            id = "rust-own-014",
            hook = "Why does this function not compile?",
            track = "rust",
        )
    }
}

class ReelOfDayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReelOfDayWidget()
}

private object Accents {
    val Orange = ColorProvider(Color(0xFFFF6B35))
    val Muted = ColorProvider(Color(0xFF9AA3B2))
}

/** Fully-qualified name keeps :feature-feed decoupled from :app. */
private const val MAIN_ACTIVITY_CLASS = "com.ferrisfeed.app.MainActivity"
