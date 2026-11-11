package com.ferrisfeed.data

/**
 * The curriculum map (redesign v3): every seeded reel's `topic_label` is one
 * of the topic keys below, and topics are grouped into ordered units per
 * track. This single object drives
 * - first-run feed ordering (fresh reels play in unit -> topic order),
 * - the Path screen (units with their topics),
 * - human titles everywhere a topic is named (feed meta line, recap, profile).
 *
 * Pure Kotlin, no Android types, so JVM tests can pin it. Topic keys are the
 * exact strings stored in the `topic` column, so `Route.TopicFeed(key)`,
 * search filters and mastery all address the same object. Unknown keys (an
 * old pack, a weekly sync pack) still work: they sort last and get a
 * title-cased fallback name.
 */
object Curriculum {

    const val RUST = "rust"
    const val SYSTEM_DESIGN = "system-design"

    val units: List<CurriculumUnit> = listOf(
        // ---- Rust ----
        CurriculumUnit(
            id = "rust-1", track = RUST, number = 1, title = "Getting started",
            blurb = "Install, build, and write your first real Rust.",
            topics = listOf(
                CurriculumTopic("cargo-toolchain", "Cargo & toolchain", "rustup, cargo and editions"),
                CurriculumTopic("variables-types", "Variables & types", "let, mut, shadowing, scalars"),
                CurriculumTopic("control-flow", "Control flow", "if, loops, ranges, expressions"),
                CurriculumTopic("dev-workflow", "Everyday tooling", "dbg!, rustfmt, clippy, errors"),
            ),
        ),
        CurriculumUnit(
            id = "rust-2", track = RUST, number = 2, title = "Ownership & borrowing",
            blurb = "The one idea that makes Rust Rust.",
            topics = listOf(
                CurriculumTopic("ownership", "Ownership & moves", "drop, move, Copy, clone"),
                CurriculumTopic("borrowing", "Borrowing rules", "& and &mut, one writer"),
                CurriculumTopic("slices-strings", "Slices & strings", "&str vs String, views"),
            ),
        ),
        CurriculumUnit(
            id = "rust-3", track = RUST, number = 3, title = "Modeling data",
            blurb = "Structs, enums and the end of null.",
            topics = listOf(
                CurriculumTopic("structs-methods", "Structs & methods", "data, receivers, derive"),
                CurriculumTopic("enums-matching", "Enums & matching", "match, if let, let-else"),
                CurriculumTopic("option-result", "Option & Result", "combinators and the ? operator"),
                CurriculumTopic("collections", "Collections", "Vec, HashMap, entry API"),
            ),
        ),
        CurriculumUnit(
            id = "rust-4", track = RUST, number = 4, title = "Errors, modules & tests",
            blurb = "Code that fails well and scales past one file.",
            topics = listOf(
                CurriculumTopic("error-handling", "Error handling", "panic vs Result, typed errors"),
                CurriculumTopic("modules-crates", "Modules & crates", "privacy, workspaces, features"),
                CurriculumTopic("testing-docs", "Testing & docs", "tests, doc tests, benches"),
            ),
        ),
        CurriculumUnit(
            id = "rust-5", track = RUST, number = 5, title = "Traits & abstractions",
            blurb = "Zero-cost generics, closures and smart pointers.",
            topics = listOf(
                CurriculumTopic("traits-generics", "Traits & generics", "bounds, impl Trait, From"),
                CurriculumTopic("lifetimes", "Lifetimes", "annotations and elision"),
                CurriculumTopic("closures-iterators", "Closures & iterators", "captures, lazy pipelines"),
                CurriculumTopic("smart-pointers", "Smart pointers", "Box, Rc, RefCell, Cow"),
                CurriculumTopic("macros", "Macros", "macro_rules!, derive, proc"),
            ),
        ),
        CurriculumUnit(
            id = "rust-6", track = RUST, number = 6, title = "Concurrency & async",
            blurb = "Fearless threads, then Tokio in production.",
            topics = listOf(
                CurriculumTopic("threads", "Threads & shared state", "Arc, Mutex, channels, atomics"),
                CurriculumTopic("async-basics", "Async fundamentals", "futures, join!, select!"),
                CurriculumTopic("async-production", "Async in production", "cancellation, backpressure"),
            ),
        ),
        CurriculumUnit(
            id = "rust-7", track = RUST, number = 7, title = "Expert Rust",
            blurb = "Type tricks, unsafe, and squeezing out speed.",
            topics = listOf(
                CurriculumTopic("advanced-types", "Advanced types", "dyn, GATs, typestate, variance"),
                CurriculumTopic("unsafe-ffi", "Unsafe & FFI", "raw pointers, Pin, Miri"),
                CurriculumTopic("performance", "Performance & builds", "profiling, allocators, LTO"),
            ),
        ),
        // ---- System design ----
        CurriculumUnit(
            id = "sd-1", track = SYSTEM_DESIGN, number = 1, title = "Foundations",
            blurb = "Protocols, APIs and the numbers that matter.",
            topics = listOf(
                CurriculumTopic("apis-protocols", "APIs & protocols", "HTTP, REST, gRPC, pagination"),
                CurriculumTopic("core-concepts", "Core concepts", "latency, CAP, estimation"),
            ),
        ),
        CurriculumUnit(
            id = "sd-2", track = SYSTEM_DESIGN, number = 2, title = "Data",
            blurb = "Where state lives and how it survives.",
            topics = listOf(
                CurriculumTopic("databases", "Databases & indexes", "SQL vs NoSQL, query plans"),
                CurriculumTopic("storage-engines", "Storage engines", "LSM, B-tree, WAL"),
                CurriculumTopic("replication-sharding", "Replication & sharding", "quorums, hashing, lag"),
            ),
        ),
        CurriculumUnit(
            id = "sd-3", track = SYSTEM_DESIGN, number = 3, title = "Caching",
            blurb = "The cheapest request is the one you skip.",
            topics = listOf(
                CurriculumTopic("caching", "Caching", "cache-aside, CDNs, stampedes"),
            ),
        ),
        CurriculumUnit(
            id = "sd-4", track = SYSTEM_DESIGN, number = 4, title = "Scale & reliability",
            blurb = "Stay up when traffic and failures arrive.",
            topics = listOf(
                CurriculumTopic("load-balancing", "Load balancing", "algorithms and health checks"),
                CurriculumTopic("rate-limiting", "Rate limiting", "token buckets, load shedding"),
                CurriculumTopic("resilience", "Resilience patterns", "timeouts, retries, breakers"),
                CurriculumTopic("deploy-observe", "Deploys & observability", "canaries, SLOs, probes"),
            ),
        ),
        CurriculumUnit(
            id = "sd-5", track = SYSTEM_DESIGN, number = 5, title = "Distributed systems",
            blurb = "Logs, consensus and agreeing on the truth.",
            topics = listOf(
                CurriculumTopic("messaging-events", "Queues & event streams", "Kafka, outbox, DLQs"),
                CurriculumTopic("distributed-consistency", "Consensus & coordination", "Raft, sagas, CRDTs"),
            ),
        ),
        CurriculumUnit(
            id = "sd-6", track = SYSTEM_DESIGN, number = 6, title = "Rust backends",
            blurb = "Production services with Axum and Tokio.",
            topics = listOf(
                CurriculumTopic("rust-backend", "Building with Axum", "routing, tower, sqlx, auth"),
            ),
        ),
        CurriculumUnit(
            id = "sd-7", track = SYSTEM_DESIGN, number = 7, title = "Case studies",
            blurb = "Whole-product designs, interview style.",
            topics = listOf(
                CurriculumTopic("case-studies", "Design case studies", "shorteners, feeds, chat"),
            ),
        ),
    )

