# ADR-0005: SRS — a fixed interval table behind a scheduler interface

- **Status:** accepted (for the MVP)
- **Date:** 2026-07-07

## Context

Exam mode steers repetitions by spaced-repetition logic. The concept names a five-level manual rating with fixed intervals (Again 1 min, Soon 10 min, Later 1 day, Good 1 week, Perfect 1 month) and leaves open whether an adaptive method (real SM-2, FSRS) with automatic derivation of the rating follows later.

## Decision

For the MVP: a **fixed interval table, rated manually** — implemented as `FixedIntervalScheduler` behind the interface `ReviewScheduler` (`:core`, package `srs`). The rating picks the interval directly; no ease factor, no card state beyond `repetitions`. Comprehensibility beats optimality: the user (= the author) should understand the system's behaviour immediately.

## Alternatives

- **Full SM-2 (ease factor, adaptive intervals):** better with large card counts, but less transparent; unnecessary for a corpus in the hundreds of words. Possible later.
- **FSRS:** the most modern approach, and it needs review history as training data — exactly what we collect with the MVP anyway. A candidate for later, as a `[PROP]` in the backlog.

## Consequences

- Changing the algorithm is a pure implementation swap behind `ReviewScheduler`; the data model stores raw data (the rating and the timestamp), not just the next due date, so that later methods can use the history.
- Automatic derivation of the rating (reaction time/repetition count) deliberately stays out — a backlog `[PROP]`.
