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

Die App decodiert nur PCM16-WAV und der Mixer (`mixWithNoise`) verlangt dieselbe Sample-Rate und Kanalzahl wie das Speech-Signal (Piper: **mono, 22050 Hz**). Die WAVs werden daher aus den (stereo, 44,1/48 kHz) MP3-Quellen so erzeugt — auf 20 s getrimmt, weil bei der Per-Wort-Mischung ohnehin nie mehr genutzt wird und das APK-Bundle klein bleibt:

```bash
ffmpeg -y -i resources/sounds/<quelle>.mp3 -t 20 -ac 1 -ar 22050 -c:a pcm_s16le \
  composeApp/src/commonMain/composeResources/files/noise/<id>.wav
```

Neues Szenario: MP3/WAV nach `resources/sounds/` legen, wie oben nach `files/noise/<id>.wav` konvertieren und einen Eintrag in `noise.json` (`id`, `label`, `fileRef`, `source`, `license`) ergänzen.
