# AGENTS.md

The binding working mode for this repository. Applies to every agent and every model.

## 1) The project in one sentence

AudioLex is a hearing-training app (Android first, iOS an open option) for rebuilding the mapping sound → word → meaning after one-sided hearing loss. Vision and subject-matter concept: `docs/konzept/AudioLex-Konzept.md` (German).

## 2) How work happens

- **Backlog-driven**: order `P0` → `P1` → `P2` → `P3` from `docs/backlog.md`. Do not start anything that is neither in the backlog nor explicitly commissioned.
- **Analysis before plan**: read the existing code and docs before planning or implementing.
- **Milestones instead of sprint dogma**: M0–M9 structure the work; the order may deviate with a reason.
- **ADRs**: every substantial technical decision becomes an ADR in `docs/adr/` (template is there). Proposals beyond the current scope go into the backlog tagged `[PROP]`.
- **Open questions**: items tagged `[KLÄRUNG]` need a decision from the author — do not resolve them on your own, ask.

## 3) Source of truth

| What | Where |
| --- | --- |
| Concept/vision | `docs/konzept/AudioLex-Konzept.md` (German) |
| Stance & tone | `SOUL.md` |
| UI/UX concept | `DESIGN.md` |
| Scenario catalogue (SDD, source of the UI ACs) | `docs/scenarios.md` |
| Architecture | `docs/architecture.md` |
| Decisions | `docs/adr/` |
| Tasks & priorities | `docs/backlog.md` |
| Implementation journal | `docs/implementation-log.md` |
| Claude context | `CLAUDE.md` |

## 4) Definition of done

1. Build green: `./gradlew build` (at minimum `:core:jvmTest` and `:composeApp:assembleDebug`).
2. New logic in `:core` has unit tests.
3. Visible/audible behaviour verified on the desktop target (`./gradlew :composeApp:run`); audio and device matters additionally on the test device (Galaxy A53, Android 16).
4. Backlog item ticked off with a short `Hinweis:` line on the outcome.
5. An entry in `docs/implementation-log.md` (newest first; date, bold title, what + why + how it was verified).
6. **Version bumped (per batch, since 2026-07-13):** `VERSION_NAME` in `composeApp/.../AppVersion.kt` by one minor step (e.g. `0.5.0` → `0.6.0`), `VERSION_CODE` +1. The patch place (`0.5.1`) is reserved for hotfixes. That file is the source of the **displayed** version. Since v0.33.6 `versionCode`/`versionName` also exist **as literals** in `composeApp/build.gradle.kts` — F-Droid's `checkupdates` reads them there by regex and cannot evaluate Gradle; with a variable it finds no version and automatic update detection stays blind (measured against fdroidserver 2.4.5). **Bump both places**; a `check()` assertion in the build fails immediately if they drift apart, so the displayed version can never differ from the built one. Name the new version in the implementation-log entry and in the commit message, so it is clear which version a device test covers. **Bundling (since 2026-07-19):** one batch may bundle several related items into **one** version/commit — the guideline is "one version per *coherent* change", not necessarily one per item. Related work (same subsystem, or a batch of device-test findings) runs as one implementation; only independent chunks (an audio feature versus a UI fix) get their own versions. **Short fixes count too (author's rule 2026-08-07):** if anything changes after the bump — a device-test finding, a correction to one's own work — the **patch** place goes up (`0.26.0` → `0.26.1`), even for two lines. The reason: the displayed version is the only handle on which state is running on the device, and two different builds under one number make every device test unprovable. Better one number too many than one that is ambiguous.
7. Commit with a concise English message.

## 5) Conventions

