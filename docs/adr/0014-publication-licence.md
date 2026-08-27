# ADR-0014: The direction of publication, the licence and the asset policy

- **Status:** accepted (the author's decisions 2026-08-07)
- **Date:** 2026-08-07

## Context

On 2026-08-07 the author decided to publish AudioLex — F-Droid before Google Play (M6/M7 in the backlog). F-Droid builds from public source itself and thereby sets three conditions that stood as `[KLÄRUNG]` items in the backlog: who it is published for, under which licence, and what happens to the shipped audio files. Two findings made the questions acute: there is no licence file in the repo ("all rights reserved" rules out inclusion), and an F-Droid build from today's repo would be mute, because the corpus WAVs are gitignored; two of the three noise loops also carry a "non-commercial" clause that F-Droid does not recognise as a free licence.

The author answered all three questions the same day. Important for the classification: **the publication itself is not imminent.** The goal is explicitly the state in which the project and the app are ready to be published — not the publication. Release work is not prioritised; ongoing work should merely not build up new release blockers.

## Decision

1. **Publication for third parties, non-commercial.** AudioLex is made available to users beyond the author's device, without financial interest. That makes a first-use path and self-explanatory texts due in principle — but only once publication becomes concrete (see point 4).

2. **Apache-2.0 for the code.** The licence is F-Droid-compatible and — unlike GPL — does not block the open iOS target. Content (the corpus, the audio) stays explicitly outside the licence and carries its own origin statement; that follows the recommendation of the older licence `[PROP]` item, which is hereby adopted. Applying the LICENSE file is part of the later release work, not of this decision.

3. **No non-free content in a release.** For the audio files concretely:

   - **Background noises:** the three bundled loops (twice salamisound "non-commercial", once Pixabay) are not published along with it. Users bring their own noises — hence the M4 item's name, "record, import and delete your own background noises": direct recording in the app **plus** importing existing WAV files (the author's decision in the second round, 2026-08-07).
   - **The bundled corpus:** stays included as a starter stock; but its WAVs get **generated at build time by TTS** (the local, reproducible Piper pipeline, ADR-0006, `tools/generate_tts.py`) rather than checked in. So the fourth route from the asset item ("publish only the texts, edit them in the app") is **not** chosen — it stays as its own, still blocked backlog item (no runtime voicing settled).
   - **No user corpora/decks** ship. The passing-on idea from the deck `[PROP]` is untouched and unbundled.

4. **"Ready, not published".** Release work (the LICENSE file, a public repository, the tag convention, integrating the TTS generation into the build, the F-Droid records) waits until publication becomes concrete. The criterion for all work until then: do not build up new obstacles to publication.

## Alternatives

The asset question was prepared in the backlog with three routes; the author chose a build variant of route 3:

- **Route 1: audio into the repo.** Rejected — it makes the "non-commercial" licence of the salamisound loops immediately acute; that clause is incompatible with F-Droid.
- **Route 2: ship without a bundled corpus, only the own corpus.** Rejected as the shipped state — the app would start empty, and the author wants to keep a starter stock. (The own-corpus capability is untouched by that and stays central.)
- **Route 3: check in a freely licensed replacement corpus.** Absorbed into the chosen build variant: instead of checking artefacts in, the TTS generation moves into the build. The licence question thereby moves from the WAVs to the Piper voice models (see consequences).
- **A fourth route: edit and voice the corpus texts inside the app.** It stays an independent backlog item. Blocked as long as runtime voicing is unsettled — not part of this decision.

## Consequences

- **The shipped state of the background noises is "no bundled loops".** That affects the sharpening of the M4 noise item directly: `noise.json` today references three WAVs that are missing in a release build — the catalogue and the noise section have to work with exclusively self-recorded noises (or none at all). That is the published normal case, not an edge state.
- **TTS in the build requires two clarifications at release time:** the licence of the Piper voice models used (today thorsten-medium) and integrating the generation into the build/F-Droid path (F-Droid builds from source — the generation has to run there, or the route to it has to be documented).
- **Third parties as the audience** eventually makes a first-use path, an explanation of the training and error-reporting texts due (the product item in M6) — tied to the moment "publication becomes concrete", not to now.
- **Non-commercial** keeps M7 (Google Play) small: the developer account, the testing obligations and the fees stay weighed against the F-Droid route (the `[KLÄRUNG]` item there).
- **The repo stays private** until publication becomes concrete; the state "all rights reserved" is intended until then and not an oversight.
- **The evidence obligation remains:** F-Droid's requirements themselves are still not proven against the current F-Droid documentation (the M6 FOSS item does that while sharpening) — this ADR records the author's decisions, not external facts.

## Addendum 2026-08-17: point 3 (background noises) is implemented — and three proven findings about the F-Droid route

