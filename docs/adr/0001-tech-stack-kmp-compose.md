# ADR-0001: Kotlin Multiplatform + Compose Multiplatform

- **Status:** accepted
- **Date:** 2026-07-07

## Context

The concept proposes "native Android (Kotlin, Jetpack Compose)" while also demanding that the Apple option not be ruled out. A native Android build makes iOS a complete rewrite later. On top of that, development runs 100 % agent-driven under WSL2, where an Android emulator is a nuisance — the agent needs a fast, local way to verify behaviour on its own. *(The environment turned out to be native Debian rather than WSL2 — author's correction 2026-08-17; the reasoning is unaffected.)*

## Decision

We use **Kotlin Multiplatform (KMP)** with **Compose Multiplatform (CMP)** as the UI:

- Targets for now: **Android** (primary, minSdk 29 / test device Galaxy A53, Android 16) and **desktop/JVM** (the development and verification target).
- iOS is not enabled, but the cut is laid out for it: logic in `commonMain`, platform access (audio output) behind expect/actual.
- Versions centrally in `gradle/libs.versions.toml` (Kotlin 2.1.x, CMP 1.8.x, AGP 8.7.x, Gradle 8.11).

## Alternatives

- **Native Android (the concept's proposal):** the fastest Android MVP, but iOS = a rewrite and every verification needs a device or emulator. Rejected.
- **Flutter:** one codebase including a Linux desktop, very agent-friendly; but Dart instead of Kotlin (the concept's preference), and the fine-grained audio control (per-channel levels, SNR mixing) would need platform channels of its own. Rejected.

## Consequences

- The agent can build, run tests and start the app on the desktop without an emulator — the shortest feedback loop.
- iOS later: add the targets in the Gradle build plus an `AudioSink` actual with AVAudioEngine; it needs a macOS build host.
- The toolchain is heavier than a pure Android project (the CMP plugin, multi-target configuration); version compatibility Kotlin↔CMP↔AGP has to be watched on upgrades.
- Audio behaviour on the desktop is only an approximation — tests that matter for the hearing aid always happen on the device (see AGENTS.md, definition of done).