- **Language**: documentation and commits in **English** (author's decision 2026-08-27 — the testers and the people who look at this app closely are largely English-speaking). Everything was German before that; the documents listed in §3 were translated in one pass, and whatever is still German is marked as German wherever it is referenced. Still German for now: the subject-matter concept under `docs/konzept/`, the root `README.md` (with `README.en.md` as its English counterpart), `docs/fdroid-anmeldung.md` and the implementation-log entries from before the F-Droid submission. Code, identifiers and code comments in English. **UI texts in German and English** (since ADR-0015, 2026-08-24). Both versions rank equally: the text catalogue is a typed interface in `core/i18n` (`Strings` + `GermanStrings`/`EnglishStrings`), and a missing translation is a compile error. New UI text therefore means: extend the interface, fill **both** catalogues. **No exceptions since 2026-08-27** — the dev channel test was one, until an English-speaking tester had to operate it (ADR-0015 addendum). The **corpus** language is a different question from the UI language and has its own setting (ADR-0016): how you read the app and what you train are chosen separately.
- **Texts other people read** (UI texts in **both** languages, commit messages, README, store descriptions): consult the style guardrails **before** writing, not afterwards as a check. The core: write like a specific, competent person short on time — concrete instead of general, no marketing or filler words, no hedging formulas, uneven sentence lengths. Where the `prose-anti-ki` skill is available it is the long form of this and gets loaded; where it is not, the core still applies. This holds for delegated sessions too. Not needed for the control files themselves (this document, CLAUDE.md, SOUL.md, DESIGN.md, configs) nor for the backlog and the implementation log: those are internal working documents, where terse structure and repetition are right.
- **Namespace**: `de.hexenwoche.audiolex`
- **No network or cloud code in phase 1** — data storage strictly local.
- **No new Gradle modules without an ADR**: components grow as packages inside `:core` (srs, audio, corpus, session) first; a module split only on real need.
- **Audio files:** whatever ships **belongs in the repo** (author's decision 2026-08-17 — F-Droid builds from source, and a build server without the WAVs ships a mute app). The condition: redistribution permitted **and** the origin recorded in that folder's README; an entry without an origin line is a release blocker. Third-party licensed material stays out — the author's library `resources/sounds/` is gitignored. This supersedes the earlier rule ("do not commit, versioning strategy open", `[PROP]` Git LFS).
- **Keep the corpus generic**: nothing hearing-loss specific wired into the data model (later use as a vocabulary trainer, concept 3.4).

## 6) Model allocation (a guideline)

- **Opus class**: architecture, ambiguous design questions, ADR drafts, process reflection, sharpening the backlog.
- **Sonnet class**: clearly specified implementation, tests, refactoring.
- **Tagged per open backlog item**: `[→Sonnet]`, `[→Opus]` or `[→Opus→Sonnet]` (sharpen first, then implement); items without a tag need the author. What decides is the degree of specification, not the size of the item: an item without acceptance criteria and non-goals is never ready for Sonnet — Sonnet sessions only implement `[→Sonnet]` items and do not sharpen them themselves.
- **Efficiency with several `[→Sonnet]` items:** work a coherent chain in **one** agent session (read the context once instead of cold per item), build only the DoD minimum per commit (`:core:jvmTest` + `:composeApp:assembleDebug`), and run the full `./gradlew build` once at the end. No full rebuild plus a cold agent restart per tiny item. (Background agents have run unreliably in this environment — delegate synchronously.)

## 7) Environment

- Development on **native Debian** (GNOME on Wayland; this used to say WSL2 — author's correction 2026-08-17), JDK 21, Android SDK under `~/Android/Sdk` (path in `local.properties`, not versioned).
- **Screenshots on the machine** only with `gnome-screenshot -f <file>.png`; `import`/`scrot`/`xwd` return nothing under Wayland. Nothing can be tapped on the desktop (no `xdotool`/`ydotool`) — so for a real interaction path, go to the device: `adb exec-out screencap -p > x.png`, navigate with `adb shell input tap`. **Read the coordinates, do not estimate them** — `adb shell uiautomator dump /sdcard/ui.xml` gives every visible text with exact `bounds`; positions measured off a screenshot are not reliable. And **never tap blindly in learning or exam mode**: exam mode persists every rating immediately (scenario S5) and there is no undo. On 2026-08-24 five cards in the author's real deck were rated that way because a tap landed somewhere other than intended. For layout and text questions the desktop is the right place.
- **Proving a particular screen on the desktop anyway** (2026-08-24, without input injection): temporarily set the initial value of `screen` in `App.kt` to the target (`Screen.Kurzanleitung`, `Screen.Einstellungen`, …), build `:composeApp:desktopJar`, take the screenshot, **revert it** and check with `git status` that nothing is left behind. This does not replace an interaction test — the path *to* the screen stays unverified — but it proves layout and texts, and it finds clipped edges before the device is available.
- **Forcing the UI language on the desktop** without changing the system language: Gradle's `run` forks a JVM with the daemon's `-Duser.language`/`-Duser.country`. Grab the running process's command line (`tr '\0' '\n' < /proc/<pid>/cmdline`), rewrite those two switches to `en`/`US` and start it directly — same build, different language, without restarting Gradle. This is how the English version was proven (ADR-0015).
- Fast iteration through the **desktop target**, not through the emulator.
- Device test: Samsung Galaxy A53 (SM-A536B/DS, Android 16) via adb over Wi-Fi or USB (usbipd).
- Deployment to the device through the `Makefile` in the repo root (`make pair` / `make deploy PORT=...`, details in `make help`); default IP 192.168.178.24, override with `IP=...` on DHCP drift. **The pairing port, the code and the connection port are new every session and come from the author** — they cannot be reused, so ask rather than guess. For your own `adb` calls set `export ANDROID_SERIAL=192.168.178.24:<port>`: `adb devices` also lists mDNS entries of the same device, otherwise you get "more than one device".
- **Reading app data off the device** (the debug build is `debuggable`, so no root needed): `adb exec-out "run-as de.hexenwoche.audiolex cat databases/audiolex.db" > audiolex.db` — **and copy `audiolex.db-wal` along with it**, or you read an empty torso: the main `.db` is about 4 KB, everything current sits in the WAL. Use `adb exec-out` rather than `adb shell` (no line-ending translation on binary data). `files/eigene-aufnahmen/` is reachable the same way. This is how migrations and data states get **proven rather than claimed** — pull before and after, compare with `sqlite3`.
- **Before any destructive device test, pull everything, not just the obvious part.** The backup test (uninstall/reinstall) deleted the session history on 2026-08-07 because only the own corpus had been copied beforehand — the database would have been the same gesture (ADR-0013 addendum). ASHA/Bluetooth audio diagnosis on the device: `adb logcat | grep bluetooth-asha` — the signature `SendStart ... volume=0x80` means a system-side muted hearing-aid volume state (not an app bug), `volume=0x0` means healthy (chain of evidence: implementation log, 2026-07-18, evening).
- **On a scrolling screen, a `uiautomator` dump proves the viewport, not the screen** (learned the hard way 2026-08-28). Both the dump and `screencap` show only what is currently visible. A newly added element at the end of scrolling content sits below the visible area on a screen already scrolled to its previous end — it looks exactly like a composable that failed to recompose. Scroll to the element before concluding it is missing. Two smaller traps from the same session: `adb shell svc power stayon true` before any tap sequence (the screen sleeps otherwise and every following screenshot is black), and aim taps at the **horizontal centre of the row**, not at the text node's centre — a tap on the label of a Compose `selectable` row lands only sometimes.
- **`dumpsys audio` before blaming the app for a channel finding**: `mMasterMono`/`mMainBalance` show accessibility mono and balance, the `Spatial audio:` section shows whether a spatializer is active for the current route, and the per-playback `FormatInfo{isSpatialized=…, channelMask=…}` lines show what the app's own output actually did. A headset with a microphone produces crosstalk on the silenced channel all by itself — the mic picks up the ear that is playing (author's device test 2026-08-27).
