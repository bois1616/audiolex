# AudioLex

*[English version](README.en.md)*

Hörtraining für Wortverständnis. Die App spielt ein Wort, man ordnet es zu, und der Abstand bis zur Wiederholung richtet sich danach, wie gut es saß — Spaced Repetition wie bei Anki, aber mit Klang als Reiz statt Schrift.

Sie ist für den Fall gebaut, in dem Hören und Verstehen auseinanderfallen: Nach einseitigem Hörverlust kommt der Schall an, wird aber nicht mehr zuverlässig als Sprache erkannt. Ein Hörgerät macht laut, nicht verständlich; der Weg vom Klang zum Wort will wieder geübt werden.

Nicht-kommerziell, ohne Konto, ohne Werbung. Es gibt keine Internet-Berechtigung, also bleiben Wortschatz, Bewertungen und Sitzungsverlauf auf dem Gerät — nicht als Zusage, sondern weil die App technisch nichts übertragen kann.

AudioLex ist ein Übungswerkzeug, kein medizinisches Produkt. Es ersetzt weder den Hörgeräteakustiker noch die HNO-Abklärung.

## Was die App kann

- **Lernmodus** — Wort hören, Text mitlesen. Baut die Verbindung zwischen Gehörtem und Bedeutung.
- **Prüfmodus** — Wort hören, Karte verdeckt, selbst bewerten. Fünf Stufen von „sofort nochmal" bis „nach einem Monat".
- **Eigene Aufnahmen** — Wörter und Sätze selbst einsprechen und verschriftlichen. Mehrere Sprecher lassen sich getrennt halten und einzeln zu- oder abschalten; eine vertraute Stimme ist eine andere Übung als eine fremde.
- **Störgeräusch** — ein Loop unter die Sprache legen und den Abstand in Dezibel einstellen (−5 bis +20 dB). Ein Businnenraum ist mitgeliefert; eigene Geräusche lassen sich aufnehmen oder als WAV importieren.
- **Trainingsstufen** — Einfach, Schwierig, Fortgeschritten als Ein-Tipp-Voreinstellungen des Störgeräusch-Paars.
- **Kanalwahl** — links, rechts, beide, wirksam im Stereo-Kopfhörer-Setup. Über ein Bluetooth-Hörgerät wird Stereo mono summiert; die App erkennt das und zeigt die Wahl dort als unwirksam.
- **Sicherung** — eigene Aufnahmen, eigene Geräusche und der Sitzungsverlauf als ZIP in die eigenen Dokumente, auf Tastendruck.
- **Deutsch oder Englisch** — die Oberfläche gibt es in beiden Sprachen, umschaltbar direkt auf dem Startbildschirm; ohne eigene Wahl folgt sie der Gerätesprache. Eine Kurzanleitung liegt daneben und folgt derselben Einstellung ([ADR-0015](docs/adr/0015-ui-lokalisierung.md)).
- **Trainingssprache getrennt davon** — was geübt wird, wählst du in den Einstellungen. Deutsch trainieren und die App auf Englisch lesen ist eine zulässige Kombination. Eigene Aufnahmen bekommen beim Anlegen eine Sprache; sie sagt, wo der Eintrag erscheint, nicht was darin gesprochen wird ([ADR-0016](docs/adr/0016-korpus-sprache.md)).

Mitgeliefert sind 72 Wörter und Sätze auf Deutsch — 68 aus einer freien Sprachsynthese, vier von echten Stimmen eingesprochen — sowie 20 englische Beispiele und ein Störgeräusch aus einem Bus.

## Lizenz

Der **Code** steht unter Apache-2.0, siehe [LICENSE](LICENSE).

Die **Inhalte** (Audiodateien, Korpustexte) stehen ausdrücklich außerhalb der Codelizenz und tragen ihre eigene Herkunftsangabe — so festgelegt in [ADR-0014](docs/adr/0014-veroeffentlichung-lizenz.md). Was ausgeliefert wird, muss weitergebbar sein:

