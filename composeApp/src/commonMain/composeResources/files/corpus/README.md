# corpus

Wortkorpus für AudioLex: Audiodateien und Metadaten, als Compose-Resource unter `composeApp` (nicht als eigenständiger Top-Level-Ordner), damit alle Targets — inklusive Android, das keinen repo-relativen Dateisystempfad hat — per `Res.readBytes(...)` einheitlich darauf zugreifen (siehe `docs/architektur.md`).

- **Metadaten** (`words.json`, `recordings.json`) werden versioniert.
- **Audiodateien** (WAV/MP3/OGG) sind per `.gitignore` ausgeschlossen, bis die Versionierungsstrategie entschieden ist (Backlog `[PROP]` Git-LFS).
- Format: WAV, PCM16, 22050 Hz, mono — native Piper-Ausgaberate (ADR-0003, ADR-0006).
- Herkunft/Lizenz jeder fremden Quelle hier dokumentieren, auch bei nicht-kommerzieller Nutzung (Konzept, Abschnitt 8.2).
- Generiert über `tools/generate_tts.py` (`uv run generate_tts.py` aus `tools/`).

## Herkunft der Satz-Einträge

Die `satz-*`-Einträge (`"kind": "SENTENCE"`) sind motivisch an Douglas Adams, „Per Anhalter durch die Galaxis" (Kapitel 1), angelehnt, aber **frei paraphrasiert** — keine wörtlichen Zitate stehen im Repo (ADR-0009). Nutzung privat, nicht-öffentlich. Kriterium ist akustische Erkennbarkeit beim Mitlesen, nicht Werktreue.
