# ADR-0003: Audio-Pipeline — gemeinsamer PCM-Mixer, plattformspezifische Ausgabe, WAV-Korpus

- **Status:** akzeptiert
- **Datum:** 2026-07-07

## Kontext

Kernanforderungen: kanalgetrennte Ansteuerung (links/rechts/beide) mit getrennten Pegeln, Störgeräusch-Overlay mit einstellbarem Signal-Rausch-Verhältnis (SNR), mehrere Zielplattformen. Diese Logik muss agentisch testbar sein — also ohne Audiogerät, als reine Funktion über Samples.

## Entscheidung

1. **Mixing in Common-Kotlin:** Kanal-Gain (`StereoGain`), Störgeräusch-Mischung und SNR-Berechnung (`noiseGainForSnr` über RMS) arbeiten auf `PcmBuffer` (interleaved PCM16) in `commonMain` — vollständig JVM-unit-testbar.
2. **Ausgabe als schmales expect/actual-Interface `AudioSink`:** Android → `AudioTrack`, Desktop → `javax.sound.sampled`, iOS später → `AVAudioEngine`. Der Sink bekommt fertig gemischtes Stereo-PCM; keine Plattformlogik oberhalb des Sinks.
3. **Korpus-Audioformat MVP: WAV (PCM16, 22050 Hz, mono):** kein Decoder nötig, ein selbstgeschriebener WAV-Loader läuft identisch auf allen Targets. 22050 Hz ist die native Ausgaberate der gewählten Piper-Stimmen (ADR-0006) — kein Resampling-Schritt, keine Qualitätsverluste. Mixer und `AudioSink` sind sample-rate-agnostisch (übernehmen die Rate aus dem `PcmBuffer`); Störgeräusch-Loops müssen dieselbe Rate wie die jeweilige Sprachaufnahme haben, sonst schlägt `mixWithNoise` fehl (Rate-Check). Sollten später hochwertigere Quellen (eigene Aufnahmen, andere TTS) mit abweichender Rate hinzukommen, ist ein Resampling-Schritt beim Korpus-Import nötig — nicht in der Kernlogik.

## Alternativen

- **Plattform-Player (ExoPlayer/Media3, AVPlayer):** nimmt Dateiformate ab, aber SNR-Mixing und Sample-genaue Kanalkontrolle wären pro Plattform doppelt zu bauen und kaum headless testbar. Verworfen.
- **Komprimierter Korpus (Opus/MP3) ab Start:** spart Platz, erzwingt aber plattformspezifische Decoder (MediaCodec/AVAudio) schon im MVP. Verschoben — bei Korpusgröße im Hundert-Wörter-Bereich ist WAV unkritisch.

## Konsequenzen

- Die fachlich riskanteste Logik (Pegel, SNR) ist deterministisch getestet; Plattform-Bugs können nur noch im dünnen Sink liegen.
- WAV-Dateien sind ~10× größer als Opus; akzeptiert für MVP, Codec-Umstieg später hinter dem Loader möglich.
- Latenz ist unkritisch (einzelne Wortwiedergabe, kein Echtzeit-Monitoring) — `AudioTrack`/javax.sound reichen, Oboe/JNI unnötig.
