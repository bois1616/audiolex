# Störgeräusch-Loops

Gebündelte Störgeräusch-Szenarien für das SNR-Overlay (Backlog M4, ADR-0010). Analog zum Korpus: die Audiodateien (`*.wav`) sind **gitignored**, nur diese `README.md` und `noise.json` (Metadaten) sind versioniert. Auf einem frischen Checkout werden die WAVs per Konvertierung aus der Autor-Quellbibliothek (`resources/sounds/`, Repo-Root, ebenfalls gitignored) neu erzeugt.

## Szenarien

| id | Label | Quelle | Lizenz |
| --- | --- | --- | --- |
| `verkehr` | Straßenverkehr | [salamisound.de](https://www.salamisound.de/) — `salamisound-4334625-strassenverkehr-mit-autos` | frei für nicht-kommerzielle Nutzung (Autor-Angabe) |
| `strassenbahn` | Autos & Straßenbahn | [salamisound.de](https://www.salamisound.de/) — `salamisound-1020027-autos-und-strassenbahn-fahren` | frei für nicht-kommerzielle Nutzung (Autor-Angabe) |
| `restaurant` | Restaurant | [pixabay.com](https://pixabay.com/de) — `38534292-record-street-restaurant-atmosphere-sound-193447` | Pixabay Content License / frei für nicht-kommerzielle Nutzung (Autor-Angabe) |

Nutzungskontext: privat, nicht-kommerziell (wie der Korpus). Die Quell-MP3s liegen unter `resources/sounds/` (Autor-Bibliothek, gitignored).

## Format & Konvertierung

Die App decodiert nur PCM16-WAV und der Mixer (`mixWithNoise`) verlangt dieselbe Sample-Rate und Kanalzahl wie das Speech-Signal (Piper: **mono, 22050 Hz**). Die WAVs werden aus den (stereo, 44,1/48 kHz) MP3-Quellen als **kurze (~8 s), gleichmäßig laute Loops aus einem stetigen Abschnitt** erzeugt und auf einheitliche Lautheit normalisiert (`loudnorm`).

**Warum so (A53-Befund 2026-07-19):** Bei der Per-Wort-Mischung wird immer nur der **Loop-Anfang** gehört (jedes Wort startet das Rauschen bei Sample 0, ein Wort dauert ~1–3 s), und die SNR-Verstärkung wird über die RMS des **ganzen** Loops berechnet. Ein leiser Intro-Abschnitt (die frühere 20-s-Fassung des Verkehrs-Loops war am Anfang ~10 dB leiser als im Schnitt) war dadurch effektiv fast unhörbar. Ein kurzer, stetiger, normalisierter Abschnitt macht Anfang ≈ Schnitt, sodass das Gehörte den eingestellten SNR trifft; 8 s > längster Satz verhindert einen Wrap-Klick beim Loopen.

```bash
# <start> = Sekunden-Offset in einen stetigen Abschnitt (Intro/leise Stellen überspringen)
ffmpeg -y -ss <start> -t 8 -i resources/sounds/<quelle>.mp3 -ac 1 -ar 22050 \
  -af loudnorm=I=-20:TP=-2:LRA=11 -c:a pcm_s16le \
  composeApp/src/commonMain/composeResources/files/noise/<id>.wav
```

Aktueller Bestand (`<start>`-Offsets): verkehr 30 s, strassenbahn 6 s, restaurant 12 s.

Neues Szenario: MP3/WAV nach `resources/sounds/` legen, wie oben (stetigen `<start>` wählen) nach `files/noise/<id>.wav` konvertieren und einen Eintrag in `noise.json` (`id`, `label`, `fileRef`, `source`, `license`) ergänzen.
