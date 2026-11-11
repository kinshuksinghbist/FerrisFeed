package com.ferrisfeed.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ferrisfeed.coreui.CodeBlock
import com.ferrisfeed.coreui.FerrisButton
import com.ferrisfeed.coreui.FerrisFeedTheme
import com.ferrisfeed.coreui.FerrisMark
import com.ferrisfeed.coreui.FerrisMotion
import com.ferrisfeed.coreui.FerrisTheme
import com.ferrisfeed.coreui.FerrisType
import com.ferrisfeed.coreui.Tracks
import com.ferrisfeed.coreui.trackColor
import com.ferrisfeed.data.LearnerProfile

/**
 * First run (v3): three short steps, nothing to type.
 *  1. What FerrisFeed is, in one screen.
 *  2. Which track(s) to learn.
 *  3. How much experience — sets the starting unit; skipped basics stay
 *     available in Path and reappear later in the feed.
 * Choices are editable later from the You tab.
 */
@Composable
fun OnboardingScreen(onDone: (trackFocus: String, experience: Int) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var focus by rememberSaveable { mutableStateOf(LearnerProfile.FOCUS_BOTH) }
    var experience by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step-- }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp),
    ) {
        StepDots(current = step, count = 3, modifier = Modifier.padding(top = 20.dp))
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(FerrisMotion.SmoothOffset) { if (forward) it / 6 else -it / 6 } + fadeIn(FerrisMotion.Smooth)) togetherWith
                    fadeOut(FerrisMotion.Quick)
            },
            label = "onboarding-step",
            modifier = Modifier.weight(1f),
        ) { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 32.dp)) {
                when (s) {
                    0 -> WelcomeStep()
                    1 -> ChoiceStep(
                        title = "What do you want to learn?",
                        subtitle = "You can change this any time.",
                        options = listOf(
                            Choice(Tracks.RUST, "Rust", "Ownership to async, unsafe and performance.", trackColor(Tracks.RUST)),
                            Choice(Tracks.SYSTEM_DESIGN, "System Design", "Caching, databases, scale, distributed systems.", trackColor(Tracks.SYSTEM_DESIGN)),
                            Choice(LearnerProfile.FOCUS_BOTH, "Both", "Three Rust lessons, then two System Design, every set.", MaterialTheme.colorScheme.onSurfaceVariant),
                        ),
                        selected = focus,
                        onSelect = { focus = it },
                    )
                    else -> ChoiceStep(
                        title = "How much have you done?",
                        subtitle = "We'll start you in the right place. Earlier lessons stay in Path.",
                        options = listOf(
                            Choice("0", "I'm new to this", "Start from the very first lesson.", null),
                            Choice("1", "I've written some", "Skip setup basics, start at the core ideas.", null),
                            Choice("2", "I use it at work", "Jump to advanced material.", null),
                        ),
                        selected = experience.toString(),
                        onSelect = { experience = it.toInt() },
                    )
                }
            }
        }
        FerrisButton(
            text = when (step) {
                0 -> "Get started"
                1 -> "Next"
                else -> "Start learning"
            },
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            onClick = { if (step < 2) step++ else onDone(focus, experience) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun WelcomeStep() {
    FerrisMark(size = 72.dp, animated = false)
    Spacer(Modifier.height(28.dp))
    Text(
        "Learn Rust and system design, one swipe at a time.",
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(16.dp))
    Text(
        "Each lesson is one idea you can read in under a minute, followed by a quick check. " +
            "Five lessons make a set. Questions you miss come back until they stick.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(28.dp))
    CodeBlock(
        code = "let a = String::from(\"crab\");\nlet b = a; // a moves into b\nprintln!(\"{b}\");",
        language = "rust",
        output = "crab",
    )
}

private data class Choice(val id: String, val title: String, val body: String, val dot: Color?)

@Composable
private fun ChoiceStep(
    title: String,
    subtitle: String,
    options: List<Choice>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Text(title, style = FerrisType.Hook, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(8.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(24.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { option ->
            ChoiceCard(option = option, isSelected = option.id == selected, onClick = { onSelect(option.id) })
        }
    }
}

@Composable
private fun ChoiceCard(option: Choice, isSelected: Boolean, onClick: () -> Unit) {
    val palette = FerrisTheme.palette
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(if (isSelected) palette.raised else scheme.surface, shape)
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) scheme.onBackground else palette.hairline, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { selected = isSelected }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (option.dot != null) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(option.dot))
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(option.title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
            Text(option.body, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) scheme.onBackground else Color.Transparent)
                .border(1.5.dp, if (isSelected) scheme.onBackground else palette.hairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) Icon(Icons.Filled.Check, contentDescription = null, tint = scheme.background, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun StepDots(current: Int, count: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            Box(
                Modifier
                    .height(4.dp)
                    .width(if (i == current) 24.dp else 12.dp)
                    .clip(CircleShape)
                    .background(if (i <= current) MaterialTheme.colorScheme.onBackground else FerrisTheme.palette.hairline),
            )
        }
    }
}

@Preview(name = "Onboarding", showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 360, heightDp = 760)
@Composable
private fun OnboardingPreview() {
    FerrisFeedTheme(darkTheme = true) {
        OnboardingScreen(onDone = { _, _ -> })
    }
}
