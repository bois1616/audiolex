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
settable signal-to-noise ratio. German UI, offline only, no internet permission.

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
