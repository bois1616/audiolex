# ADR-0016: The corpus language as a classification by the speaker, separate from the UI language

- **Status:** accepted (the author's decision 2026-08-24)
- **Date:** 2026-08-24

## Context

ADR-0015 made the interface bilingual and explicitly left open what gets trained: "the vocabulary stays German." That is exactly what the author commissioned changing the same day — English example sentences, and a corpus qualified by language.

The model already carries half the answer: `Word.language` and `AudioRecording.locale` have been BCP-47 fields since M1 and are set to `"de-DE"` everywhere. For the bundled corpus, qualifying by language is therefore pure data work. The gap was `OwnEntry` — recorded entries knew only a `speaker`, no language. But speaker and language are two axes: Andy can record German today and English tomorrow.

Two older decisions had to fall along the way. The backlog had listed "language arc batches A/B" as deferred since 2026-08-07. And on 2026-07-29 the author had chosen `en_US-lessac-medium` as the English voice — that was before ADR-0014 and before the F-Droid submission, so before "only what may be redistributed ships" applied.

## Decision

**1. The language is a classification by whoever creates the entry, not a statement about its content.** Whoever creates an entry picks the language; that decides exclusively **where the entry appears and gets used**. If Andy records his entries as German and later mixes an English sentence in, that one stays under German. In the author's words: "it could be Chinese or Bushman, we could not prevent it anyway."

That is not convenience but the only honest construction. The app sees the text only as a string somebody typed, and the audio is entirely opaque to it. Detection would be wrong sometimes — and inexplicable when it was.

**2. `CorpusLanguage` is the small, closed set of drawers** the UI offers (`DEUTSCH`, `ENGLISCH`), matched against the BCP-47 tags on the **primary subtag**: `de`, `de-AT`, `de_DE` all fall into the same drawer. A tag without a drawer — somebody's `zh-CN` — fits none and is not shown. That is the honest outcome; a catch-all "other" would feign an order that does not exist.

What gets stored in `OwnEntry` is the **tag**, not the enum name: this is a user file, and an entry from a later version with more languages has to keep parsing rather than blowing up the whole file (the `noiseScenario` pattern).

**3. The language filter applies before all others.** In `mergeCorpus` a foreign drawer drops out along with its recordings before `kind` and `excludedSpeakers` even run. The reason is not tidiness but protection from oneself: the corpus becomes SRS cards through `allOrSeed`. An English entry that slipped through would sit as a card in the author's real deck until he deleted it by hand.

**4. Separate from the UI language**, as ADR-0015 laid it out. Training German with an English interface is a legitimate combination. Coupling them would switch someone's training material when they only wanted to read the labels. Two settings, two places: the UI language on the start screen, the training language in the settings — with a line beneath it naming the difference.

**5. The English voice: `en_US-ljspeech-high`.** The licence decided, not the sound. LJ Speech is public domain (LibriVox recordings, book texts from 1884–1964), and the MODEL_CARD says "trained from scratch" — the model inherits nothing. The "high" tier follows the kerstin finding from M1: this app's core exercise is the isolated single word, and low tiers compress there. Measured on the first output: "bread" 0.53 s, "cow" 0.50 s, against thorsten's "Ball" at 0.52 s.

**6. Background noises stay language-free** (the author: "for background noise the language does not matter"). `NoiseScenario` has no language field and is not getting one.

## Alternatives

**Detect the language from the content.** Rejected, see point 1 — and the author framed the question differently anyway: he wanted a selector, not automation.

**`en_US-lessac-medium` (the decision of 2026-07-29).** Trained on Blizzard 2013, whose licence limits use to "research purposes" and explicitly excludes developing speech-synthesis products. Incompatible with ADR-0014. **`ryan`** and **`hfc_female`/`hfc_male`**: CC BY-**NC**-SA — the same non-commercial clause that got the three purchased noise loops removed in August; and `hfc` is additionally fine-tuned from `lessac`.

**`en_US-libritts_r-medium`** (CC BY 4.0) stays the fallback: free, but a 904-speaker model — you would have to pick a speaker id, the quality varies across readers, and it only exists as "medium".

**Couple the language to the UI language.** It would be one setting fewer. Rejected: the main user trains German, and switching his vocabulary when he switches the labels would be a side effect nobody ordered.

**A speaker exclusion per language** (`Map<language, Set<speaker>>` instead of a flat set). Rejected as effort without an occasion — see consequences.

## Consequences

**Easier:** a third language is one line in `CorpusLanguage`, one line in `tools/generate_tts.py` and data. The generator renders only what is missing and assigns every voice to its language; the folder `raw/<locale>/` follows from that.

**Harder:** two language settings need explaining. The hint line beneath the training language is the price for that.

**Deliberately accepted:** `excludedSpeakers` stays a flat set across all languages. Whoever deselects "Andy" in the German view has deselected him in the English one too. That can surprise; but an exclusion per language would be a new persistence form and a new migration for a case that does not exist yet. When it occurs, that is the occasion to change it.

**Also deliberate:** an entry filed under the wrong language stays filed wrongly until someone creates it anew — there is no changing an existing entry's language after the fact in this version. Existing entries sit at `de-DE` through the kotlinx default, which is factually right for all four that exist today.

**Not done and not claimed:** the twenty English entries are a taster, not a curated collection — ten words, ten sentences, freely composed. And the listening check has so far only measured duration; whether the voice is suitable for training is for the author's ear to decide.