| Inhalt | Herkunft | Weitergabe |
| --- | --- | --- |
| 68 synthetische Aufnahmen (`voiceId: thorsten`) | Lokal erzeugt mit [Piper](https://github.com/rhasspy/piper), Stimme `de_DE-thorsten-medium` — Modell MIT, Datensatz [Thorsten-Voice](https://github.com/thorstenMueller/Thorsten-Voice) CC0 | CC0-1.0 |
| 20 synthetische Aufnahmen (`voiceId: ljspeech`) | Lokal erzeugt mit Piper, Stimme `en_US-ljspeech-high` — Modell MIT, Datensatz [LJ Speech](https://keithito.com/LJ-Speech-Dataset/) gemeinfrei | CC0-1.0 |
| 4 Demo-Einsprachen (`voiceId: stephan`, `grete`) | Eigene Aufnahmen, in der App eingesprochen | CC0-1.0; Einverständnis der zweiten Sprecherin liegt dem Autor vor (2026-08-17) |
| Satz-Einträge (`satz-*`) | Frei paraphrasiert nach Douglas Adams, „Per Anhalter durch die Galaxis", Kap. 1 — keine wörtlichen Zitate ([ADR-0009](docs/adr/0009-satz-korpus-modell.md)) | eigener Text |
| Englische Einträge (`en-*`, `satz-en-*`) | Frei formuliert, keine Übersetzungen und ohne Vorlage | eigener Text |
| Gebündeltes Störgeräusch (`files/noise/bus.wav`) | Eigene Aufnahme des Autors, Businnenraum | CC0-1.0 |

Fremdlizenzierte Audiodateien gehören nicht in dieses Repository. Drei zugekaufte Störgeräusch-Loops sind aus genau diesem Grund im August 2026 entfernt worden.

## Stack

Kotlin Multiplatform + Compose Multiplatform ([ADR-0001](docs/adr/0001-tech-stack-kmp-compose.md)):

| Ziel | Zweck |
| --- | --- |
| Android (minSdk 29) | Primäre Zielplattform, Testgerät Galaxy A53 / Android 16 |
| Desktop (JVM) | Entwicklungs- und Verifikations-Target, kein Emulator nötig |
| iOS | Option, Modulschnitt vorbereitet, nicht aktiviert (braucht macOS-Host) |

Abhängigkeiten: Kotlin/kotlinx, AndroidX (Activity, Room, SQLite), Compose Multiplatform. Keine Play Services, kein Firebase, keine Analytics, keine Werbung.

## Build & Run

```bash
./gradlew :core:jvmTest              # Unit-Tests der Kernlogik
./gradlew :composeApp:run            # Desktop-App starten
./gradlew :composeApp:assembleDebug  # Android-APK bauen
./gradlew build                      # alles
```

Voraussetzungen: JDK 21 (installiert, nicht heruntergeladen — es gibt bewusst kein Toolchain-Provisioning-Plugin), Android SDK mit Pfad in `local.properties`.

Ein frischer Klon baut vollständig: Die Korpus-Audios sind versioniert. Neu erzeugen lassen sie sich mit

```bash
cd tools
uv run python -m piper.download_voices --download-dir voices \
  de_DE-thorsten-medium en_US-ljspeech-high
uv run generate_tts.py        # rendert nur, was fehlt; --force erzwingt alles
```

Details: [tools/generate_tts.py](tools/generate_tts.py), [ADR-0006](docs/adr/0006-audioquelle-tts.md).

## Projektnavigation

- Verbindlicher Arbeitsmodus: [AGENTS.md](AGENTS.md)
- Konzept/Vision: [docs/konzept/AudioLex-Konzept.md](docs/konzept/AudioLex-Konzept.md)
- Architektur: [docs/architecture.md](docs/architecture.md)
- Entscheidungen: [docs/adr/](docs/adr/)
- Aufgaben: [docs/backlog.md](docs/backlog.md)
- Umsetzungsjournal: [docs/implementation-log.md](docs/implementation-log.md)
- Weg zur F-Droid-Aufnahme: [docs/fdroid-anmeldung.md](docs/fdroid-anmeldung.md)
