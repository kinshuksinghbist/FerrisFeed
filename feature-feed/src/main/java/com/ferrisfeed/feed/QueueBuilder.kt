package com.ferrisfeed.feed

import com.ferrisfeed.data.Curriculum
import com.ferrisfeed.data.LearnerProfile

/**
 * Orders the main feed (v3). Replaces the old 70/20/10 shuffle, which sorted
 * fresh reels by their index *inside each pack file*, so a brand-new learner
 * got the first reel of all 22 packs in a row: Cargo basics, then unsafe
 * aliasing, then SIMD.
 *
 * Now:
 * 1. Fresh reels follow the curriculum (unit -> topic -> level -> authored
 *    order) for the learner's chosen track(s). Units below their starting
 *    point (experience from onboarding) move to the back instead of vanishing.
 * 2. With both tracks chosen, fresh reels alternate 3 Rust : 2 System Design,
 *    so every set of five has both.
 * 3. Reviews that are due are woven in: at most [DUE_PER_SET] per set of five,
 *    so review never drowns out new material.
 * 4. Seen-but-not-due reels and the other track's fresh reels follow at the end.
 */
object QueueBuilder {
    const val DUE_PER_SET = 2

    fun build(all: List<Reel>, nowMs: Long, profile: LearnerProfile): List<Reel> {
        if (all.isEmpty()) return emptyList()
        val focus = focusTracks(profile.trackFocus)
        val due = all.filter { !it.isNew && it.dueAtEpochMs <= nowMs }.sortedBy { it.dueAtEpochMs }
        val freshByTrack = Track.entries.associateWith { track ->
            all.filter { it.isNew && it.track == track }.sortedWith(freshOrder(track, profile.experience))
        }
        val focusFresh = when (focus.size) {
            1 -> freshByTrack.getValue(focus.first())
            else -> alternate(freshByTrack.getValue(Track.RUST), freshByTrack.getValue(Track.SYSTEM_DESIGN), 3, 2)
        }
        val woven = weave(due, focusFresh)
        val otherFresh = Track.entries.filter { it !in focus }.flatMap { freshByTrack.getValue(it) }
        val later = all.filter { !it.isNew && it.dueAtEpochMs > nowMs }.sortedBy { it.dueAtEpochMs }
        return (woven + otherFresh + later).distinctBy { it.id }
    }

    /** Topic or single-lesson practice: the topic's canonical lesson order. */
    fun topicOrder(reels: List<Reel>): List<Reel> =
        reels.sortedWith(compareBy<Reel>({ it.level }, { it.pathOrder }, { it.id }))

    fun focusTracks(focus: String): List<Track> = when (focus) {
        Track.RUST.id -> listOf(Track.RUST)
        Track.SYSTEM_DESIGN.id -> listOf(Track.SYSTEM_DESIGN)
        else -> listOf(Track.RUST, Track.SYSTEM_DESIGN)
    }

    /** First unit (0-based, within the track) a learner of [experience] starts at. */
    fun startUnit(track: Track, experience: Int): Int = when (track) {
        Track.RUST -> when (experience) { 0 -> 0; 1 -> 1; else -> 4 }
        Track.SYSTEM_DESIGN -> when (experience) { 0 -> 0; 1 -> 1; else -> 3 }
    }

    private fun freshOrder(track: Track, experience: Int): Comparator<Reel> {
        val start = startUnit(track, experience)
        return compareBy<Reel>(
            { if (Curriculum.unitIndexInTrack(it.topic) < start) 1 else 0 },
            { Curriculum.orderOf(it.topic) },
            { it.level },
            { it.pathOrder },
            { it.id },
        )
    }

    private fun alternate(a: List<Reel>, b: List<Reel>, takeA: Int, takeB: Int): List<Reel> {
        val out = ArrayList<Reel>(a.size + b.size)
        var i = 0
        var j = 0
        while (i < a.size || j < b.size) {
            repeat(takeA) { if (i < a.size) out += a[i++] }
            repeat(takeB) { if (j < b.size) out += b[j++] }
        }
        return out
    }

    private fun weave(due: List<Reel>, fresh: List<Reel>): List<Reel> {
        val out = ArrayList<Reel>(due.size + fresh.size)
        var d = 0
        var f = 0
        while (d < due.size || f < fresh.size) {
            var dueInSet = 0
            repeat(FeedPaging.SET_SIZE) { slot ->
                // Reviews take the last slots of a set, after new material.
                val wantDue = d < due.size && dueInSet < DUE_PER_SET &&
                    (slot >= FeedPaging.SET_SIZE - DUE_PER_SET || f >= fresh.size)
                if (wantDue) {
                    out += due[d++]
                    dueInSet++
                } else if (f < fresh.size) {
                    out += fresh[f++]
                } else if (d < due.size) {
                    out += due[d++]
                }
            }
        }
        return out
    }
}
