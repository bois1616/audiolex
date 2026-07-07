# corpus-data

Wortkorpus für AudioLex: Audiodateien und Metadaten.

- **Metadaten** (JSON/Markdown) werden versioniert.
- **Audiodateien** (WAV/MP3/OGG) sind per `.gitignore` ausgeschlossen, bis die Versionierungsstrategie entschieden ist (Backlog `[PROP]` Git-LFS).
- Format-Vorgabe für Aufnahmen: WAV, PCM16, 48 kHz, mono (Wörter) bzw. stereo (Störgeräusch-Loops) — siehe ADR-0003.
- Herkunft/Lizenz jeder fremden Quelle hier dokumentieren, auch bei nicht-kommerzieller Nutzung (Konzept, Abschnitt 8.2).

Befüllung ab M1; Audioquelle (TTS vs. eigene Aufnahmen) ist ein offener `[KLÄRUNG]`-Punkt im Backlog.
