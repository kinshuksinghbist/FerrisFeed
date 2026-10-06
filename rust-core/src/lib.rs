//! `ferris-core` — shared Rust core for FerrisFeed.
//!
//! This crate holds the pieces that must behave identically on Android
//! (via UniFFI, see [`ffi`]) and on the web preview (via wasm-bindgen,
//! see [`wasm`] behind the `wasm` feature):
//!
//! * [`Reel`] / [`Quiz`] curriculum types plus [`validate_reel`].
//! * FSRS-lite spaced-repetition scheduling ([`CardState`], [`Grade`]).
//! * [`xp_for_reel`] XP calculator.
//! * [`TopicGraph`] prerequisite DAG with cycle detection.
//! * [`grade_quiz`] quiz grading.

pub mod ffi;

#[cfg(feature = "wasm")]
pub mod wasm;

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use std::collections::{BTreeMap, BTreeSet, HashSet};
use std::fmt;

/// Maximum words allowed in [`Reel::body_md`] (about 60 seconds of reading).
pub const MAX_BODY_WORDS: usize = 70;
/// Maximum lines allowed in [`Reel::code`].
pub const MAX_CODE_LINES: usize = 15;

// ---------------------------------------------------------------------------
// Curriculum model
// ---------------------------------------------------------------------------

/// Which learning track a reel belongs to.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[serde(rename_all = "kebab-case")]
pub enum Track {
    Rust,
    Wasm,
    SystemDesign,
}

/// A single quiz attached to a reel.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct Quiz {
    /// Question prompt.
    pub q: String,
    /// Answer options; the correct one lives at index [`Quiz::answer`].
    pub options: Vec<String>,
    /// Index into [`Quiz::options`] of the correct answer.
    pub answer: usize,
    /// Explanation shown after answering.
    pub explain: String,
}

/// One bite-size learning card.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct Reel {
    pub id: String,
    pub track: Track,
    pub level: u8,
    /// One-line curiosity gap.
    pub hook: String,
    /// Markdown body, at most [`MAX_BODY_WORDS`] words.
    pub body_md: String,
    /// Optional runnable snippet, at most [`MAX_CODE_LINES`] lines.
    pub code: Option<String>,
    /// Language tag for [`Reel::code`] (e.g. `"rust"`).
    pub language: Option<String>,
    /// One-sentence rule to remember.
    pub takeaway: String,
    /// One common mistake / compiler error.
    pub trap: String,
    pub quiz: Quiz,
}

// ---------------------------------------------------------------------------
// Validation
// ---------------------------------------------------------------------------

/// Reasons a [`Reel`] can fail validation.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ReelError {
    EmptyHook,
    BodyTooLong { words: usize, max: usize },
    CodeTooLong { lines: usize, max: usize },
    EmptyTakeaway,
    EmptyQuestion,
    EmptyQuizOptions,
    AnswerOutOfBounds { answer: usize, options: usize },
}

impl fmt::Display for ReelError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            ReelError::EmptyHook => write!(f, "hook must be non-empty"),
            ReelError::BodyTooLong { words, max } => {
                write!(f, "body has {words} words, max is {max}")
            }
            ReelError::CodeTooLong { lines, max } => {
                write!(f, "code has {lines} lines, max is {max}")
            }
            ReelError::EmptyTakeaway => write!(f, "takeaway must be non-empty"),
            ReelError::EmptyQuestion => write!(f, "quiz question must be non-empty"),
            ReelError::EmptyQuizOptions => write!(f, "quiz must have at least one option"),
            ReelError::AnswerOutOfBounds { answer, options } => {
                write!(
                    f,
                    "quiz answer {answer} out of bounds for {options} options"
                )
            }
        }
    }
}

impl std::error::Error for ReelError {}

