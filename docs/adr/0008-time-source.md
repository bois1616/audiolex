# ADR-0008: The time source for SRS due dates — an injectable `Clock` interface

- **Status:** accepted (author's decision 2026-07-10)
- **Date:** 2026-07-10

## Context

The SRS logic (`ReviewScheduler.review`, `ReviewQueue.due`, `ExamSession.rate`) computes with a `nowEpochMillis: Long` supplied by the caller — deliberately so, to keep `:core` free of a date library and the logic deterministically testable (the ADR-0003 line, AGENTS.md §5). While building the exam-mode screen, the Opus review of 2026-07-10 noticed that there is **no** productive "now" time source anywhere in the project: the screen hard-coded `nowEpochMillis = 0L` because nothing else was available. The consequence — the SRS core function was effectively dead (every rated card got a due date in 1970 and never became due again; from the second session on, exam mode permanently showed "nothing due"). No unit test caught it, because the `:core` classes are tested correctly in isolation with sensible `now` values — the bug lived only at the UI integration point.

Two forces compel the decision: (1) `:core` should not read the system time directly from common code (there is no platform-neutral `currentTimeMillis` in `commonMain`, and an outside dependency would be a deliberate break with the project's dependency asceticism). (2) The time source has to be **injectable** — exactly what was missing when the bug arose: a testable clock would have allowed an integration test ("rate a card → due again with the clock advanced, not due with it unchanged") that would have caught the regression.

## Decision

We introduce a narrow, platform-free `Clock` interface in `:core`:

```kotlin
// core/commonMain, package e.g. de.hexenwoche.audiolex.core.time
interface Clock {
    fun nowEpochMillis(): Long
}

expect fun systemClock(): Clock
```

The `actual` implementations supply the system time at the thin platform boundary (analogously to `createAudioSink`): `jvmMain` and `androidMain` each with an `actual fun systemClock(): Clock = ... System.currentTimeMillis() ...`. The `Clock` is created by the app — like the `ReviewCardRepository` and the `AudioLexDatabase` — and passed through `App()` to the screens that need it; the screens call `clock.nowEpochMillis()` instead of a constant. In tests a `FakeClock` (with a settable `var now`) is injected.

The key point: **the `Clock` *interface* is the injectable abstraction, and `systemClock()` is only the one platform-specific factory.** A plain `expect fun nowEpochMillis(): Long` would be simpler but precisely **not** injectable (a static call) — it would not solve the testing problem that triggered this ADR.

## Alternatives

- **A plain `expect fun nowEpochMillis(): Long`** (without an interface): minimal, and it continues the `createAudioSink` shape, but it is not injectable — tests of the call site would still have to fake the time through a detour. Rejected, because the missing testability was the actual cause of the bug.
- **`kotlinx-datetime`** (`Clock.System.now().toEpochMilliseconds()`): well maintained, KMP-native, but it brings an outside dependency and a `Clock` concept that is oversized for the MVP's need (one `Long`). It stays a candidate should real date/time-zone logic arrive later (a session history with local dates, S12) — then `systemClock()`'s `actual` can switch to it internally without changing the callers.
- **`kotlin.time.Clock`** (stdlib): experimental at Kotlin 2.1.21, **stable only from Kotlin 2.3** (not 2.2.x — research 2026-07-10, KT-80778). A Kotlin bump as a means to a time source is not worth it: it drags KSP (whose version equals the Kotlin version), the Compose compiler and Compose Multiplatform along synchronously (a real regression risk, especially Skiko) and would bring the *stable* `Clock` only with 2.3 anyway. The version jump is recorded as its own `[PROP]` backlog item, decoupled from this ADR. As soon as the project moves to 2.3, `systemClock()`'s `actual` switches internally to `kotlin.time.Clock.System` — the callers notice nothing, which is exactly what our own abstraction is for.

## Consequences

- The SRS call site becomes testable: an integration test with `FakeClock` guards against the regression this bug was — which was impossible before.
- Two more trivial `actual` files (jvm + android), consistent with the existing `AudioSink` structure; no new Gradle module, no outside dependency.
- `:core` stays free of a date library; switching to `kotlinx-datetime` or `kotlin.time.Clock` is later a pure swap behind `systemClock()`, without touching the callers.
- `Clock` deliberately supplies only UTC epoch millis, no time-zone or calendar logic — and that is sufficient for SRS due dates, because only **time differences** count there (how long since the card was last shown), not absolute accuracy or local calendar dates (author's decision 2026-07-10). For later **reporting** (session history S12, a local date and time) the clean route is to write the time zone as an additional field when storing a session timestamp and to convert only for display — the zone-free millis arithmetic is untouched by that. That is a matter for the session-history item (noted there), not for this time source; `kotlinx-datetime` gets reassessed there should the conversion need more than trivial formatting.