On 2026-08-17 the author commissioned removing the licence-bound background noises and working out the route to inclusion in F-Droid. That starts a piece of the release work this ADR had put under point 4 as "waits until publication becomes concrete". The order stays the ADR's: first the state "ready"; the publication itself is not commissioned.

**Implemented (v0.32.0):** the three loops are removed from the project, along with `noise.json`, the licence table and the bundled loading in the code (ADR-0010 addendum). Evidence rather than a claim: in the built APK there are **0** files under `files/noise/` and 71 resource files (the corpus). A side finding from the same check worth recording: an *incremental* build would have kept shipping the loops — `copyDebugComposeResourcesToAndroidAssets` does not clean up removed resources, and the files were still in `build/intermediates/assets/` after the deletion. After removing a Compose resource, a `clean` for that module is mandatory, or you are checking the past.

**Three findings, proven against the current F-Droid documentation** (the evidence obligation from the last paragraph of this ADR is thereby met for these points; the details, sources and sequence of steps are in `docs/fdroid-anmeldung.md`):

1. **The licence of the Piper voice is uncritical.** The model `de_DE-thorsten-medium` is under MIT, and the underlying dataset (Thorsten-Voice) under CC0 — the open clarification from this ADR's consequences is answered. The generated corpus WAVs are therefore freely redistributable, and F-Droid's inclusion policy allows the larger latitude for non-functional assets anyway, while explicitly requiring the right to redistribute.
2. **"TTS in the build" is the risky part of the decision, not the safe one.** F-Droid permits prebuilt binary parts only from a fixed list of sources (Debian main, Maven Central/Google/Sonatype/JFrog/JitPack/Clojars, PyPI wheels, Rust/Go/Node); a 63 MB voice model from Hugging Face is not on it. The expectation "the generation runs on the build server" does not carry in that form. The practicable route is the one this ADR describes under route 3 as the checked-in variant and had rejected: generate the WAVs locally and check them in — they are CC0/MIT-derived, so exactly the unproblematic content this was about. **That is a new author decision** and noted in the backlog as a `[KLÄRUNG]`, not pre-empted here.
3. **A blocker in the build that was unknown until now.** `settings.gradle.kts` loads the plugin `org.gradle.toolchains.foojay-resolver-convention` to obtain a JDK. F-Droid's scanner lists exactly this plugin as a "usual suspect" and aborts on it — it downloads a Java runtime from an uncontrolled source. That belongs out of the build before submitting, not into a `scanignore`.

## Addendum 2026-08-17 (the second): point 3 is corrected — shipped audio lives in the repository

The author commissioned the deployment preparation and made two decisions along the way that change point 3 of this ADR:

1. **"We work with our own WAVs."** The shipped audio files get **checked in**, not generated at build time. That settles the `[KLÄRUNG]` from the first addendum, and against the build variant: the Piper voice model (63 MB, Hugging Face) is not a binary source F-Droid permits, whereas the generated WAVs are unproblematic (the model MIT, the dataset CC0). Implemented: 68 corpus WAVs in the index, 3.6 MB, the `.gitignore` exception dropped, the origin in `files/corpus/README.md` as a mandatory statement. The old `[PROP]` Git LFS item is thereby moot. The author's own recordings and further voices (Grete) are to be added as examples — the format and the three conditions (consent, format, two JSON entries) are in the same README.
2. **One background noise ships**, contrary to the first addendum: the author's bus recording. The problem was never the bundling but the foreign licence. The rule is now "only content that may be redistributed", not "nothing" (ADR-0010 second addendum).

**What is also done with this addendum** (v0.33.0): the LICENSE file applied (Apache-2.0, Debian's canonical text, only the copyright line filled), the toolchain-provisioning plugin removed from `settings.gradle.kts`, the imprint sentence "without public distribution" corrected, the README rewritten for strangers including a content licence table, the store metadata created in the fastlane layout (de-DE + en-US, a 512×512 icon rasterised from the vector paths), and a copy of the recipe under `fdroid/`.

**Proven at the artefact, not claimed:** the index was exported into an empty directory (`git checkout-index --prefix`) and built there without `local.properties` and without the author library — that is the build server's situation. `assembleRelease` runs through, and the APK carries 68 corpus WAVs and none of the old loops. Two findings along the way:

- **The scanner reads comments too.** After removing the plugin, its identifier still stood in the explanatory comment in the same file. F-Droid searches for it as text in Gradle files — the comment would have triggered the same abort as the plugin. The identifier now only appears in the documentation, which is not a Gradle file.
- **Dropping the toolchain plugin carries locally.** `jvmToolchain(21)` resolves against the installed JDK 21; `./gradlew build` stays green, and the argument from back then ("the host only has a JRE") is superseded.

**One licence question stays open that only the author can answer:** under which licence his own recordings (the bus sound, the human recordings) are passed on. For non-functional assets F-Droid explicitly requires the right to redistribute, so a statement is needed. The proposal in the README: CC0-1.0. Until the decision it says "still to be decided" there — visibly, not silently.
