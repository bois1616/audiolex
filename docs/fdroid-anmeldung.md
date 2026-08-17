# AudioLex bei F-Droid anmelden

Arbeitsanleitung für den Weg von „privates Repo, alle Rechte vorbehalten" zu „im F-Droid-Katalog". Neun Schritte, jeder mit dem, was dabei entsteht, und mit dem Befehl, der belegt, dass er wirklich fertig ist.

**Stand 2026-08-17.** Die F-Droid-Seite ist gegen die aktuelle Doku geprüft (Quellen unten), die AudioLex-Seite gegen das Repo. Was hier steht, ist nicht aus dem Gedächtnis übernommen — das war die Auflage aus ADR-0014.

**Stand der Umsetzung (v0.33.0, 2026-08-17):** Sechs der neun Schritte sind erledigt — der Autor hat die Deployment-Vorbereitung beauftragt. Was übrig bleibt, steht am Ende unter „Was nur der Autor kann"; jeder Schritt unten trägt seinen Zustand in der Überschrift.

## Wie die Aufnahme funktioniert

F-Droid nimmt keine fertige APK entgegen. Der Buildserver klont das öffentliche Repository, baut aus dem Quelltext und signiert das Ergebnis mit F-Droids Schlüssel. Daraus folgt fast alles Weitere: Der Quelltext muss öffentlich und frei lizenziert sein, jede Version braucht einen Git-Tag, und der Build muss ohne Zutun durchlaufen — auf einer Maschine, die kein `local.properties` kennt und keinen Korpus danebenliegen hat.

Der Antrag selbst ist ein Merge Request an das Repository `fdroiddata`, der genau eine Datei hinzufügt: die Build-Rezeptur `metadata/de.hexenwoche.audiolex.yml`. Alles andere — Beschreibungen, Screenshots, Änderungstexte — lebt im eigenen Repo und wird von dort eingesammelt.

## Was heute schon trägt

Im Repo geprüft, ohne Handlungsbedarf:

| Punkt | Stand |
| --- | --- |
| Abhängigkeiten | Kotlin/kotlinx, AndroidX (Activity, Room, SQLite), Compose Multiplatform — alle Apache-2.0, alle aus Maven Central bzw. Google Maven. Keine Play Services, kein Firebase, keine Analytics. |
| Berechtigungen | Genau eine: `RECORD_AUDIO`. Keine `INTERNET`-Berechtigung. |
| Datensparsamkeit | `allowBackup="false"` (ADR-0013), keine Tracker, keine Werbung, kein Konto. Anti-Feature-Kandidaten sind damit keine in Sicht. |
| Störgeräusche | Seit v0.32.0 keine fremdlizenzierten Loops mehr im Projekt (ADR-0010 Nachtrag). |
| Stimmlizenz | Piper-Modell `de_DE-thorsten-medium` unter MIT, Datensatz Thorsten-Voice unter CC0. Die erzeugten Korpus-WAVs sind frei weitergebbar. |
| Versionierung | `AppVersion.kt` ist die einzige Quelle für `versionName`/`versionCode`, Gradle liest sie per Regex. Passt zu F-Droids `UpdateCheckMode: Tags`. |
| Release-Build | Kein `signingConfig` — `assembleRelease` liefert eine unsignierte APK. Genau das erwartet der Buildserver. |

## Die neun Schritte

### Schritt 1 — LICENSE-Datei einsetzen · **erledigt**

Ohne Lizenzdatei gilt „alle Rechte vorbehalten", und damit ist die Aufnahme ausgeschlossen. Die Wahl ist getroffen (Apache-2.0, ADR-0014 Punkt 2), es fehlt der Vollzug.

Deliverable: `LICENSE` im Repo-Root mit dem unveränderten Apache-2.0-Text, Copyright-Zeile auf Stephan Reindl. Dazu ein Absatz in der README, der die Inhalte ausdrücklich ausnimmt — der Korpus und die Aufnahmen stehen außerhalb der Codelizenz und tragen ihre eigene Herkunftsangabe (so hält es ADR-0014 fest, und `files/corpus/README.md` tut es bereits informell).

