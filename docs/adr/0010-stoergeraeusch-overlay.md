# ADR-0010: Störgeräusch-Overlay (SNR) — Mischung im gemeinsamen Producer, freie Quelle, persistiertes Setting

- **Status:** akzeptiert (Autor-Entscheide 2026-07-19, Architektur durch Opus-Schärfung am selben Tag)
- **Datum:** 2026-07-19

## Kontext

Kern des Trainings ist das Decodieren von Sprache unter erschwerten Bedingungen; ein Störgeräusch-Overlay mit einstellbarem Signal-Rausch-Abstand (SNR) ist der nächste Schritt (Backlog M4, Konzept §3). Der Mixer ist seit M1 vorbereitet, aber ungenutzt: `mixWithNoise`, `noiseGainForSnr` und `rms()` in `core/audio/Mixer.kt` sind plattformfrei und unit-getestet. Offen waren drei Fragen: **wo** im Audio-Pfad gemischt wird, **woher** das Störgeräusch kommt, und **wie/wo** der SNR gesteuert und gespeichert wird.

Autor-Entscheide vom 2026-07-19, die den Rahmen setzen: das Störgeräusch kommt aus einer **freien Quelle** (frei lizenziert, z. B. CC0), der SNR-Regler sitzt in den **Einstellungen**, und das Rauschen wirkt in **beiden** Trainingsmodi (Lern- und Prüfmodus).

## Entscheidung

1. **Mischen im gemeinsamen Producer, nicht im Sink.** Beide Trainings-Screens spielen über `queue.play { … }` — der Producer decodiert die WAV zu `PcmBuffer` und gibt sie zurück. Das Störgeräusch wird **dort**, nach dem Decode und vor der Rückgabe, eingemischt (`mixWithNoise(speech, noise, noiseGainForSnr(...))`). Damit gilt die Mischung für Android und Desktop gleich, und der `AudioSink` bleibt eine dumme Ausgabe (ADR-0003). Die 180-ms-Pre-Roll-Stille des Android-Sinks (`withLeadingSilence`) wird nach dem Mischen vorne angehängt und bleibt unberührt.

2. **Störgeräusch als freie, lokal beschaffte Ressource — Quellbibliothek getrennt von der gebündelten Ressource.** Der Autor hält seine Roh-Audiobibliothek unter `resources/sounds/` (Repo-Root, gitignored) — Störgeräusche jetzt, eigene Wort-/Satzaufnahmen später. Für die App werden die Loops daraus auf **22050 Hz mono PCM16** konvertiert (`ffmpeg -t 20 -ac 1 -ar 22050 -c:a pcm_s16le …`; Speech-Format, da `mixWithNoise` gleiche Sample-Rate und Kanalzahl verlangt; die App decodiert nur WAV, kein MP3) und unter `composeApp/src/commonMain/composeResources/files/noise/` abgelegt — nur von dort lädt Compose sie zur Laufzeit auf jedem Target (`Res.readBytes`), ein Repo-Root-Verzeichnis ist auf Android nicht erreichbar. Diese WAVs bleiben **gitignored** wie die Korpus-WAVs; versioniert sind nur `noise.json` (Szenario-Metadaten: `id`/`label`/`fileRef`/`source`/`license`) und `README.md` (Herkunft/Lizenz/Konvertierung). Erster Bestand (2026-07-19): drei Szenarien (`verkehr`, `strassenbahn`, `restaurant`) von salamisound.de/pixabay.com, frei für nicht-kommerzielle Nutzung, auf 20 s getrimmt. Fehlt eine Datei, ist das Feature hörbar wirkungslos (Fallback: sauberes Speech), bricht aber nicht.

