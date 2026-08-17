# Gebündelte Störgeräusche

Störgeräusch-Loops, die mit der App ausgeliefert werden (SNR-Overlay, ADR-0010). Anders als früher sind hier **Dateien und Metadaten versioniert** — F-Droid baut aus dem Quelltext, also muss alles Ausgelieferte im Repository liegen.

## Was hier liegen darf

Nur Aufnahmen, deren Weitergabe erlaubt ist. Praktisch heißt das: eigene Aufnahmen des Autors. Fremde Loops mit „frei für nicht-kommerzielle Nutzung" sind mit einer Veröffentlichung unvereinbar — die drei früheren Loops (salamisound, Pixabay) sind aus genau diesem Grund entfernt worden (ADR-0014, ADR-0010 Nachtrag). Bei jeder Datei gehören Herkunft und Lizenz in die Tabelle unten, damit ein F-Droid-Prüfer sie findet, ohne fragen zu müssen.

## Bestand

| id | Label | Datei | Herkunft | Lizenz |
| --- | --- | --- | --- | --- |
| `bus` | Bus, innen | `bus.wav` | Eigene Aufnahme des Autors, 2026-08-10, in der App aufgenommen | CC0-1.0 (Autor-Entscheid 2026-08-17) |

Die Datei ist **byte-identisch** zur Aufnahme vom Testgerät (per SHA-256 belegt): Sie kam schon als 22050 Hz mono PCM16 aus der App-Aufnahme, also im Format, das der Mixer verlangt. Konvertiert wurde nichts, nur umbenannt.

## Warum diese Aufnahme unbearbeitet bleibt

Der Konvertierungsweg unten (trimmen auf 8 s, `loudnorm`) war für zugekaufte Loops gedacht und ist hier gegenstandslos — gemessen am 2026-08-17:

- **Pegelverlauf flach:** In 0,5-s-Fenstern schwankt der Pegel um höchstens ±2,2 dB um den Gesamtschnitt; die ersten 2 s liegen +0,07 dB daneben. Damit trifft das Gehörte den eingestellten SNR, was der ganze Zweck der früheren Normalisierung war (A53-Befund 2026-07-19).
- **Länge reicht:** 5,6 s, während die längste Aufnahme im Korpus 3,76 s dauert (`satz-zug`). Der Mixer wickelt den Loop per Modulo um; ein Wrap-Klick kann also nicht auftreten, weil kein Wort über das Loop-Ende hinausreicht.
- **Pegel gesund:** RMS −19,4 dBFS, Peak −1,9 dBFS — praktisch das Ergebnis, das `loudnorm=I=-20:TP=-2` bei den alten Loops erzeugt hat.

**Hörprobe am Hörgerät, 2026-08-17: bestanden.** Urteil des Autors: „sehr gut". Bei „Fortgeschritten" (SNR −5 dB) ist das Geräusch **sehr dominant**, was der Einstellung entspricht — dort liegt das Rauschen über der Sprache —, und lässt sich mit dem Regler leicht zurücknehmen. Von Knistern war keine Rede: Die gemessene Begrenzung (Crest-Faktor 17,5 dB; bei −5 dB laufen 3,1× Verstärkung und damit 1,7 % der Samples in den Clip des Mixers, bei +5 dB 0,035 %) fällt am Ohr nicht auf. Deshalb **kein Limiter-Durchgang** und keine Bearbeitung der Datei; die Zahlen bleiben hier stehen, damit ein späteres Knistern nicht neu gemessen werden muss.

## Ein weiteres Geräusch einsetzen

1. Aufnahme besorgen. Vom Testgerät geht das ohne Root, weil der Debug-Build `debuggable` ist:

   ```bash
   export ANDROID_SERIAL=192.168.178.24:<Port>
   adb exec-out "run-as de.hexenwoche.audiolex cat files/eigene-stoergeraeusche/<datei>.wav" > resources/sounds/bus.wav
   ```

   Welche Datei welches Geräusch ist, steht in `files/eigene-stoergeraeusche/geraeusche.json` (dieselbe `run-as`-Zeile mit `cat`).

2. In das Format bringen, das der Mixer verlangt (22050 Hz, mono, PCM16 — die Piper-Ausgaberate, sonst wird das Rauschen zur Wiedergabe stillschweigend weggelassen). `<start>` in einen gleichmäßig lauten Abschnitt legen, Intro und Löcher überspringen:

   ```bash
   ffmpeg -y -ss <start> -t 8 -i resources/sounds/bus.wav -ac 1 -ar 22050 \
     -af loudnorm=I=-20:TP=-2:LRA=11 -c:a pcm_s16le \
     composeApp/src/commonMain/composeResources/files/noise/bus.wav
   ```

   **Warum 8 Sekunden und normalisiert** (A53-Befund 2026-07-19): Bei der Per-Wort-Mischung wird immer nur der **Loop-Anfang** gehört — jedes Wort startet das Rauschen bei Sample 0 und dauert 1–3 s —, während die SNR-Verstärkung über die RMS des **ganzen** Loops berechnet wird. Ein leiser Anfang macht das Geräusch dadurch fast unhörbar, obwohl der eingestellte SNR etwas anderes verspricht. Ein kurzer, stetiger, normalisierter Abschnitt macht Anfang ≈ Schnitt; 8 s sind länger als der längste Satz, also klickt es beim Loopen nicht.

3. Eintrag in `noise.json` ergänzen:

   ```json
   [
     { "id": "bus", "label": "Bus", "fileRef": "bus.wav" }
   ]
   ```

   `id` ist die persistierte Kennung (steht danach in den Einstellungen des Nutzers), `label` der sichtbare Text, `fileRef` der Dateiname in diesem Ordner. Unbekannte Felder werden beim Parsen ignoriert, ein zusätzlicher Kommentar-Schlüssel bricht also nichts.

4. Tabelle oben ausfüllen und den Abschnitt „Inhalte" in der Haupt-README prüfen: Dort steht die Lizenz der ausgelieferten Inhalte.

5. Hörprobe am Gerät, nicht am Desktop — der Pegel ist nur mit dem Hörgerät beurteilbar (AGENTS.md §7, DoD §3).