**Erledigt 2026-08-17:** `LICENSE` liegt im Root — Debians kanonischer Apache-2.0-Text (`/usr/share/common-licenses/Apache-2.0`), unverändert bis auf die Copyright-Zeile („Copyright 2026 Stephan Reindl"); ein `diff` gegen das Original belegt, dass sonst kein Zeichen abweicht. Die README hat einen Abschnitt „Lizenz" mit einer Tabelle, die jeden ausgelieferten Inhalt einzeln aufführt. Die Rezeptur nennt `License: Apache-2.0`.

### Schritt 2 — Foojay-Plugin aus dem Build nehmen · **erledigt**

Der eine Blocker, den vor dieser Prüfung niemand auf der Liste hatte. `settings.gradle.kts` lädt `org.gradle.toolchains.foojay-resolver-convention`, um sich eine JDK zu beschaffen. F-Droids Scanner führt dieses Plugin als „usual suspect" und bricht den Build ab, mit der Begründung, es lade eine Java-Laufzeit aus einer nicht kontrollierten Quelle.

Der richtige Weg ist, es zu entfernen, nicht es per `scanignore` wegzudrücken: Der Buildserver bringt eine JDK mit, und `jvmToolchain(21)` in `:core`/`:composeApp` löst dann gegen die auf. Was lokal passiert, ist zu prüfen — die Entwicklungsmaschine hatte laut Kommentar im Skript nur eine JRE, deshalb stand das Plugin überhaupt dort. Inzwischen liegt unter `/usr/lib/jvm/java-21-openjdk-amd64` eine JDK 21, das Argument von damals ist also womöglich hinfällig.

Falls die Aufnahme über einen älteren Tag laufen soll, in dem das Plugin noch steckt, kennt F-Droid den Notausgang `prebuild: sed -i -e '/foojay/d' ../settings.gradle.kts` in der Rezeptur. Für einen frischen Tag ist das unnötiger Ballast.

**Erledigt 2026-08-17:** Der `plugins { }`-Block in `settings.gradle.kts` ist ganz weg, `jvmToolchain(21)` löst gegen die installierte JDK 21 auf, `./gradlew build` bleibt grün. Das Argument von damals („der Host hat nur eine JRE") war überholt.

**Ein Fund beim Nachprüfen, der Geld gekostet hätte:** Nach dem Entfernen stand die Plugin-Kennung noch im erklärenden **Kommentar** derselben Datei. F-Droid sucht sie als Text in Gradle-Dateien — der Kommentar hätte denselben Abbruch ausgelöst wie das Plugin selbst. Sie steht jetzt nur noch in dieser Anleitung, die keine Gradle-Datei ist. Wer das Plugin je wieder erwähnt, tut es außerhalb von `*.gradle.kts`.

### Schritt 3 — Die Korpus-Audios in den Quelltext bringen · **erledigt**

Hier liegt die zweite offene Stelle, und sie braucht einen Entscheid des Autors, weil sie einen Punkt aus ADR-0014 korrigiert.

Der Befund: `git ls-files` trackt genau eine WAV-Datei, die Loader-Testfixture. Die 68 Korpus-Aufnahmen sind gitignoriert. Ein Buildserver, der nur den Quelltext hat, baut also eine App mit Metadaten ohne Ton. ADR-0014 löst das mit „TTS zur Build-Zeit erzeugen". Das trägt so nicht: F-Droid erlaubt vorgebaute Binärteile nur aus einer festen Quellenliste — Debian main, Maven Central, Google Maven, OSS Sonatype, OSS JFrog, JitPack, Clojars, dazu Android-/Flutter-SDK, PyPI-Wheels, Nix, Rust, Go, Node. Ein 63 MB großes Stimmodell von Hugging Face steht dort nicht, und ein Buildschritt, der es zieht, ist genau die Art Download, gegen die die Regel geschrieben ist.

Der gangbare Weg ist der andere: die WAVs lokal mit `tools/generate_tts.py` erzeugen und einchecken. Sie sind aus einem MIT-Modell und einem CC0-Datensatz entstanden, also frei weitergebbar — und F-Droids Aufnahmepolitik behandelt Audio als nicht-funktionales Asset, für das ohnehin der größere Spielraum gilt, solange die Weiterverbreitung erlaubt ist. Der Umfang ist harmlos: 68 Dateien, zusammen 3,6 MB. Dafür fällt der `.gitignore`-Eintrag für `files/corpus/**/*.wav`, und das alte `[PROP]`-Git-LFS-Item wird gegenstandslos.

Zwei Dinge gehören dann in `files/corpus/README.md`: die Herkunft der Stimme (Modell MIT, Datensatz CC0, Link auf Thorsten-Voice) und der Hinweis, dass die Satz-Einträge frei paraphrasiert sind (ADR-0009) — beides ist heute schon halb dokumentiert und wird mit der Veröffentlichung zur Pflichtangabe.

**Autor-Entscheid 2026-08-17: einchecken.** Erledigt in v0.33.0 — 68 WAVs im Index, die `.gitignore`-Ausnahme gestrichen (mit Begründung an ihrer Stelle), Herkunft in `files/corpus/README.md` als Tabelle: Sprecher, Erzeugung, Weitergaberecht. Ein Eintrag ohne Herkunftszeile gilt dort ausdrücklich als Release-Blocker.

Belegt statt behauptet: Der Index wurde per `git checkout-index --prefix` in ein leeres Verzeichnis exportiert — ohne `local.properties`, ohne die Autor-Bibliothek, also die Lage des Buildservers — und dort mit `ANDROID_HOME` aus der Umgebung gebaut. `assembleRelease` läuft durch, das APK trägt 68 Korpus-WAVs und keinen der alten Loops.

**Was hier noch offen ist:** die eigenen Einsprachen (Autor, Grete) als Beispiel-Kontingent. Format, die drei Bedingungen und die JSON-Einträge stehen in `files/corpus/README.md`; die Dateien selbst liegen nur auf dem Testgerät.

### Schritt 4 — Repository öffentlich machen und Tags setzen · **beim Autor**

`origin` ist `git@github.com:bois1616/audiolex.git` und privat. F-Droid braucht ein öffentlich lesbares Repository und je Version einen Tag, den die Rezeptur referenzieren kann; heute leben die Versionen nur in Commit-Messages.

Konvention: `v<VERSION_NAME>`, also `v0.32.0`. Der Tag gehört an denselben Commit wie der Versions-Bump aus DoD §6, damit „was zeigt die App an" und „was hat F-Droid gebaut" dieselbe Antwort haben. Signierte Tags (`git tag -s`) sind kein Muss, aber der Buildserver kann sie prüfen, und es kostet einmal Einrichtung.

Vor dem Öffentlichmachen einmal durch die Historie schauen: Telefonnummer und E-Mail aus dem Impressum stehen im Quelltext (`ImpressumScreen.kt`) — das ist eine bewusste Angabe und bleibt, aber es sollte eine bewusste bleiben, wenn plötzlich Fremde mitlesen.

Deliverable: öffentliches Repo, Tag `v0.32.0` (oder die Version, die veröffentlicht wird).

### Schritt 5 — Impressum und README auf „veröffentlicht" umstellen · **erledigt**

Zwei Texte behaupten heute das Gegenteil dessen, was nach der Aufnahme gilt.

Im Impressum steht „Privates, nicht-kommerzielles Projekt ohne öffentlichen Vertrieb." Der zweite Halbsatz wird mit der Veröffentlichung falsch. Nicht-kommerziell bleibt richtig und darf stehen bleiben. Die Datenschutz-Absätze daneben stimmen unverändert und sind besser als das, was die meisten Store-Einträge bieten — sie bleiben, wie sie sind.

In der README steht „Status: Gerüstphase (M0)", was seit rund dreißig Versionen nicht mehr zutrifft, und die Bau-Anleitung erklärt einem Fremden nichts über die App. Für F-Droid ist die README kein Pflichtdokument, aber sie ist das Erste, was ein Prüfer sieht.

**Erledigt 2026-08-17:** Der Impressum-Satz heißt jetzt „Nicht-kommerzielles Projekt. Der Quelltext ist offen (Apache-2.0), die App wird ohne finanzielle Interessen bereitgestellt." Die README ist für Fremde umgeschrieben: was die App tut, für wen, die Inhalte-Lizenztabelle, und der Bauteil dahinter statt davor.

### Schritt 6 — Store-Metadaten im eigenen Repo anlegen · **erledigt**

F-Droid liest Beschreibungen und Bilder aus dem App-Repository, im Fastlane-Layout. Zwei Sprachen sind sinnvoll: `en-US` ist die Vorgabe und wird angezeigt, wenn nichts Passendes da ist; `de-DE` ist die Sprache der App.

```text
fastlane/metadata/android/en-US/short_description.txt   max 80 Zeichen
fastlane/metadata/android/en-US/full_description.txt    max 4000 Zeichen
fastlane/metadata/android/en-US/title.txt               max 50 Zeichen
fastlane/metadata/android/en-US/images/icon.png         512×512
fastlane/metadata/android/en-US/images/phoneScreenshots/1.png …
fastlane/metadata/android/en-US/changelogs/41.txt       max 500 Zeichen, Dateiname = versionCode
fastlane/metadata/android/de-DE/…                       dieselbe Struktur
```

**Erledigt 2026-08-17:** Die Struktur steht für `de-DE` und `en-US`, mit Titel, Kurz- und Langbeschreibung, Änderungstext für versionCode 41 und Icon. Zeichenzahlen nachgezählt: Kurzbeschreibung 76 bzw. 79 von 80 erlaubten, Langbeschreibung 2525 bzw. 2249 von 4000, Änderungstext 325 bzw. 315 von 500.

Zum Icon: Es existierte nur als Vektor (adaptive icon). Die Pfade sind ein gefüllter Punkt und drei rechte Halbkreis-Bögen mit runden Enden — ImageMagicks interner SVG-Renderer verwarf sie, also wurde das Icon analytisch gerastert (`icon.png`, 512×512, 4×4-Supersampling, innerer 72er-Bereich des 108er-Canvas, den eine Launcher-Maske zeigt). Das Skript dazu ist keine Projektdatei; wird das Icon geändert, ist es in einer Viertelstunde neu geschrieben oder besser gleich in Inkscape gezeichnet.

Screenshots kommen am ehrlichsten vom Testgerät:

```bash
export ANDROID_SERIAL=192.168.178.24:<Port>
adb exec-out screencap -p > 1.png
```

Vier Bilder reichen und sollten zeigen, worum es geht: Startbildschirm, Lernmodus mit sichtbarem Wort, Prüfmodus mit verdeckter Karte, Einstellungen mit Störgeräusch und Trainingsstufe. Bilder mit echten Inhalten, nicht mit leeren Listen. Ablage: `fastlane/metadata/android/de-DE/images/phoneScreenshots/1.png` und so weiter, dieselben Dateien nach `en-US/` kopiert (die Sprache der Oberfläche ist ohnehin Deutsch).

Die tatsächlichen Texte liegen in den Dateien; die Entwürfe unten im Abschnitt „Textvorlagen" sind ihre Quelle und bleiben zum Nachlesen stehen.

### Schritt 7 — Den Build so testen, wie F-Droid ihn testet · **halb erledigt**

Ein grüner lokaler Build sagt wenig über einen Buildserver, der weder `local.properties` noch die Autor-Bibliothek kennt. Zwei Proben:

```bash
# 1. Frischer Klon aus dem öffentlichen Repo, nichts danebengelegt
git clone https://github.com/bois1616/audiolex.git /tmp/fdroid-probe && cd /tmp/fdroid-probe
./gradlew :composeApp:assembleRelease

# 2. F-Droids eigene Werkzeuge, im Container
git clone --depth=1 https://gitlab.com/fdroid/fdroidserver ~/fdroidserver
docker run --rm -itu vagrant --entrypoint /bin/bash \
  -v ~/fdroiddata:/build:z -v ~/fdroidserver:/home/vagrant/fdroidserver:Z \
  registry.gitlab.com/fdroid/fdroidserver:buildserver
# im Container:
fdroid readmeta && fdroid rewritemeta de.hexenwoche.audiolex \
  && fdroid lint de.hexenwoche.audiolex && fdroid build -v -l de.hexenwoche.audiolex
```

**Probe 1 ist gelaufen (2026-08-17), in der Variante, die ohne öffentliches Repo auskommt:** `git checkout-index -a --prefix=<leeres Verzeichnis>` exportiert genau den Repository-Stand, `ANDROID_HOME` aus der Umgebung ersetzt das fehlende `local.properties`, `./gradlew :composeApp:assembleRelease --no-daemon` läuft durch. Damit sind zwei der drei Vermutungen erledigt: Der Desktop-Zielteil des KMP-Moduls stört den Android-Release-Build nicht, und KSP/Room laufen durch. Nach Schritt 4 ist derselbe Lauf mit einem echten `git clone` zu wiederholen — der Unterschied ist gering, aber er kostet nichts.

**Probe 2 ist offen** und braucht Docker: Ob die Gradle-Version aus dem Wrapper (8.11.1) in F-Droids Buildumgebung vorhanden ist, ob `fdroid lint` etwas an der Rezeptur auszusetzen hat und ob der Scanner noch irgendwo anschlägt, beantwortet nur `fdroid build`. Der Lauf lohnt sich vor dem Merge Request, nicht danach — im Merge Request ist ein durchgefallener Build öffentlich.

### Schritt 8 — Die Rezeptur schreiben · **erledigt**

Eine Datei, `metadata/de.hexenwoche.audiolex.yml`, im `fdroiddata`-Fork. **Erledigt 2026-08-17:** Sie liegt fertig unter [`fdroid/metadata/de.hexenwoche.audiolex.yml`](../fdroid/metadata/de.hexenwoche.audiolex.yml) — dort als pflegbare Kopie, die der eigene Build nicht liest; hineingehört sie in den Fork. Inhalt:

```yaml
Categories:
  - Science & Education
  - Sports & Health
License: Apache-2.0
AuthorName: Stephan Reindl
AuthorEmail: audiolex26@proton.me
SourceCode: https://github.com/bois1616/audiolex
IssueTracker: https://github.com/bois1616/audiolex/issues

RepoType: git
Repo: https://github.com/bois1616/audiolex.git

Builds:
  - versionName: 0.32.0
    versionCode: 35
    commit: v0.32.0
    subdir: composeApp
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 0.33.0
CurrentVersionCode: 36
```

`gradle: - yes` heißt „bauen, ohne Flavor" — das Projekt hat keine. `subdir: composeApp` zeigt auf das App-Modul, `:core` zieht Gradle selbst mit. `UpdateCheckMode: Tags` zusammen mit `AutoUpdateMode: Version` heißt: Ein neuer Tag `v0.34.0` erzeugt automatisch einen neuen Build-Eintrag, ohne dass jemand die Rezeptur anfasst. Genau dafür lohnt sich die Tag-Disziplin aus Schritt 4.

`AntiFeatures` bleibt leer, solange nichts Unfreies mitreist. Die Liste, gegen die ein Prüfer abgleicht: Ads, KnownVuln, NonFreeAdd, NonFreeAssets, NonFreeDep, NonFreeNet, NoSourceSince, NSFW, TetheredNet, Tracking. `NonFreeAssets` wäre der Eintrag gewesen, den die alten Störgeräusch-Loops eingebracht hätten.

Zur Signatur: F-Droid signiert mit eigenem Schlüssel, das ist der Normalfall und für den Anfang das Richtige. Reproduzierbare Builds mit `AllowedAPKSigningKeys` (die App wird dann mit dem Schlüssel des Autors ausgeliefert und F-Droid verifiziert nur) gelten als gute Praxis, verlangen aber Bit-Gleichheit zwischen zwei Builds und kosten Einrichtung. Der Preis dafür, das später zu ändern: Nutzer können nicht von einer F-Droid-signierten auf eine anders signierte Version aktualisieren, sie müssen neu installieren. Wenn es also je passieren soll, dann besser vor der ersten Veröffentlichung als danach.

### Schritt 9 — Merge Request stellen · **beim Autor**

Der Autor hat mit GitLab bisher nicht gearbeitet (2026-08-17), deshalb hier der Weg in der Reihenfolge, in der man ihn klickt. GitLab ist GitHub sehr ähnlich; die Begriffe unterscheiden sich an einer Stelle: Was auf GitHub „Pull Request" heißt, heißt hier **Merge Request**, kurz MR.

**a) Konto und Fork.** Auf `gitlab.com` registrieren (kostenlos, E-Mail-Bestätigung). Dann `https://gitlab.com/fdroid/fdroiddata` öffnen und oben rechts auf **Fork** klicken → eigener Namensraum als Ziel, Sichtbarkeit öffentlich. Der Fork ist eine eigene Kopie des Rezeptur-Repositories unter dem eigenen Konto; darin darf man alles, ohne bei F-Droid etwas anzurichten. Achtung, das Repository ist groß (mehrere Tausend Rezepturen) — der Fork dauert ein paar Minuten.

**b) SSH-Schlüssel hinterlegen**, sonst fragt jeder Push nach Zugangsdaten. Der Schlüssel vom GitHub-Konto lässt sich wiederverwenden: `cat ~/.ssh/id_ed25519.pub` (oder `id_rsa.pub`) und den Inhalt unter *Profil → Preferences → SSH Keys* einfügen.

