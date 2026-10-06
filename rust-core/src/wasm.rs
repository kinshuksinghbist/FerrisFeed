//! Web/WASM bindings behind the `wasm` feature.
//!
//! The web preview calls these from JS after `wasm-pack build`:
//! JSON goes in, JSON comes out, so no TS-side type gymnastics are needed.

use crate::Reel;
use wasm_bindgen::prelude::*;

/// Validate a reel given as JSON; returns `{"ok":true}` or
/// `{"ok":false,"error":"..."}`.
#[wasm_bindgen]
pub fn validate_reel_json(reel_json: &str) -> String {
    crate::validate_reel_json_inner(reel_json)
}

/// Grade a quiz given the reel as JSON; returns
/// `{"correct":bool,"explanation":"..."}` or `{"error":"..."}`.
#[wasm_bindgen]
pub fn grade_quiz_json(reel_json: &str, chosen: u32) -> String {
    crate::grade_quiz_json_inner(reel_json, chosen as usize)
}

/// Return the quiz options of a reel as a real JS array (via
/// `serde-wasm-bindgen`), or `null` when the JSON does not parse.
#[wasm_bindgen]
pub fn reel_quiz_options(reel_json: &str) -> JsValue {
    match serde_json::from_str::<Reel>(reel_json) {
        Ok(reel) => serde_wasm_bindgen::to_value(&reel.quiz.options).unwrap_or(JsValue::NULL),
        Err(_) => JsValue::NULL,
    }
}
