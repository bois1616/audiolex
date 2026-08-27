# SOUL — the soul of the AudioLex project

This document records what AudioLex stands for and how the app talks to its user, independently of any particular feature. It complements `AGENTS.md` (working mode) and `docs/konzept/AudioLex-Konzept.md` (subject-matter concept, German); where the two conflict, the concept wins.

## What this is

A hearing-training app that rebuilds the mapping *sound → word → meaning*. The problem is neurological, not acoustic: the sound arrives through the hearing aid, but the brain does not (yet) decode it as speech. AudioLex trains that decoding — systematically, repeatedly, gradually harder.

## Who the user is

Stephan himself: roughly 80 % one-sided hearing loss, hearing-aid wearer. He is the author, the client, and in phase 1 the only user. That settles the roles: **the user is the authority on his own hearing.** The app supplies structure, repetition and difficulty control — it does not judge what he "ought to be able to hear".

## Stance — not negotiable

**A training device, not a medical product.** No promises of cure, no diagnoses, no therapy-speak. The app schedules repetitions of recognition exercises; it claims nothing beyond that.

**Self-rating without judgement.** In exam mode the user rates himself. The scale (Again/Soon/Later/Good/Perfect) describes *when the word comes back* — not success or failure. There is no "wrong", no red, no error rate held up as an accusation.

**Patience is built in.** Neurological rebuilding takes months. So: no streak pressure, no gamification tricks, no guilt after a break. Spaced repetition *is* the patience — the app does not have to stage it.

**Private means private.** Hearing performance is health data. Everything stays local on the device: no account, no cloud, no telemetry (AGENTS.md §5, concept 4.5).

**The trained ear governs the app.** The reference setup is the Bluetooth hearing aid on the left ear (ADR-0007). Every audio feature is thought through from that question: does the signal arrive there intelligibly and at the right level? Channel separation (left/right/both) remains available as a tool for alternative setups, but it does not set the pace.

## Tone of the UI texts

- German and English, clear, adult. Short sentences.
- Matter-of-fact and friendly — never childish, never sympathetic hand-wringing, never a motivational poster.
- Error messages say what to do ("No audio output found — is the hearing aid connected?") rather than only what went wrong.
- Progress is reported soberly (cards due, words completed), neither celebrated nor held against the user.

## What AudioLex is not

- Not a commercial product: non-commercial, no hardening for hostile use (concept, header).
- Not a substitute for ENT or audiology.
- Not a language-learning app with a subscription, ads or leaderboards — even though the corpus architecture allows a vocabulary trainer later (concept 3.4), the soul stays the same: concentrated hearing training.

## The voice in one sentence

AudioLex speaks like a dependable training partner: says plainly what is next, waits patiently, does not judge.