**c) Fork klonen und den Zweig anlegen.** Der Zweigname ist Vorschrift: er heißt wie die App-ID.

```bash
git clone git@gitlab.com:<dein-gitlab-name>/fdroiddata.git ~/fdroiddata
cd ~/fdroiddata
git checkout -b de.hexenwoche.audiolex
cp ~/projects/audiolex/fdroid/metadata/de.hexenwoche.audiolex.yml metadata/
git add metadata/de.hexenwoche.audiolex.yml
git commit -m "New App: de.hexenwoche.audiolex"
git push origin de.hexenwoche.audiolex
```

**d) Pipeline abwarten.** Der Push startet automatisch eine Prüfung (*Build → Pipelines* im Fork). Sie muss grün sein — sie prüft Syntax und Lint der Rezeptur, nicht den App-Build. Rot heißt: Meldung lesen, Datei korrigieren, erneut committen und pushen. Das ist kein Fehlversuch mit Publikum, der Fork ist der Übungsplatz.

**e) Merge Request stellen.** GitLab bietet nach dem Push einen Link „Create merge request" an; alternativ *Code → Merge requests → New*. Quelle ist der eigene Zweig, Ziel `fdroid/fdroiddata` mit `master`. Die Vorlage im Beschreibungsfeld **vollständig** ausfüllen — sie fragt genau die Dinge ab, die ein Paketierer sonst nachfragen muss. „Squash commits" ist voreingestellt und bleibt an.