/// Enforce the bite-size reel schema: non-empty hook, body of at most
/// [`MAX_BODY_WORDS`] words, code of at most [`MAX_CODE_LINES`] lines,
/// non-empty takeaway, and a quiz whose answer index is in bounds.
pub fn validate_reel(reel: &Reel) -> Result<(), ReelError> {
    if reel.hook.trim().is_empty() {
        return Err(ReelError::EmptyHook);
    }
    let words = reel.body_md.split_whitespace().count();
    if words > MAX_BODY_WORDS {
        return Err(ReelError::BodyTooLong {
            words,
            max: MAX_BODY_WORDS,
        });
    }
    if let Some(code) = reel.code.as_ref() {
        let lines = code.lines().count();
        if lines > MAX_CODE_LINES {
            return Err(ReelError::CodeTooLong {
                lines,
                max: MAX_CODE_LINES,
            });
        }
    }
    if reel.takeaway.trim().is_empty() {
        return Err(ReelError::EmptyTakeaway);
    }
    if reel.quiz.q.trim().is_empty() {
        return Err(ReelError::EmptyQuestion);
    }
    if reel.quiz.options.is_empty() {
        return Err(ReelError::EmptyQuizOptions);
    }
    if reel.quiz.answer >= reel.quiz.options.len() {
        return Err(ReelError::AnswerOutOfBounds {
            answer: reel.quiz.answer,
            options: reel.quiz.options.len(),
        });
    }
    Ok(())
}

// ---------------------------------------------------------------------------
// FSRS-lite scheduler (covers TODO 31, SRS part)
// ---------------------------------------------------------------------------

/// Self-reported review outcome, mapping to the UI's
/// "Again / Hard / Got it" buttons.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum Grade {
    Again,
    Hard,
    GotIt,
}

/// Spaced-repetition state for one reel.
///
/// `stability` is the estimated memory stability in days and `difficulty`
/// runs from 1.0 (easy) to 10.0 (hard). Calling [`CardState::review`]
/// updates both and moves [`CardState::due`] to the next review time.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct CardState {
    pub stability: f32,
    pub difficulty: f32,
    pub due: DateTime<Utc>,
}

impl CardState {
    /// A fresh card: seen today, medium difficulty, due now.
    pub fn new() -> Self {
        Self {
            stability: 1.0,
            difficulty: 5.0,
            due: Utc::now(),
        }
    }

    /// Apply a [`Grade`] and reschedule the card.
    ///
    /// * `Again` collapses stability back to a short interval and raises
    ///   difficulty, so the card comes back much sooner.
    /// * `Hard` grows stability modestly and nudges difficulty up.
    /// * `GotIt` grows stability strongly and lowers difficulty.
    pub fn review(&mut self, grade: Grade) {
        match grade {
            Grade::Again => {
                self.difficulty = (self.difficulty + 1.5).min(10.0);
                self.stability = 0.5;
            }
            Grade::Hard => {
                self.difficulty = (self.difficulty + 0.3).min(10.0);
                self.stability = (self.stability * 1.4).clamp(0.2, 365.0);
            }
            Grade::GotIt => {
                self.difficulty = (self.difficulty - 0.7).max(1.0);
                self.stability = (self.stability * 2.5 + 1.0).clamp(0.2, 365.0);
            }
        }
        let secs = (f64::from(self.stability) * 86_400.0) as i64;
        self.due = Utc::now() + chrono::Duration::seconds(secs.max(60));
    }
}

impl Default for CardState {
    fn default() -> Self {
        Self::new()
    }
}

// ---------------------------------------------------------------------------
// XP calculator
// ---------------------------------------------------------------------------

/// XP for finishing one reel.
///
/// The reward grows with the reel `level` (10 XP per level on top of a
/// 20 XP base), pays a bonus for a correct quiz answer, and adds a
/// streak bonus of 2 XP per streak day capped at 30 days, so XP is
/// monotonically non-decreasing in `streak_days`.
pub fn xp_for_reel(level: u8, quiz_correct: bool, streak_days: u32) -> u32 {
    let base = 20_u32.saturating_add(u32::from(level).saturating_mul(10));
    let quiz_bonus = if quiz_correct { 15 } else { 5 };
    let streak_bonus = streak_days.min(30).saturating_mul(2);
    base.saturating_add(quiz_bonus).saturating_add(streak_bonus)
}

