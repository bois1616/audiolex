"""Generates the corpus WAV files from the corpus words.json via Piper TTS.

Offline, one-shot tooling (ADR-0006): run manually whenever the word list or
voice set changes; the app itself never calls Piper at runtime.

Voice models are not checked in (large binary downloads, see .gitignore);
fetch them once per machine:
    uv run python -m piper.download_voices --download-dir voices \\
        de_DE-thorsten-medium

Usage:
    uv run generate_tts.py
"""

import json
import subprocess
import sys
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
OUTPUT_DIR = CORPUS_DIR / "raw" / "de-DE"

# voiceId -> Piper model file. Standard German (locale "de-DE").
#
# A second, female voice was deliberately left out for now: Piper's only
# German single-speaker female models are "low"/"x_low" quality (kerstin,
# ramona, eva_k). Testing kerstin-low showed it speaks isolated single
# words noticeably rushed/compressed compared to full sentences (e.g.
# "Ball" alone: ~0.24s vs. ~0.52s for the same word with thorsten-medium)
# -- likely a quality-tier limitation, not fixable by prompt tricks alone.
# See docs/adr/0006-audioquelle-tts.md and backlog M1 for the follow-up.
VOICES = {
    "thorsten": VOICES_DIR / "de_DE-thorsten-medium.onnx",
}
LOCALE = "de-DE"


def generate(word_text: str, model_path: Path, out_path: Path) -> None:
    out_path.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        [sys.executable, "-m", "piper", "--model", str(model_path), "--output-file", str(out_path)],
        input=word_text,
        text=True,
        check=True,
    )


def main() -> None:
    words = json.loads(WORDS_JSON.read_text(encoding="utf-8"))

    for voice_id, model_path in VOICES.items():
        if not model_path.exists():
            raise SystemExit(
                f"Voice model missing: {model_path}\n"
                f"Run: uv run python -m piper.download_voices "
                f"--download-dir {VOICES_DIR} {model_path.stem}"
            )

    recordings = []
    for word in words:
        for voice_id, model_path in VOICES.items():
            out_path = OUTPUT_DIR / f"{word['id']}__{voice_id}.wav"
            generate(word["text"], model_path, out_path)
            print(f"  {out_path.relative_to(REPO_ROOT)}")
            recordings.append({
                "id": f"{word['id']}__{voice_id}",
                "wordId": word["id"],
                "voiceId": voice_id,
                "locale": LOCALE,
                "fileRef": str(out_path.relative_to(CORPUS_DIR)),
            })

    RECORDINGS_JSON.write_text(
        json.dumps(recordings, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )
    print(f"Generated {len(recordings)} recordings for {len(words)} words "
          f"x {len(VOICES)} voices (locale={LOCALE}).")
    print(f"Metadata written to {RECORDINGS_JSON.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    main()