**f) Antworten.** Ein Paketierer schaut es sich an und stellt Rückfragen im MR; man bekommt sie per E-Mail. Zügig antworten hilft, weil ein MR ohne Antwort irgendwann liegen bleibt.

**Zur KI-Frage, weil sie beim Autor Sorge ausgelöst hat** (geprüft 2026-08-17): F-Droids Aufnahmepolitik erwähnt KI-generierten Code **nicht** — sie handelt von Lizenz, freier Werkzeugkette, fehlenden proprietären Blobs und Baubarkeit aus dem Quelltext. Wie der Code entstanden ist, gehört nicht zu den Kriterien, es gibt also keine Regel, an der AudioLex scheitern könnte. Zwei Einordnungen dazu, damit die Lage nicht besser klingt als sie ist: Erstens ist das ein bewegtes Thema — andere Projekte (GCC, SDL) haben für ihre **eigenen** Codebasen Regeln gegen KI-Beiträge erlassen, und ob F-Droid dergleichen einführt, wird diskutiert; das betrifft aber Upstreams, die Beiträge annehmen, nicht einen Distributor, der fertige Projekte aufnimmt. Zweitens: geprüft ist die veröffentlichte Policy von heute, nicht die von morgen. Empfehlung: Die Entstehung im Merge Request offen ansprechen, statt sie unerwähnt zu lassen — das Repository macht sie über ADRs und Journal ohnehin sichtbar, und im Impressum der App steht sie auch. Wer es selbst sagt, verhandelt; wer es findet, prüft.