// ---------------------------------------------------------------------------
// Prerequisite DAG (covers TODO 32, DAG part)
// ---------------------------------------------------------------------------

/// Why [`TopicGraph::topo_order`] can fail.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum DagError {
    CycleDetected,
}

impl fmt::Display for DagError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            DagError::CycleDetected => write!(f, "topic graph contains a cycle"),
        }
    }
}

impl std::error::Error for DagError {}

/// Directed acyclic graph of topic prerequisites.
///
/// Edges point from prerequisite to dependent
/// (`add_edge("ownership", "lifetimes")` means ownership must come first).
/// `BTreeSet`/`BTreeMap` storage keeps iteration order deterministic.
#[derive(Debug, Clone, Default)]
pub struct TopicGraph {
    topics: BTreeSet<String>,
    /// prereq -> dependents
    edges: BTreeMap<String, BTreeSet<String>>,
    /// topic -> its prerequisites
    prereqs: BTreeMap<String, BTreeSet<String>>,
}

impl TopicGraph {
    pub fn new() -> Self {
        Self::default()
    }

    /// Register a topic (idempotent).
    pub fn add_topic(&mut self, id: &str) {
        self.topics.insert(id.to_string());
    }

    /// Declare that `prereq` must be completed before `next`.
    /// Unknown topics are registered implicitly.
    pub fn add_edge(&mut self, prereq: &str, next: &str) {
        self.add_topic(prereq);
        self.add_topic(next);
        self.edges
            .entry(prereq.to_string())
            .or_default()
            .insert(next.to_string());
        self.prereqs
            .entry(next.to_string())
            .or_default()
            .insert(prereq.to_string());
    }

    /// Deterministic topological order (Kahn's algorithm).
    /// Returns [`DagError::CycleDetected`] when the graph has a cycle.
    pub fn topo_order(&self) -> Result<Vec<String>, DagError> {
        let mut indegree: BTreeMap<&String, usize> = BTreeMap::new();
        for topic in &self.topics {
            let degree = self.prereqs.get(topic).map_or(0, BTreeSet::len);
            indegree.insert(topic, degree);
        }
        // BTreeSet acts as a sorted work queue for deterministic output.
        let mut ready: BTreeSet<&String> = indegree
            .iter()
            .filter(|(_, degree)| **degree == 0)
            .map(|(topic, _)| *topic)
            .collect();
        let mut order: Vec<String> = Vec::with_capacity(self.topics.len());
        while let Some(node) = ready.iter().next().cloned() {
            ready.remove(node);
            order.push(node.clone());
            if let Some(dependents) = self.edges.get(node) {
                for dependent in dependents {
                    if let Some(degree) = indegree.get_mut(dependent) {
                        *degree -= 1;
                        if *degree == 0 {
                            ready.insert(dependent);
                        }
                    }
                }
            }
        }
        if order.len() == self.topics.len() {
            Ok(order)
        } else {
            Err(DagError::CycleDetected)
        }
    }

    /// Topics whose prerequisites are all in `completed` and which are not
    /// themselves completed yet, in sorted order.
    pub fn unlocked(&self, completed: &HashSet<String>) -> Vec<String> {
        self.topics
            .iter()
            .filter(|topic| {
                !completed.contains(*topic)
                    && self
                        .prereqs
                        .get(*topic)
                        .is_none_or(|required| required.iter().all(|pre| completed.contains(pre)))
            })
            .cloned()
            .collect()
    }
}

// ---------------------------------------------------------------------------
// Quiz grading
// ---------------------------------------------------------------------------

