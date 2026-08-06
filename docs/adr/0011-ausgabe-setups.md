# ADR-0011: Zwei gleichrangige Ausgabe-Setups (Hörgerät / Stereo-Kopfhörer), automatisch erkannt

- **Status:** akzeptiert (Autor-Entscheid 2026-08-06, Architektur durch Opus-Schärfung am selben Tag)
- **Datum:** 2026-08-06

## Kontext

ADR-0007 hat 2026-07-08 das **BT-Hörgerät am linken Ohr** als Referenz-Trainings-Setup festgelegt und Kanaltrennung damit vom Kernfeature zur „Setup-Option für Alternativ-Hardware" herabgestuft. Der Grund war zwingend: über den ASHA-/BT-Pfad erreicht das Signal nur ein Hörgerät, das Stereo zu Mono summiert — Panning ist dort nicht wahrnehmbar, und eine UI, die Kanalwahl als wirksam anbietet, würde lügen.

Der Autor hat am 2026-08-06 eine Erweiterung gefordert: Die App soll auch mit (kabelgebundenen, jedenfalls stereo-tauglichen) Kopfhörern laufen — „das würde die App auf einen Lern-Modus heben und nicht geräteabhängig machen". Damit ändert sich nicht der Trainingszweck, wohl aber der Anspruch: nicht mehr faktisch an eine Hardware gebunden.

Der Kern trägt das bereits. `StereoGain` (`BOTH`/`LEFT_ONLY`/`RIGHT_ONLY`, je mit eigenem Pegel) und `PcmBuffer.toStereoWithGain` liegen seit M1 fertig und unit-getestet in `core/audio` und werden von keinem Screen benutzt; `AndroidAudioSink` setzt die Kanalmaske bereits abhängig von `buffer.channels` (`CHANNEL_OUT_MONO`/`CHANNEL_OUT_STEREO`). ADR-0007 hatte das ausdrücklich vorgesehen: „Ändert sich die Versorgung […], ist nur UI-/Preset-Arbeit nötig — Mixer und Datenmodell bleiben kanalfähig."

Offen war, **woher die App weiß**, welches Setup gerade aktiv ist. Ohne diese Information kann sie die Kanalwahl nur pauschal anbieten (und damit im Hörgerät-Fall Wirksamkeit vortäuschen, was ADR-0007 verbietet) oder pauschal verstecken (und damit den Kopfhörer-Fall nicht bedienen).

## Entscheidung

**1. Zwei gleichrangige Setups statt Referenz + Ausnahme.** Wir modellieren `OutputSetup { HOERGERAET, STEREO_KOPFHOERER }` als benannten Zustand. Das Hörgerät bleibt das Setup, gegen das die Gerätetests der DoD laufen (AGENTS.md §4.3) — die trainierte Hörsituation ändert sich nicht. Der Kopfhörer ist aber kein Sonderfall mehr, sondern ein unterstützter Betriebsmodus mit eigener Trainingsqualität: nur dort ist gezielte Kanalarbeit überhaupt möglich.

**2. Automatische Erkennung, keine manuelle Umschaltung** (Autor-Entscheid 2026-08-06; erwogene Alternativen siehe unten). Die Erkennungsregel folgt der einzigen Frage, auf die es fachlich ankommt — *erreichen zwei getrennte Signale zwei Ohren?* — und nicht der Gerätekategorie an sich:

| Erkanntes Ausgabegerät | Setup | Begründung |
| --- | --- | --- |
| `TYPE_WIRED_HEADPHONES`, `TYPE_WIRED_HEADSET`, `TYPE_USB_HEADSET` | Stereo-Kopfhörer | zwei Wandler, ein Ohr je Kanal |
| `TYPE_BLUETOOTH_A2DP` | Stereo-Kopfhörer | reguläre BT-Kopfhörer; ein ASHA-Hörgerät meldet sich **nicht** als A2DP |
| `TYPE_HEARING_AID` | Hörgerät | der Referenzfall, mono summiert |
| BLE-Typen (`TYPE_BLE_HEADSET` u. ä.) | Hörgerät | mehrdeutig (LE-Audio-Ohrhörer *oder* LE-Audio-Hörgerät) — konservativ, siehe Konsequenzen |
| Lautsprecher, Hörmuschel, alles Übrige | Hörgerät | Kanaltrennung nicht kontrollierbar (ADR-0007: Freifeld ist ungeeignet) |

Im Zweifel gilt das Hörgerät-Setup. Der Fehler geht damit immer in die harmlose Richtung: eine gesperrte Kanalwahl, nie eine vorgetäuschte.