Was dabei nicht passieren kann: Etwas kaputtmachen. Schreibrechte am echten `fdroiddata` hat man nicht — der MR ist ein Vorschlag, den jemand annimmt oder nicht.

Regeln aus dem Beitragsleitfaden, die man sonst zweimal macht: Branch heißt wie die App-ID, nicht auf `master` committen, keinen Merge Request aus einem geschützten Branch öffnen, Commits squashen, die Vorlage im Merge Request vollständig ausfüllen. Die CI-Pipeline des Forks muss grün sein, bevor jemand draufschaut.

Danach beantwortet man Rückfragen des Paketierers. Nach dem Merge dauert es etwa 24 bis 48 Stunden, bis die App im Katalog auftaucht — die Signatur passiert nicht vollautomatisch. Baulogs: `https://monitor.f-droid.org/builds/`.

Alternative, falls der Autor die Rezeptur nicht selbst schreiben will: ein Antrag unter `https://gitlab.com/fdroid/rfp/issues`. Dann macht es jemand anderes, dauert aber länger und man gibt die Kontrolle über die Rezeptur ab.

## Deliverables auf einen Blick

| Was | Wo | Zustand |
| --- | --- | --- |
| `LICENSE` (Apache-2.0) | Repo-Root | **erledigt** |
| 68 Korpus-WAVs im Index | `files/corpus/raw/de-DE/` | **erledigt** |
| Toolchain-Plugin entfernt | `settings.gradle.kts` | **erledigt** |
| Öffentliches Repository | GitHub | **Autor** |
| Git-Tag je Version (`v0.33.5`) | Repo | **Autor** |
| `short_description.txt` (≤80) | `fastlane/metadata/android/<locale>/` | **erledigt**, de + en |
| `full_description.txt` (≤4000) | dito | **erledigt**, de + en |
| `title.txt` (≤50) | dito | **erledigt** |
| `images/icon.png` (512×512) | dito | **erledigt** |
| `changelogs/36.txt` (≤500) | dito | **erledigt**, de + en |
| Korrigierter Impressum-Satz | `ImpressumScreen.kt` | **erledigt** |
| Rezeptur `de.hexenwoche.audiolex.yml` | `fdroid/metadata/`, gehört in den Fork | **erledigt**, einsetzen bleibt |
| Vier Screenshots | `images/phoneScreenshots/` | **erledigt** — vom A53, alle aus Build 0.33.5 |
| Gebündeltes Störgeräusch (Bus) | `files/noise/bus.wav` + `noise.json` | **erledigt** — vom Gerät geholt, unbearbeitet |
| Lizenz der eigenen Aufnahmen | README-Tabelle | **erledigt** — CC0-1.0 (Autor-Entscheid) |
| Datenschutzerklärung unter Web-Adresse | — | **nicht nötig** — das verlangt Google Play, nicht F-Droid |