/// Grade one quiz answer: `(correct, explanation)`.
/// The explanation is always the reel's own, so learners see the
/// reasoning whether they were right or wrong.
pub fn grade_quiz(reel: &Reel, chosen: usize) -> (bool, String) {
    (chosen == reel.quiz.answer, reel.quiz.explain.clone())
}

// ---------------------------------------------------------------------------
// JSON helpers shared by the UniFFI (ffi) and wasm-bindgen (wasm) shims
// ---------------------------------------------------------------------------

pub(crate) fn validate_reel_json_inner(reel_json: &str) -> String {
    match serde_json::from_str::<Reel>(reel_json) {
        Err(err) => serde_json::json!({"ok": false, "error": format!("invalid reel json: {err}")})
            .to_string(),
        Ok(reel) => match validate_reel(&reel) {
            Ok(()) => serde_json::json!({"ok": true}).to_string(),
            Err(err) => serde_json::json!({"ok": false, "error": err.to_string()}).to_string(),
        },
    }
}

pub(crate) fn grade_quiz_json_inner(reel_json: &str, chosen: usize) -> String {
    match serde_json::from_str::<Reel>(reel_json) {
        Err(err) => serde_json::json!({"error": format!("invalid reel json: {err}")}).to_string(),
        Ok(reel) => {
            let (correct, explanation) = grade_quiz(&reel, chosen);
            serde_json::json!({"correct": correct, "explanation": explanation}).to_string()
        }
    }
}

// UniFFI scaffolding for the `#[uniffi::export]` items in [`ffi`].
uniffi::setup_scaffolding!();

