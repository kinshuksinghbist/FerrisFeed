//! UniFFI-facing shims for Android (Kotlin/Swift).
//!
//! The functions here use only FFI-safe types (`String`, `u32`, `Vec`,
//! one `Object`) and delegate to the pure logic in the crate root, so the
//! Kotlin side never has to deal with `chrono` or `usize` directly.

use crate::{DagError, TopicGraph};
use std::collections::HashSet;
use std::fmt;
use std::sync::{Arc, Mutex};

/// Errors crossing the UniFFI boundary. Payloads are plain strings so
/// every bound language can represent them.
#[derive(Debug, uniffi::Error)]
pub enum FfiError {
    InvalidInput { message: String },
    CycleDetected,
}

impl fmt::Display for FfiError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            FfiError::InvalidInput { message } => write!(f, "invalid input: {message}"),
            FfiError::CycleDetected => write!(f, "topic graph contains a cycle"),
        }
    }
}

impl std::error::Error for FfiError {}

impl From<DagError> for FfiError {
    fn from(_: DagError) -> Self {
        FfiError::CycleDetected
    }
}

/// Validate a reel given as JSON; returns `{"ok":true}` or
/// `{"ok":false,"error":"..."}`.
#[uniffi::export]
pub fn validate_reel_json(reel_json: String) -> String {
    crate::validate_reel_json_inner(&reel_json)
}

/// Grade a quiz given the reel as JSON; returns
/// `{"correct":bool,"explanation":"..."}` or `{"error":"..."}`.
#[uniffi::export]
pub fn grade_quiz_json(reel_json: String, chosen: u32) -> String {
    crate::grade_quiz_json_inner(&reel_json, chosen as usize)
}

/// XP for finishing one reel. Thin FFI wrapper over [`crate::xp_for_reel`].
#[uniffi::export]
pub fn xp_for_reel(level: u8, quiz_correct: bool, streak_days: u32) -> u32 {
    crate::xp_for_reel(level, quiz_correct, streak_days)
}

/// UniFFI object wrapping [`TopicGraph`] behind a mutex so bound
/// languages can build and query the prerequisite graph.
#[derive(uniffi::Object)]
pub struct TopicGraphFfi {
    inner: Mutex<TopicGraph>,
}

#[uniffi::export]
impl TopicGraphFfi {
    #[uniffi::constructor]
    pub fn new() -> Arc<Self> {
        Arc::new(Self {
            inner: Mutex::new(TopicGraph::new()),
        })
    }

    pub fn add_topic(&self, id: String) {
        self.inner.lock().expect("graph lock").add_topic(&id);
    }

    pub fn add_edge(&self, prereq: String, next: String) {
        self.inner
            .lock()
            .expect("graph lock")
            .add_edge(&prereq, &next);
    }

    pub fn topo_order(&self) -> Result<Vec<String>, FfiError> {
        self.inner
            .lock()
            .expect("graph lock")
            .topo_order()
            .map_err(FfiError::from)
    }

    pub fn unlocked(&self, completed: Vec<String>) -> Vec<String> {
        let done: HashSet<String> = completed.into_iter().collect();
        self.inner.lock().expect("graph lock").unlocked(&done)
    }
}
