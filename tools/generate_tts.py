"""Generates the corpus WAV files from the corpus words.json via Piper TTS.

Offline, one-shot tooling (ADR-0006): run manually whenever the word list or
voice set changes; the app itself never calls Piper at runtime.

Voice models are not checked in (large binary downloads, see .gitignore);
fetch them once per machine:
    uv run python -m piper.download_voices --download-dir voices \\
        de_DE-thorsten-medium en_US-ljspeech-high

Usage:
    uv run generate_tts.py            # only what's missing
    uv run generate_tts.py --force    # re-render everything
"""

import argparse
import json
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

TOOLS_DIR = Path(__file__).parent
REPO_ROOT = TOOLS_DIR.parent
# Corpus lives inside composeApp's Compose Resources, not a top-level dir:
# it's read at runtime via Res.readBytes on every target (incl. Android,
# which has no repo-relative filesystem access) -- see ADR-0003/architektur.md.
CORPUS_DIR = REPO_ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "corpus"
WORDS_JSON = CORPUS_DIR / "words.json"
RECORDINGS_JSON = CORPUS_DIR / "recordings.json"
VOICES_DIR = TOOLS_DIR / "voices"


@dataclass(frozen=True)
class Voice:
    """A Piper model plus the corpus locale it is responsible for."""

    model: Path
    locale: str


# voiceId -> Voice. A voice only ever renders words whose `language` matches
# its locale (ADR-0016), so adding a language means adding a row here and
# entries to words.json -- nothing else.
#
# German: a second, female voice was deliberately left out. Piper's only
# German single-speaker female models are "low"/"x_low" quality (kerstin,
# ramona, eva_k). Testing kerstin-low showed it speaks isolated single
# words noticeably rushed/compressed compared to full sentences (e.g.
# "Ball" alone: ~0.24s vs. ~0.52s for the same word with thorsten-medium)
# -- likely a quality-tier limitation, not fixable by prompt tricks alone.
#
# English: ljspeech at "high". Chosen for its licence above all -- the LJ
# Speech dataset is public domain, so the rendered WAVs may be shipped in
# the repository (ADR-0014's condition). The obvious-looking alternatives
# fail exactly there: lessac is Blizzard-2013 ("Research Purposes" only),
# ryan and hfc are CC BY-NC-SA, and hfc is additionally fine-tuned from
# lessac. "high" rather than "medium" because of the kerstin finding above:
# this app's core exercise is the isolated single word, and that is where
# low tiers compress. Measured on the first take: "bread" 0.53s, "cow"
# 0.50s -- in line with thorsten's 0.52s, no compression.
VOICES = {
    "thorsten": Voice(VOICES_DIR / "de_DE-thorsten-medium.onnx", locale="de-DE"),
    "ljspeech": Voice(VOICES_DIR / "en_US-ljspeech-high.onnx", locale="en-US"),
}


def generate(word_text: str, model_path: Path, out_path: Path) -> None:
    out_path.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        [sys.executable, "-m", "piper", "--model", str(model_path), "--output-file", str(out_path)],
        input=word_text,
        text=True,
        check=True,
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--force",
        action="store_true",
        help="re-render WAVs that already exist (default: leave them alone)",
    )
    args = parser.parse_args()

    words = json.loads(WORDS_JSON.read_text(encoding="utf-8"))

    for voice_id, voice in VOICES.items():
        if not voice.model.exists():
            raise SystemExit(
                f"Voice model missing: {voice.model}\n"
                f"Run: uv run python -m piper.download_voices "
                f"--download-dir {VOICES_DIR} {voice.model.stem}"
            )

    # Entries produced by hand rather than by Piper -- the demo recordings the
    # author and Grete spoke into the app -- are carried over untouched.
    # Without this the script would silently drop them on every run, since it
    # rewrites this file wholesale; they are irreplaceable and were nearly
    # lost that way.
    existing = json.loads(RECORDINGS_JSON.read_text(encoding="utf-8")) if RECORDINGS_JSON.exists() else []
    handmade = [r for r in existing if r["voiceId"] not in VOICES]
    # A word somebody actually spoke does not get a synthetic double. The
    # first run of this rewrite happily rendered thorsten versions of the four
    # demo entries the author and Grete had recorded, quietly changing corpus
    # content that nobody asked to change. The rule is data-driven on purpose:
    # no marker to maintain, and it keeps holding as more entries get spoken.
    spoken_word_ids = {r["wordId"] for r in handmade}

    generated, rendered, skipped = [], 0, 0
    for word in words:
        for voice_id, voice in VOICES.items():
            if word["language"] != voice.locale or word["id"] in spoken_word_ids:
                continue
            out_path = CORPUS_DIR / "raw" / voice.locale / f"{word['id']}__{voice_id}.wav"
            if out_path.exists() and not args.force:
                skipped += 1
            else:
                generate(word["text"], voice.model, out_path)
                rendered += 1
                print(f"  {out_path.relative_to(REPO_ROOT)}")
            generated.append({
                "id": f"{word['id']}__{voice_id}",
                "wordId": word["id"],
                "voiceId": voice_id,
                "locale": voice.locale,
                "fileRef": str(out_path.relative_to(CORPUS_DIR)),
            })

    RECORDINGS_JSON.write_text(
        json.dumps(handmade + generated, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )
    per_locale = ", ".join(
        f"{locale}: {sum(1 for r in generated if r['locale'] == locale)}"
        for locale in sorted({v.locale for v in VOICES.values()})
    )
    print(f"Rendered {rendered}, left alone {skipped}, kept {len(handmade)} hand-made.")
    print(f"Recordings per locale -- {per_locale}")
    print(f"Metadata written to {RECORDINGS_JSON.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    main()
