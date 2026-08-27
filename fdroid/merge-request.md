# Text für den Merge Request

Vorlage im GitLab-Formular: **App inclusion**. Ihre fett gesetzten Hinweiszeilen ganz oben werden gelöscht — das steht dort ausdrücklich („Please remove above lines!").

Der Titel ist Vorschrift und muss dem Format „New app: app name" folgen:

```text
New app: AudioLex
```

Zwei Voraussetzungen prüft niemand für dich, sie stehen aber in der Vorlage: Der Fork muss **öffentlich** sein, und der Zweig `de.hexenwoche.audiolex` darf **nicht geschützt** sein — F-Droid merged per Fast-Forward und muss vorher rebasen können.

**Was ins Beschreibungsfeld kommt, steht in `merge-request-body.md` daneben — die Datei ganz kopieren, sonst nichts.** Sie enthält ausschließlich den englischen Text für GitLab: die Vorlage mit gesetzten Haken, die Projektbeschreibung und die Label-Zeile. Diese Datei hier ist die deutsche Handreichung dazu und gehört *nicht* in den Merge Request.

Unten steht dieselbe Fassung noch einmal im Zusammenhang, damit man sie lesen kann, ohne zwei Dateien nebeneinanderzulegen — maßgeblich ist `merge-request-body.md`.

---

## Required

* [x] The app complies with the [inclusion criteria](https://f-droid.org/docs/Inclusion_Policy)
* [x] The original app author has been notified (and does not oppose the inclusion) — I am the author.
* [x] All related fdroiddata and RFP issues have been referenced in this merge request — there are none, this is a direct submission.
* [x] Builds with `fdroid build` and all pipelines pass
* [x] There is an issue tracker and contact info of the author

## Strongly Recommended

* [x] The upstream app source code repo contains the app metadata in a Fastlane folder structure — `fastlane/metadata/android/{de-DE,en-US}/`, including four screenshots and a changelog for versionCode 42.
* [x] Releases are tagged and auto update is enabled — `UpdateCheckMode: Tags`, `AutoUpdateMode: Version`.

## Suggested

* [ ] External repos are added as git submodules instead of srclibs — the app has neither.
* [ ] Enable [Reproducible Builds](https://f-droid.org/docs/Reproducible_Builds) — No, I don't want this.
* [ ] Multiple apks for native code — the app has no native code. The 30 MB are bundled audio (73 WAV files, 4.3 MB compressed assets plus the Compose runtime); splitting by ABI would not change that.

---

AudioLex is a hearing-training app for the case where hearing and understanding
come apart: after one-sided hearing loss the sound arrives, but it is no longer
reliably decoded as speech. Two modes — one shows the word while playing it, the
other keeps it covered and lets you rate yourself; a fixed-interval spaced
repetition scheduler decides when a word returns. Optional background noise at a
settable signal-to-noise ratio. Bilingual German/English interface, switchable on the start screen; the training corpus has its own language setting. Offline only, no internet permission.

I am the author, and this is my first submission to F-Droid. A few things I
checked before opening this, in case it saves a round trip:

- `fdroid lint` is clean and `fdroid rewritemeta` changes nothing.
- `fdroid build -v -l` succeeds locally in the `fdroidserver:buildserver` image
  with fdroidserver 2.4.5. The scanner reports nothing.
- Everything the app ships is redistributable and its origin is documented in the
  README of the respective folder: 68 speech samples generated with Piper using
  the Thorsten voice (model MIT, dataset CC0), four sample recordings by me and a
  second speaker who consented, and one background noise I recorded on a bus. All
  of it CC0-1.0. No third-party audio, no prebuilt binaries beyond the Gradle
  wrapper, which fdroidserver replaces with its own verified Gradle anyway.
- The app was written with substantial help from an AI coding assistant (Claude
  Code). The concept, the decisions and the acceptance are mine. I mention it
  because the repository makes it visible anyway — every significant decision is
  an ADR, every step is in a journal — and because I would rather say it than
  have it found. If that is a problem for inclusion, please tell me directly.

/label ~"New App"

---

## Entschieden: F-Droid signiert (Autor-Entscheid 2026-08-17)

Die Vorlage will das ausdrücklich so notiert haben — Kästchen leer lassen und
„No, I don't want this." dahinterschreiben. Das ist oben bereits eingetragen.

Warum so, damit die Entscheidung später nachvollziehbar ist: Die technische
Voraussetzung wäre erfüllt gewesen. Zwei Builds desselben Tags — einer mit dem
Android-SDK des Entwicklungsrechners, einer im F-Droid-Container mit deren SDK
und deren nachgeladenem Gradle — ergaben ein **byte-identisches** APK
(30 400 285 Bytes, gleicher SHA-256). Dagegen standen drei Dinge: Der Nachweis
stammt von **einer** Maschine, während F-Droid über verschiedene hinweg
vergleicht. Die Zusage gilt nicht einmalig, sondern für jede künftige Version —
reproduziert ein Build nach einem Gradle- oder AGP-Sprung nicht mehr, bleibt das
Update liegen, bis es wieder passt. Und ein eigener Signaturschlüssel muss für
die Lebensdauer der App sicher verwahrt werden; geht er verloren, gibt es keine
Updates mehr.

Der Preis dieser Entscheidung ist benannt und akzeptiert: Sie lässt sich nicht
nachholen. Die Vorlage sagt dazu *„you can't enable it later"*, weil das APK dann
mit F-Droids Schlüssel signiert ist und Nutzer nicht auf eine anders signierte
Fassung aktualisieren können.