    private val topicIndex: Map<String, TopicPosition> = buildMap {
        var global = 0
        units.forEachIndexed { unitIdx, unit ->
            unit.topics.forEachIndexed { topicIdx, topic ->
                put(topic.key, TopicPosition(unit, unitIdx, topicIdx, global++))
            }
        }
    }

    fun unitsFor(track: String): List<CurriculumUnit> = units.filter { it.track == track }

    fun topic(key: String): CurriculumTopic? = topicIndex[key]?.unit?.topics?.firstOrNull { it.key == key }

    fun unitOf(key: String): CurriculumUnit? = topicIndex[key]?.unit

    /** Global curriculum position; unknown topics sort after every known one. */
    fun orderOf(key: String): Int = topicIndex[key]?.global ?: UNKNOWN_ORDER

    /** Index of the topic's unit within its own track (0-based); unknown = large. */
    fun unitIndexInTrack(key: String): Int {
        val unit = topicIndex[key]?.unit ?: return UNKNOWN_ORDER
        return unitsFor(unit.track).indexOf(unit)
    }

    /** Human title for any stored topic string, known or not. */
    fun titleOf(key: String): String = topic(key)?.title ?: prettify(key)

    /**
     * Mastery is persisted under a sanitized key (see ProgressStore). This maps
     * a sanitized key back to the real topic key so taps on mastery rows open
     * the right topic feed instead of an empty one.
     */
    fun topicForMasteryKey(sanitized: String): String? =
        topicIndex.keys.firstOrNull { sanitize(it) == sanitized }

    fun sanitize(key: String): String = key.lowercase().replace(SANITIZE, "_")

    fun prettify(key: String): String = key
        .split(' ', '_', '-')
        .filter { it.isNotBlank() }
        .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
        .ifBlank { key }

    const val UNKNOWN_ORDER = 9_999

    private val SANITIZE = "[^a-z0-9]+".toRegex()

    private data class TopicPosition(
        val unit: CurriculumUnit,
        val unitIdx: Int,
        val topicIdx: Int,
        val global: Int,
    )
}

data class CurriculumUnit(
    val id: String,
    val track: String,
    val number: Int,
    val title: String,
    val blurb: String,
    val topics: List<CurriculumTopic>,
)

data class CurriculumTopic(
    val key: String,
    val title: String,
    val blurb: String,
)
