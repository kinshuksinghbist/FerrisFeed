package com.ferrisfeed.path

import kotlin.math.roundToInt

/**
 * Progress system: XP, mastery %, streaks, heatmap levels, Ferris evolution,
 * and achievements. Pure functions + thin state holder; persistence lives in
 * `ProgressStore` (data module), UI reads via ViewModel.
 *
 * Rationale (see docs/learning-science.md):
 * - XP rewards *retrieval* (quiz answers), not passive scrolling.
 * - Streaks reward daily return, capped so one long session can't farm them.
 * - Ferris evolution is a cosmetic mastery proxy: Egg -> Crab -> Armored Crab.
 */
object ProgressSystem {

    // ---- XP ----

    /** Base XP awards. Quiz bonus dominates so skimming pays poorly. */
    const val XP_REEL_SEEN = 10
    const val XP_QUIZ_CORRECT = 15
    const val XP_QUIZ_HARD = 8
    const val XP_SAVE = 2
    const val XP_DAILY_MIX_COMPLETE = 50

    /**
     * Streak multiplier: +5% per streak day, capped at +50% (10+ days).
     * Rewards return visits without making early content trivially farmable.
     */
    fun streakMultiplier(streakDays: Int): Float =
        1f + (streakDays.coerceIn(0, 10) * 0.05f)

    fun xpForReel(streakDays: Int, saved: Boolean = false): Int =
        ((XP_REEL_SEEN + if (saved) XP_SAVE else 0) * streakMultiplier(streakDays)).roundToInt()

    /** grade: 1=Again, 3=Hard, 4=Got it (FSRS grades). */
    fun xpForQuiz(grade: Int, streakDays: Int): Int {
        val base = when (grade) {
            4 -> XP_QUIZ_CORRECT
            3 -> XP_QUIZ_HARD
            else -> 0
        }
        return (base * streakMultiplier(streakDays)).roundToInt()
    }

    // ---- Mastery ----

    /** 0..1 -> 0..100 display percent. */
    fun masteryPct(score: Float): Int = (score.coerceIn(0f, 1f) * 100).roundToInt()

    fun masteryTier(pct: Int): MasteryTier = when {
        pct >= 85 -> MasteryTier.MASTERED
        pct >= 60 -> MasteryTier.PROFICIENT
        pct >= 30 -> MasteryTier.LEARNING
        else -> MasteryTier.NEW
    }

    // ---- Heatmap ----

    /** GitHub-style 0..4 activity level from daily XP. */
    fun heatLevel(dayXp: Int): Int = when {
        dayXp <= 0 -> 0
        dayXp < 30 -> 1
        dayXp < 80 -> 2
        dayXp < 150 -> 3
        else -> 4
    }

    // ---- Ferris evolution ----

    /**
     * Egg (0-499) -> Crab (500-1999) -> Armored Crab (2000+).
     * Thresholds ~= 1 week / 1 month of casual daily mixes.
     */
    fun ferrisStage(totalXp: Int): FerrisStage = when {
        totalXp >= FerrisStage.ARMORED_CRAB.minXp -> FerrisStage.ARMORED_CRAB
        totalXp >= FerrisStage.CRAB.minXp -> FerrisStage.CRAB
        else -> FerrisStage.EGG
    }

    fun xpToNextStage(totalXp: Int): Int? {
        return when (ferrisStage(totalXp)) {
            FerrisStage.EGG -> FerrisStage.CRAB.minXp - totalXp
            FerrisStage.CRAB -> FerrisStage.ARMORED_CRAB.minXp - totalXp
            FerrisStage.ARMORED_CRAB -> null
        }
    }

    // ---- Achievements ----

    /**
     * Evaluates unlocks against a snapshot of user stats. The ViewModel calls
     * this after each meaningful event and persists newly unlocked IDs.
     */
    fun checkAchievements(stats: AchievementStats): List<Achievement> {
        return ALL_ACHIEVEMENTS.filter { it !in stats.unlocked && it.unlocks(stats) }
    }

    val ALL_ACHIEVEMENTS: List<Achievement> = listOf(
        Achievement(
            id = "borrow_checker_survivor",
            title = "Borrow Checker Survivor",
            description = "Reach 80% mastery in Ownership and Borrowing.",
            unlocks = { s -> (s.mastery["ownership"] ?: 0f) >= 0.8f && (s.mastery["borrowing"] ?: 0f) >= 0.8f },
        ),
        Achievement(
            id = "p99_slayer",
            title = "P99 Slayer",
            description = "Reach 70% mastery in Rate Limiter + answer its quiz correctly 3 times.",
            unlocks = { s ->
                (s.mastery["rate-limiter"] ?: 0f) >= 0.7f && s.rateLimiterCorrect >= 3
            },
        ),
        Achievement(
            id = "seven_day_streak",
            title = "Crab Walk Week",
            description = "Keep a 7-day streak.",
            unlocks = { s -> s.streakDays >= 7 },
        ),
        Achievement(
            id = "daily_mixer",
            title = "Daily Mixer",
            description = "Finish 5 Daily Mixes.",
            unlocks = { s -> s.dailyMixesCompleted >= 5 },
        ),
        Achievement(
            id = "lifetimes_tamer",
            title = "Lifetimes Tamer",
            description = "Reach 70% mastery in Lifetimes.",
            unlocks = { s -> (s.mastery["lifetimes"] ?: 0f) >= 0.7f },
        ),
    )
}

enum class MasteryTier(val label: String) {
    NEW("New"),
    LEARNING("Learning"),
    PROFICIENT("Proficient"),
    MASTERED("Mastered"),
}

enum class FerrisStage(val minXp: Int, val label: String, val asset: String) {
    EGG(0, "Egg", "ferris_egg.png"),
    CRAB(500, "Crab", "ferris_crab.png"),
    ARMORED_CRAB(2000, "Armored Crab", "ferris_armored.png"),
}

/** Unlock predicate snapshot; built by the ViewModel from store + DAO. */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val unlocks: (AchievementStats) -> Boolean,
)

data class AchievementStats(
    val mastery: Map<String, Float> = emptyMap(),
    val streakDays: Int = 0,
    val rateLimiterCorrect: Int = 0,
    val dailyMixesCompleted: Int = 0,
    val unlocked: Set<Achievement> = emptySet(),
)