Für die „Neu"-Liste im Client braucht ein Eintrag: Name, Icon, Kurz- und Langbeschreibung, Lizenz, mindestens einen Änderungstext, mindestens ein Bild und mindestens eine Übersetzung. Ohne diese Teile wird die App aufgenommen, taucht aber prominent nirgends auf.

## Textvorlagen

Diese Entwürfe **stehen inzwischen als Dateien** unter `fastlane/metadata/android/`; sie bleiben hier stehen, weil sich Textarbeit besser an einem Ort liest als in acht Dateien. Wer sie ändert, ändert die Dateien — nicht diesen Abschnitt. Die endgültige Langbeschreibung ist gegenüber dem Entwurf unten um Trainingsstufen, Kanalwahl und Sicherung gewachsen (2525 Zeichen von 4000 erlaubten).

**`de-DE/title.txt`**

```text
AudioLex
```

**`de-DE/short_description.txt`** (76 Zeichen)

```text
Hörtraining für Wortverständnis: hören, erkennen, wiederholen. Ganz offline.
```

**`de-DE/full_description.txt`**

```text
AudioLex trainiert das Verstehen gesprochener Wörter, wenn Hören und Verstehen auseinanderfallen: Der Schall kommt an, wird aber nicht mehr zuverlässig als Sprache erkannt. Das passiert nach einseitigem Hörverlust, und ein Hörgerät allein löst es nicht — der Weg vom Klang zum Wort will wieder geübt werden.

Zwei Modi, beide auf dasselbe Ziel:

Lernmodus — ein Wort wird gespielt, der Text steht dabei. Das baut die Verbindung zwischen dem, was ankommt, und dem, was es bedeutet.

Prüfmodus — das Wort wird gespielt, die Karte bleibt verdeckt. Man entscheidet selbst, wie gut es saß. Aus dieser Bewertung ergibt sich, wann das Wort wiederkommt: nach einer Minute, nach zehn, nach einem Tag, einer Woche oder einem Monat.

Mitgeliefert sind 68 Wörter und Sätze in deutscher Sprache, erzeugt mit einer freien Sprachsynthese. Eigene Aufnahmen kommen dazu: Wörter oder Sätze einsprechen, verschriftlichen, trainieren. Mehrere Sprecher lassen sich getrennt halten und einzeln zu- oder abschalten — vertraute Stimmen sind eine andere Übung als eine fremde.

Wer schwerer üben will, legt ein Störgeräusch darunter und stellt den Abstand zwischen Sprache und Geräusch in Dezibel ein. Die Geräusche bringt man selbst mit, aufgenommen oder als WAV-Datei importiert; die App liefert keine mit.

Was AudioLex nicht tut: Es fordert keine Internet-Berechtigung an, hat keine Konten, keine Tracker, keine Werbung. Wortschatz, Bewertungen und Sitzungsverlauf bleiben auf dem Gerät. Eine Sicherung schreibt beides auf Tastendruck als ZIP-Datei in die eigenen Dokumente — was danach damit passiert, entscheidet man selbst.

AudioLex ist ein Übungswerkzeug, kein medizinisches Produkt. Es ersetzt weder den Hörgeräteakustiker noch die HNO-Abklärung.

Die App ist ursprünglich für einen einzelnen Menschen entstanden, der genau dieses Problem hat. Das merkt man ihr an: Sie ist auf Deutsch, sie ist nüchtern, und sie erklärt sich nicht von selbst.
```