**3. Die Erkennung lebt in `:composeApp`, nicht in `:core`.** Auf Android braucht `AudioManager` einen `Context`; `:core` bleibt kontextfrei, wie schon bei der Datenbank entschieden (ADR-0004, `DatabaseBuilder.android.kt` liegt aus genau diesem Grund in `:composeApp`). `:core` steuert nur den plattformfreien Teil bei: das Enum und die bereits vorhandene `toStereoWithGain`-Rechnung.

**4. Der erkannte Zustand ist beobachtbar, nicht einmalig abgefragt.** Kopfhörer werden im laufenden Betrieb ein- und ausgesteckt; Android liefert dafür `AudioManager.registerAudioDeviceCallback`. Die UI muss dem folgen, ohne dass der Nutzer einen Screen neu betritt.

**5. Die Kanalwahl wirkt im gemeinsamen Producer**, dort wo bereits das Störgeräusch eingemischt wird (`NoiseMixing.kt`, ADR-0010) — nach Decode und Rauschmischung, vor der Übergabe an den Sink. Der Sink bleibt dumm (ADR-0003).

**6. Auf dem Desktop wird nicht erkannt.** Die Ausgabe läuft dort über `paplay` (ADR-0003); eine belastbare Geräteerkennung wäre nur über zusätzliche externe Aufrufe zu haben. Das Desktop-Target ist Dev-Target, keine Verifikationsplattform — es gilt dort pauschal als stereofähig, dokumentiert statt kaschiert.

## Alternativen

- **Manuelle Umschaltung in den Einstellungen** („Ausgabegerät: Hörgerät / Kopfhörer"). Kein Plattformcode, keine Fehlerkennung, sofort umsetzbar. Vom Autor am 2026-08-06 verworfen zugunsten der Automatik — bleibt die naheliegende Rückfalloption, falls sich die Erkennung am Gerät als unzuverlässig erweist.
- **Auto-Erkennung mit manueller Überstimmung.** Beste Bedienbarkeit, aber zwei Zustände, die auseinanderlaufen können, und die größte Umsetzung. Verworfen als verfrüht: erst zeigen, ob die Automatik trägt.
- **Kopfhörer als neues Referenz-Setup.** Verworfen aus demselben Grund wie 2026-07-08 in ADR-0007: trainiert werden soll die reale Hörsituation, und die ist das Hörgerät. Der Kopfhörer kommt hinzu, er ersetzt nicht.
- **Erkennung in `:core` mit durchgereichtem Context.** Verworfen — bräche die Kontextfreiheit von `:core`, die das Projekt an mehreren Stellen bewusst durchhält.

## Konsequenzen

- **ADR-0007 wird nicht abgelöst, sondern ergänzt** (Nachtrag dort vermerkt). Sein Kern bleibt gültig: Referenz ist das Hörgerät, maßgeblich sind Pegel und Verständlichkeit am trainierten Ohr. Was sich ändert, ist die Rolle der Kanaltrennung — vom „Werkzeug für Alternativ-Setups" zum zweiten unterstützten Betriebsmodus.
- **Die UI-Auflage aus ADR-0007 bleibt bestehen und wird schärfer**: Eine Kanalwahl ≠ „beide" darf im Hörgerät-Setup nicht als wirksam erscheinen. Neu hinzu kommt die Gegenrichtung — die App zeigt sichtbar an, welches Setup sie erkannt hat, damit eine Fehlerkennung für den Nutzer überhaupt bemerkbar ist.
- **Bewusst eingegangenes Risiko:** Die Automatik kann falsch liegen, und für den Nutzer ist das schwer zu durchschauen (gesperrte Regler ohne ersichtlichen Grund). Die konservative Zweifelsregel begrenzt den Schaden auf „zu wenig angeboten". Zeigt sich das im Gerätetest, ist die manuelle Überstimmung der dokumentierte Nachzug.
- **BLE-Ambiguität ist eine offene Flanke.** LE-Audio-Ohrhörer werden als Hörgerät-Setup behandelt und verlieren damit die Kanalwahl. Das ist für diese App vertretbar (der Nutzer ist Hörgeräteträger), aber es ist eine Vereinfachung, keine korrekte Unterscheidung.
- **Neuer Plattformcode auf Android**, der bisher nirgends existierte. Er ist auf die Erkennung begrenzt; der Audio-Pfad selbst bleibt plattformfrei (ADR-0003).
- **Der Prototyp-Charakter der Kanalarbeit endet.** `StereoGain` war seit M1 toter, aber getesteter Code — mit diesem ADR wird er zum ersten Mal produktiv. Die vorhandenen Tests decken die Rechnung bereits ab.