3. **SNR als persistiertes App-Setting, beide Modi.** `AppSettings`/`SettingsEntity` bekommen `noiseEnabled: Boolean = false` und `snrDb: Int = 10` (DB-Version 4 → 5, destruktiver Fallback trägt wie bei den bisherigen Bumps). Ein `Switch` + `Slider` im `EinstellungenScreen` steuern beides; die Werte werden über dasselbe Lade-/Speicher-Muster wie `themeMode`/`corpusMode` durch `App()` an beide Screens gereicht. Beide Modi teilen dasselbe Setting — keine per-Modus-Trennung.

4. **Kein Resampling in Code.** Die Sample-Rate-Angleichung ist ein dokumentierter ffmpeg-Schritt, kein Laufzeit-Resampler. Ein defensiver `PcmBuffer.toMono()` fängt eine versehentlich stereo abgelegte Datei ab; bei abweichender Sample-Rate wird das Rauschen für die Wiedergabe weggelassen (kein Crash) statt aliasing-behaftet resampled.

## Alternativen

- **Störgeräusch im Code erzeugen (rosa/weißes Rauschen):** verworfen zugunsten der Autor-Wahl „freie Quelle" — generiertes Breitbandrauschen wäre asset- und lizenzfrei, deterministisch testbar und ohne Beschaffungsschritt, klingt aber weniger realistisch als echtes Stimmengewirr. Bleibt als möglicher Weg für ein späteres, zusätzliches Szenario notiert.
- **Mischen im plattformspezifischen Sink:** verworfen — verdoppelte die Logik über Android/Desktop und verletzte die ADR-0003-Grenze „unter dem Sink keine Business-Logik".
- **Laufzeit-Resampler in `:core`:** vorerst verworfen — Downsampling ohne Tiefpass erzeugt Aliasing (Spektrumverschiebung), und der ffmpeg-Schritt ist einmalig und trivial. Bei echtem Bedarf ein eigenes `[PROP]`.
- **SNR als In-Session-Regler statt Einstellung:** verworfen (Autor-Entscheid „in den Einstellungen") — zentrale Persistenz, komponiert später sauber mit dem Presets-Item.
- **Durchgehender Rausch-Teppich über Wortgrenzen:** außerhalb des Scope — das Rauschen startet je Wiedergabe neu am Loop-Anfang. Für ein erstes Szenario akzeptiert; ein kontinuierlicher Bed wäre ein eigener Umbau (Sink hielte dann Zustand).

## Konsequenzen

- **Leichter:** kein neuer Audio-Layer, der vorbereitete Mixer wird endlich genutzt; die Mischung sitzt an einer Stelle für beide Plattformen; das Setting dockt an das bestehende `SettingsEntity`-Fundament an; die Presets-Stufe (M4) findet ihre Haupt-Stellschraube (`noiseEnabled`/`snrDb`) vor.
- **Schwerer / bewusste Schulden:** Content-Abhängigkeit — die Loops werden lokal aus `resources/sounds/` konvertiert (wie die Korpus-WAVs), auf einem frischen Checkout sind sie bis dahin inert. Das Rauschen ist nicht kontinuierlich über Wörter hinweg (startet je Wiedergabe neu am Loop-Anfang); die Bundle-WAVs sind auf 20 s getrimmt, weil die Per-Wort-Mischung ohnehin nur den Anfang nutzt. Die destruktive DB-Migration verwirft beim Bump die SRS-Karten des Testgeräts (akzeptierter Prototyp-Stand, keine echten Nutzerinstallationen).
- **Abgrenzung eigene Aufnahmen:** `resources/sounds/` ist eine **Build-Zeit-Quellbibliothek** (der Autor legt Rohdateien ab, sie werden konvertiert und ins Bundle übernommen). Später gewünschte, zur Laufzeit in der App eingesprochene Wörter/Sätze (Backlog M2 „Eigene Wörter/Sätze einsprechen", `[→Opus]`) sind etwas anderes: Nutzer-Content, der app-lokal/geräteintern gespeichert und importiert werden muss, **nicht** über den zur Build-Zeit gepackten Compose-Resources-Pfad. Ein Repo-Verzeichnis erscheint nicht automatisch in der laufenden App.