**`de-DE/changelogs/35.txt`** (274 Zeichen)

```text
Die drei mitgelieferten Störgeräusche sind entfallen. Ihre Lizenzen erlaubten keine Weitergabe, und ein Übungswerkzeug soll keine Rechtsfragen mitliefern. Störgeräusche nimmt man ab sofort selbst auf oder importiert sie als WAV-Datei — Einstellungen, Abschnitt Störgeräusch.
```

**`en-US/short_description.txt`** (79 Zeichen)

```text
Hearing training for word recognition: listen, identify, repeat. Fully offline.
```

Für `en-US/full_description.txt` genügt eine Übersetzung des deutschen Texts. Kürzen ist erlaubt — der englische Eintrag ist die Rückfallebene, nicht die Hauptsache.

## Was nur der Autor kann

Stand 2026-08-17, nachdem er die Gerätedaten geliefert und die offenen Entscheide getroffen hat. Übrig sind drei Dinge.

**1. Das Repository öffentlich machen und taggen.** GitHub-Sichtbarkeit umstellen, `git tag -a v0.33.5`, pushen. Vorher der Blick, ob alle lokalen Commits hinaus sollen. Im Impressum steht jetzt nur noch die E-Mail-Adresse, keine Telefonnummer mehr (Autor-Entscheid) — F-Droid selbst verlangt kein Impressum; ob österreichisches Recht für eine nicht-kommerzielle App mehr fordert, ist eine Frage an jemanden mit Zulassung, nicht an diese Anleitung.

