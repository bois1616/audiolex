# ADR-0015: A German and English UI, switchable on the start screen

- **Status:** accepted (the author's commission 2026-08-24) · **the channel-test exemption withdrawn 2026-08-27, see the addendum**
- **Date:** 2026-08-24

> **Addendum 2026-08-27: the channel test now speaks both languages.**
>
> Under "alternatives" below stands the reason the channel test was allowed to stay German: an instrument, reachable only through a long press on the version line that nobody finds by accident. That reasoning carried exactly as long as the only person picking the instrument up reads German.
>
> On 2026-08-27 the F-Droid tester chivalry reported that the channel selection in the settings has no effect for him — with a wired headset, and the author cannot reproduce it on his hardware. That makes the channel test no longer an internal tool but **the piece of evidence** an outside tester has to operate: it bypasses the setup detection, sets one channel to exact zero, and is therefore the only thing that answers whether the app puts the sound where it claims. A German instrument in English hands answers nothing.
>
> Changed: the channel test's texts live in both catalogues like every other text (`channelTest*` in `Strings.kt`). **Not** changed: the way in. It stays the long press on the version line — being an instrument is decided by who finds it, not by which language it speaks.
>
> The **corpus** language is untouched by this: which words the test plays is still decided by the training language in the settings (ADR-0016), not by the language of the interface.

## Context

Until v0.33.6 the app was German only, and that was explicitly decided: AGENTS.md §5 ("UI texts in German"), DESIGN.md under "visual principles", and noted three times in the backlog as a non-goal — at language arc batches A and B and at the imprint page. The reason was good: the app is built for a single user, and a second language nobody reads is ballast that wants maintaining on every text change.

Since 17 August the app has been with F-Droid. That makes the premise untrue. The store entry already exists in two languages (`fastlane/metadata/android/{de-DE,en-US}/`), but the app behind it does not — whoever reads the English description and installs it lands in a German interface.

On 2026-08-24 the author commissioned two things: localisation into English with a language choice on the entry screen, and a short guide, likewise reachable from the main page and following the selected language.

The technical constraint that determined the solution: Compose Multiplatform 1.8.2 resolves the language of Compose Resources from the platform locale and offers no supported way to override it at runtime. A language choice *inside* the app would therefore have worked against the framework.

## Decision

**1. The text catalogue is a typed Kotlin interface, not a resource file.** `core/i18n/Strings.kt` declares every text as a `val` or `fun`; `GermanStrings` and `EnglishStrings` implement it. A missing translation is thereby a compile error, not an empty label on the device. That is the point where this solution differs from `strings.xml` — and with two versions in one person's hands, exactly that guarantee is worth more than the tooling around it.

**2. The catalogue lives in `:core`, not in `:composeApp`.** That is the unclean spot in this decision: UI wording in a module described as "platform-free logic". What decided it was testability — `:composeApp` has no test source set, and `:core:jvmTest` runs in every DoD loop. The catalogue depends on nothing from Compose; it consists of strings and `when` mappings over domain enums, and those mappings are the part that deserves a test.

**3. The language is a persisted setting with `SYSTEM` as the default.** `UiLanguage { SYSTEM, DEUTSCH, ENGLISCH }` follows the `ThemeMode` pattern. `SYSTEM` resolves through the device's primary language subtag: `de`, `de-AT`, `de_DE` give German, everything else English. That makes the update invisible on a German device — the column arrives as `'SYSTEM'`, the device says `de`, nothing changes — and an English device gets English without anyone setting anything. DB schema v8 → v9, carried by `MIGRATION_8_9` (`ALTER TABLE ... ADD COLUMN`), not by the destructive fallback.

**4. The choice sits on the start screen, not in the settings.** Two text buttons, "Deutsch" and "English", each written in its own language, the active one in the accent colour. The reason is more compelling than convenience: whoever cannot read the current language must not have to guess which of five German buttons opens the settings. It still sits at the bottom in the quiet zone, not above the training buttons — it is touched once per installation, and the start screen's job is to lead to the first word in two taps (DESIGN.md guiding principle 6).

`SYSTEM` is deliberately **not** a selectable option. It is the initial state, not a choice worth offering; a third button would invite thinking about locale inheritance on a training start screen. Which language `SYSTEM` currently gives is shown by the accent colour anyway. A tap always writes a concrete language: whoever has said what they want should not additionally be overruled by the device.

**5. The quick guide is a screen, not a pointer to the README.** It explains the two modes, the five rating levels and what the settings do — and it follows the language choice. The README is German and written for someone who *builds* the app, not for someone who uses it.

**6. The vocabulary stays German.** What is translated is the interface, not the training material. The corpus language is a separate, deferred question (the backlog "language arc", `Word.language`); `UiLanguage` and `Word.language` must not grow together, or you could no longer practise in English what you operate in German later.

## Alternatives

**Compose Resources (`composeResources/values/strings.xml` + `values-en/`).** The obvious tool, and in a pure Android app the right answer. Two reasons against: in 1.8.2 there is no supported way to override the locale at runtime — the required in-app language choice would have come down to `Locale.setDefault` tricks that break differently per platform. And a missing translation only shows up on the device. With 150 texts in two versions and no translation team, the compiler is the better control.

**English only, drop German.** It halves the maintenance. Rejected: the main user is the author, the training material is German, and the imprint texts are worded for German law.

**Follow the system language, without a choice.** It would be the smallest solution and, without the choice, would have had no persistence or migration effort either. The author explicitly asked for the choice; it is also the only thing that helps on a device whose system language you do not want to change.

**Translate everything, the dev channel test included.** Rejected: the channel test is an instrument, reachable only through a long press on the version line that nobody finds by accident. Its wording stays German; only the way back uses shared UI and follows the language. — **Withdrawn on 2026-08-27** (the addendum above): as soon as an outside tester has to operate the instrument, the language stops being cosmetic.

## Consequences

**Easier:** two versions cannot drift apart without the build noticing. Switching takes effect immediately and without a restart, because a `CompositionLocal` gets swapped rather than the platform locale. A third language would be one file and one enum entry — the compiler enumerates the missing texts.

**Harder:** every new UI text now costs three lines in three places instead of one in the screen. That is the price of the compiler check and it is intended, but it is friction.

**Deliberate debt:** the interface's wording lives in `:core`. Should `:composeApp` ever get a test source set, the catalogue belongs moved there; until then this is the price of tests in the fast loop.

**Two rules are superseded.** AGENTS.md §5 and DESIGN.md said "UI texts in German". They are changed to "German and English, both versions maintained as equals". The three non-goal notes in the backlog still apply to the *corpus* language but no longer to the UI language; they are annotated accordingly.
