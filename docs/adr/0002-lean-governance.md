# ADR-0002: Lean governance instead of the full wegrose apparatus

- **Status:** accepted
- **Date:** 2026-07-07

## Context

The concept (section 5) carries over the full wegrose governance apparatus: AGENTS.md, CLAUDE.md, RSI.md, schema-validated EXP records (YAML), a PROP system, ADRs, sprint Scrum through GitHub Projects. AudioLex is a solo, non-commercial project with one author; what is asked for is traceability "in the familiar way" (a backlog, an implementation log) — and explicitly no sprint dogma.

## Decision

We use the lean set:

- `AGENTS.md` (the binding working mode) + `CLAUDE.md` (context, without duplication)
- `docs/backlog.md` — P0–P3, a checkbox format with a `Note:` on completion (the wp_service style)
- `docs/implementation-log.md` — a journal, newest first
- `docs/adr/` — decisions
- **Milestones M0–M5** instead of sprints; the order may deviate with a reason.

What replaces the artefacts we drop: RSI insights flow in as log entries or into ADR consequences; EXP records are absorbed by the implementation log; PROP becomes the backlog tag `[PROP]`; an open question needing a decision becomes the tag `[KLÄRUNG]`. The backlog is file-based rather than GitHub Projects (no remote needed, versioned, directly editable by an agent).

## Alternatives

- **The full wegrose apparatus:** proven in a team or client context, but ceremony without an audience here; the maintenance effort competes with product work. Rejected.

## Consequences

- Fewer files, one place per question ("where does what live?" → the AGENTS.md table).
- Should the project gain contributors or move to GitHub Projects, the backlog migrates one to one; EXP/RSI can be split out again if needed (record a superseded note here).