**2. GitLab-Konto, Fork, Merge Request** — Schritt 9 führt das für jemanden durch, der es noch nie gemacht hat. Ein Konto kann ich nicht anlegen und keinen Antrag in seinem Namen stellen.

**3. Die Abnahme am Hörgerät.** Zwei Punkte konkret: Klingt das Bus-Geräusch bei „Fortgeschritten" (SNR −5 dB) noch sauber? Gemessen laufen dort 1,7 % der Samples in die Begrenzung des Mixers — ob man das hört, sagt nur das Ohr. Und: Trägt der Loop unter den Wörtern, oder störten Lücken am Anfang? (Gemessen liegt der Pegel gleichmäßig, aber gemessen ist nicht gehört.) Falls es knistert, ist ein Limiter-Durchgang auf `bus.wav` die Antwort.

Zwei Kleinigkeiten, die keine Blocker sind:

- **Die lokalen Doubletten auf dem A53 sind aufgeräumt** (2026-08-17): Die vier eigenen Aufnahmen und das eigene Bus-Geräusch waren identisch mit dem, was jetzt mitgeliefert wird, und sind nach Autor-Entscheid vom Gerät entfernt worden — Sicherung liegt im Sitzungsverzeichnis. Auf fremden Geräten konnte die Doppelung nie auftreten.
- **Akzent der Demo-Einsprachen:** Sie sind als `locale: de-DE` eingetragen, weil sich das am Schreibtisch nicht feststellen lässt. Sind sie österreichisch gefärbt, gehört `de-AT` hinein (und die Dateien in ein `raw/de-AT/`).

Erledigt und damit nicht mehr auf dieser Liste: LICENSE, Toolchain-Plugin, 72 Korpus-Audios im Index, gebündeltes Bus-Geräusch, Demo-Einsprachen samt Lizenz und Einverständnis, Impressum, README, Store-Texte, Icon, vier Screenshots vom Gerät, Rezeptur, Build-Probe aus dem reinen Repository-Stand.

Was **ich** noch beisteuern kann: `fdroid lint`/`fdroid build` im Container (Schritt 7, Probe 2) — dafür braucht es Docker; und die Rezeptur auf den Tag anpassen, der am Ende wirklich veröffentlicht wird.

## Quellen

Alle am 2026-08-17 abgerufen.

- Aufnahmepolitik, erlaubte Binärquellen, Assets: `https://f-droid.org/docs/Inclusion_Policy/`
- Ablauf und Merge Request: `https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/`
- Checkliste für Autoren, Signaturfrage: `https://f-droid.org/docs/Inclusion_How-To/`
- Metadatenstruktur, Zeichenzahlen, erlaubtes Markup: `https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/`
- Felder der Rezeptur: `https://f-droid.org/docs/Build_Metadata_Reference/`
- Reproduzierbare Builds, `AllowedAPKSigningKeys`: `https://f-droid.org/docs/Reproducible_Builds/`
- Beitragsregeln: `https://gitlab.com/fdroid/fdroiddata/-/blob/master/CONTRIBUTING.md`
- Kategorienliste: `https://gitlab.com/fdroid/fdroiddata/-/blob/master/config/categories.yml`
- Foojay-Plugin als „usual suspect": `https://forum.f-droid.org/t/troubleshooting-a-failing-build-suss-json-found-usual-suspect-org-gradle-toolchains-foojay-resolver/34413`
- Lizenz der Stimme: `https://huggingface.co/rhasspy/piper-voices` (Model Card `de/de_DE/thorsten/medium`), Datensatz `https://github.com/thorstenMueller/Thorsten-Voice`