// ---------------------------------------------------------------------------
// Unit tests (`cargo test` must stay green)
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    fn sample_reel() -> Reel {
        Reel {
            id: "rust-own-014".to_string(),
            track: Track::Rust,
            level: 1,
            hook: "Why does this simple function not compile?".to_string(),
            body_md: "Ownership moves values. Only one owner exists at a time.".to_string(),
            code: Some("fn main() {\n    let s = String::from(\"hi\");\n}".to_string()),
            language: Some("rust".to_string()),
            takeaway: "Each value has exactly one owner.".to_string(),
            trap: "Using a moved value fails to compile.".to_string(),
            quiz: Quiz {
                q: "What happens after `let t = s;`?".to_string(),
                options: vec!["s is moved".to_string(), "s is copied".to_string()],
                answer: 0,
                explain: "String is not Copy, so ownership moves.".to_string(),
            },
        }
    }

    #[test]
    fn validator_accepts_valid_reel() {
        assert_eq!(validate_reel(&sample_reel()), Ok(()));
    }

    #[test]
    fn validator_rejects_empty_hook() {
        let mut reel = sample_reel();
        reel.hook = "   ".to_string();
        assert_eq!(validate_reel(&reel), Err(ReelError::EmptyHook));
    }

    #[test]
    fn validator_rejects_long_body() {
        let mut reel = sample_reel();
        reel.body_md = (0..(MAX_BODY_WORDS + 1))
            .map(|i| format!("word{i}"))
            .collect::<Vec<_>>()
            .join(" ");
        let err = validate_reel(&reel).expect_err("body over 70 words must fail");
        assert!(matches!(err, ReelError::BodyTooLong { .. }), "got {err:?}");
    }

    #[test]
    fn validator_rejects_long_code() {
        let mut reel = sample_reel();
        reel.code = Some(
            (0..(MAX_CODE_LINES + 1))
                .map(|i| format!("let x{i} = {i};"))
                .collect::<Vec<_>>()
                .join("\n"),
        );
        assert!(matches!(
            validate_reel(&reel),
            Err(ReelError::CodeTooLong { .. })
        ));
    }

    #[test]
    fn validator_rejects_answer_out_of_bounds() {
        let mut reel = sample_reel();
        reel.quiz.answer = reel.quiz.options.len();
        assert!(matches!(
            validate_reel(&reel),
            Err(ReelError::AnswerOutOfBounds { .. })
        ));
    }

    #[test]
    fn srs_again_resets_sooner_than_got_it() {
        let mut again = CardState::new();
        let mut got_it = CardState::new();
        again.review(Grade::Again);
        got_it.review(Grade::GotIt);
        assert!(
            again.due < got_it.due,
            "Again ({}) must be due sooner than GotIt ({})",
            again.due,
            got_it.due
        );
        assert!(again.stability < got_it.stability);
    }

    #[test]
    fn srs_hard_lands_between_again_and_got_it() {
        let mut again = CardState::new();
        let mut hard = CardState::new();
        let mut got_it = CardState::new();
        again.review(Grade::Again);
        hard.review(Grade::Hard);
        got_it.review(Grade::GotIt);
        assert!(again.due < hard.due);
        assert!(hard.due < got_it.due);
    }

    #[test]
    fn dag_topo_order_respects_edges() {
        let mut graph = TopicGraph::new();
        graph.add_edge("ownership", "lifetimes");
        graph.add_edge("lifetimes", "async");
        graph.add_edge("async", "axum");
        let order = graph.topo_order().expect("acyclic graph must sort");
        for (first, second) in [
            ("ownership", "lifetimes"),
            ("lifetimes", "async"),
            ("async", "axum"),
        ] {
            let a = order.iter().position(|t| t == first).unwrap();
            let b = order.iter().position(|t| t == second).unwrap();
            assert!(a < b, "order {order:?} violates {first} -> {second}");
        }
    }

    #[test]
    fn dag_detects_cycle() {
        let mut graph = TopicGraph::new();
        graph.add_edge("a", "b");
        graph.add_edge("b", "a");
        assert_eq!(graph.topo_order(), Err(DagError::CycleDetected));
    }

    #[test]
    fn dag_unlocked_returns_ready_topics() {
        let mut graph = TopicGraph::new();
        graph.add_edge("ownership", "lifetimes");
        graph.add_edge("ownership", "borrowing");
        let completed: HashSet<String> = ["ownership".to_string()].into_iter().collect();
        let unlocked = graph.unlocked(&completed);
        assert_eq!(
            unlocked,
            vec!["borrowing".to_string(), "lifetimes".to_string()]
        );
        // Nothing completed: only the root is unlocked.
        let unlocked = graph.unlocked(&HashSet::new());
        assert_eq!(unlocked, vec!["ownership".to_string()]);
    }

    #[test]
    fn xp_monotonic_with_streak() {
        let mut previous = 0;
        for streak in 0..=40 {
            let xp = xp_for_reel(2, true, streak);
            assert!(xp >= previous, "XP dipped at streak {streak}");
            previous = xp;
        }
        assert!(xp_for_reel(2, true, 10) > xp_for_reel(2, true, 0));
    }

    #[test]
    fn xp_rewards_level_and_correct_quiz() {
        assert!(xp_for_reel(3, true, 0) > xp_for_reel(1, true, 0));
        assert!(xp_for_reel(1, true, 0) > xp_for_reel(1, false, 0));
    }

    #[test]
    fn quiz_grading_returns_explanation() {
        let reel = sample_reel();
        let (correct, explain) = grade_quiz(&reel, 0);
        assert!(correct);
        assert_eq!(explain, reel.quiz.explain);
        let (correct, explain) = grade_quiz(&reel, 1);
        assert!(!correct);
        assert_eq!(explain, reel.quiz.explain);
    }

    #[test]
    fn track_deserializes_kebab_case() {
        assert_eq!(
            serde_json::from_str::<Track>(r#""system-design""#).unwrap(),
            Track::SystemDesign
        );
        assert_eq!(
            serde_json::from_str::<Track>(r#""rust""#).unwrap(),
            Track::Rust
        );
    }
}
