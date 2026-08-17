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

### Schritt 4 — Repository öffentlich machen und Tags setzen · **erledigt**

`origin` ist `git@github.com:bois1616/audiolex.git` und privat. F-Droid braucht ein öffentlich lesbares Repository und je Version einen Tag, den die Rezeptur referenzieren kann.

Konvention: `v<VERSION_NAME>`, also `v0.33.5`. Der Tag gehört an denselben Commit wie der Versions-Bump aus DoD §6, damit „was zeigt die App an" und „was hat F-Droid gebaut" dieselbe Antwort haben. Signierte Tags (`git tag -s`) sind kein Muss, aber der Buildserver kann sie prüfen, und es kostet einmal Einrichtung.

**Was die Sichtbarkeit ändert und was nicht.** Öffentlich heißt Lesen für alle. Schreibrechte hat weiterhin nur, wer als Collaborator eingetragen ist; Fremde können forken und Pull Requests vorschlagen, mehr nicht. Zwei Stellen sind trotzdem einen Blick wert: *Settings → Collaborators* (muss leer sein) und GitHub Actions — ein Fork-PR kann Workflows auslösen. AudioLex hat kein `.github/`, also entfällt das hier. Die Merge-Optionen für Pull Requests vergeben keine Rechte, deren Voreinstellung kann bleiben. Issues gehören eingeschaltet, die Rezeptur verweist darauf.

**Historie bereinigt, 2026-08-17.** Veröffentlicht wird nicht der aktuelle Stand, sondern jeder Stand. Die Telefonnummer des Autors stand in **40 der 105 Commits** (eingeführt mit v0.16.0) und im aktuellen Stand außer im Impressum auch noch in Backlog und Umsetzungslog — der Entscheid „nur die E-Mail veröffentlichen" war damit nur halb umgesetzt. `git filter-repo --replace-text` hat sie in allen Commits durch `[Nummer entfernt]` ersetzt. Belege: 0 Treffer auf `master` und in den Notes danach (alle 105 Commits durchsucht), Commit-Zahl unverändert 105, und der Baum-Hash des HEAD ist derselbe wie vorher — es hat sich ausschließlich Altes geändert. Wer nachprüft, sollte `git rev-list master` nehmen, nicht `--all`: `refs/remotes/origin/*` zeigt bis zum Push weiter auf den alten Stand bei GitHub und liefert dort erwartungsgemäß Treffer. Das Zeitfenster war genau hier: noch privat, keine Forks, keine fremden Klone. Nach dem Öffentlichmachen ist derselbe Eingriff wirkungslos, weil Klone und Caches die alte Fassung behalten.

Nebenwirkung, die zu wissen ist: Alle Commit-Hashes ab v0.16.0 sind neu, der Push muss also erzwungen werden. `refs/notes/ai-attribution` (7 Notizen) hing danach an den alten Hashes und ist über die `commit-map` von `filter-repo` neu angehängt worden.

**Ausgeführt am 2026-08-17.** Der Autor hat den Force-Push gewählt:

```bash
git push --force-with-lease origin master   # 7f5cce1 -> 754a339, forced update
git push origin v0.33.5
git push origin refs/notes/ai-attribution
```

Zwei Dinge, die dabei zu lernen waren. Erstens: Der Tag-Push geht auch dann durch, wenn der Branch-Push an der umgeschriebenen Historie abprallt — Tags brauchen kein Fast-Forward. Nach dem ersten Versuch lag also die bereinigte Historie samt aller Objekte (8,17 MiB) schon bei GitHub, während `master` weiter auf den alten Stand zeigte. Zweitens: `--force-with-lease` verlangt einen aktuellen Remote-Tracking-Ref als Vergleichsgrundlage; nach `filter-repo` fehlt der, ein `git fetch` stellt ihn her (hier hatte VS Codes Autofetch das schon getan).

Die Alternative wäre gewesen, das GitHub-Repository zu löschen und gleichnamig neu anzulegen. Der Unterschied: GitHub behält nach einem Force-Push die unerreichbaren Objekte und liefert sie aus, wenn jemand den genauen Hash kennt. Diese Hashes waren nie öffentlich — praktisch also nicht auffindbar, aber „nicht auffindbar" ist ein schwächeres Ergebnis als „nicht vorhanden". Wer diesen Weg noch einmal geht, entscheidet das vor dem ersten Push, nicht danach.

**Belegt, nicht angenommen:** `git ls-remote https://github.com/bois1616/audiolex.git` **ohne Zugangsdaten** (`credential.helper` leer, `GIT_TERMINAL_PROMPT=0`) liefert `refs/heads/master`, `refs/tags/v0.33.5` und `refs/notes/ai-attribution`, alle auf `754a339` — das Repository ist öffentlich lesbar und der Tag auflösbar, genau in der Form, in der der Buildserver ihn holt. In den 106 Commits von `master` steht die Telefonnummer nicht mehr.

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

**Erledigt 2026-08-17:** Die Struktur steht für `de-DE` und `en-US`, mit Titel, Kurz- und Langbeschreibung, Änderungstext für versionCode 41 und Icon. Zeichenzahlen nachgezählt, Stand nach allen Nachträgen des Tages: Kurzbeschreibung 76 bzw. 79 von 80 erlaubten, Langbeschreibung 2599 bzw. 2309 von 4000, Änderungstext 398 bzw. 388 von 500. Die Tabelle im Abschnitt Textvorlagen führt alle acht Dateien.

Zum Icon: Es existierte nur als Vektor (adaptive icon). Die Pfade sind ein gefüllter Punkt und drei rechte Halbkreis-Bögen mit runden Enden — ImageMagicks interner SVG-Renderer verwarf sie, also wurde das Icon analytisch gerastert (`icon.png`, 512×512, 4×4-Supersampling, innerer 72er-Bereich des 108er-Canvas, den eine Launcher-Maske zeigt). Das Skript dazu ist keine Projektdatei; wird das Icon geändert, ist es in einer Viertelstunde neu geschrieben oder besser gleich in Inkscape gezeichnet.

Screenshots kommen am ehrlichsten vom Testgerät:

```bash
export ANDROID_SERIAL=192.168.178.24:<Port>
adb exec-out screencap -p > 1.png
```

Vier Bilder reichen und sollten zeigen, worum es geht: Startbildschirm, Lernmodus mit sichtbarem Wort, Prüfmodus mit verdeckter Karte, Einstellungen mit Störgeräusch und Trainingsstufe. Bilder mit echten Inhalten, nicht mit leeren Listen. Ablage: `fastlane/metadata/android/de-DE/images/phoneScreenshots/1.png` und so weiter, dieselben Dateien nach `en-US/` kopiert (die Sprache der Oberfläche ist ohnehin Deutsch).

Die tatsächlichen Texte liegen in den Dateien; die Entwürfe unten im Abschnitt „Textvorlagen" sind ihre Quelle und bleiben zum Nachlesen stehen.

### Schritt 7 — Den Build so testen, wie F-Droid ihn testet · **erledigt**

Ein grüner lokaler Build sagt wenig über einen Buildserver, der weder `local.properties` noch die Autor-Bibliothek kennt. Zwei Proben:

```bash
# 1. Frischer Klon aus dem öffentlichen Repo, nichts danebengelegt
git clone --branch v0.33.5 --depth 1 https://github.com/bois1616/audiolex.git /tmp/fdroid-probe
cd /tmp/fdroid-probe && ./gradlew :composeApp:assembleRelease --no-daemon

# 2. F-Droids eigene Werkzeuge, im Container.
#    Das Image ist reine Bauumgebung (JDK 21, SDK, gradlew-fdroid) und enthaelt
#    fdroidserver NICHT -- das wird als Checkout hineingemountet.
git clone --depth=1 --branch 2.4.5 https://gitlab.com/fdroid/fdroidserver ~/fdroidserver
docker run --rm -u vagrant -w /build \
  -v ~/fdroiddata:/build:z -v ~/fdroidserver:/home/vagrant/fdroidserver:Z \
  --entrypoint /bin/bash registry.gitlab.com/fdroid/fdroidserver:buildserver -lc '
    export PATH=/home/vagrant/fdroidserver:$PATH
    git config --global --add safe.directory /build
    fdroid readmeta && fdroid lint de.hexenwoche.audiolex \
      && fdroid rewritemeta de.hexenwoche.audiolex \
      && fdroid build -v -l de.hexenwoche.audiolex'
```

Vier Fallgruben stecken in diesen zwölf Zeilen, alle am 2026-08-17 einzeln hineingetreten:

- **`fdroid` liegt nicht auf dem `PATH`.** Es kommt aus dem gemounteten Checkout, also `export PATH=/home/vagrant/fdroidserver:$PATH` — sonst `command not found`.
- **`bash -lc`, nicht `bash -c`.** `ANDROID_HOME=/opt/android-sdk` steht im Profil des Benutzers `vagrant`; eine Nicht-Login-Shell sieht es nicht, und der Build sucht dann ins Leere.
- **Das Datenverzeichnis muss ein Git-Repository sein.** Reproduzierbare Builds brauchen `SOURCE_DATE_EPOCH`; fdroidserver holt den aus dem Git-Log des Quellverzeichnisses und fällt, solange das noch nicht geklont ist, auf den Commit-Zeitstempel der Rezeptur im `fdroiddata`-Repo zurück. Ohne `.git` gibt es `None`, und der Lauf stirbt mit `TypeError: str expected, not NoneType` — ein Fehlerbild, das nach Bug aussieht und keiner ist.
- **`fdroid lint` prüft Kategorien gegen `config/`.** Ohne `config/categories.yml` und die Kategorie-Icons aus `fdroiddata` beanstandet es *jede* Kategorie und stirbt später an einer fehlenden `category_connectivity.png`. Im echten Fork ist das da; für eine Probe ohne Vollklon reicht `curl -sL "https://gitlab.com/fdroid/fdroiddata/-/archive/master/fdroiddata-master.tar.gz?path=config"` (445 KB).

**Probe 1 ist gelaufen (2026-08-17), in der Variante, die ohne öffentliches Repo auskommt:** `git checkout-index -a --prefix=<leeres Verzeichnis>` exportiert genau den Repository-Stand, `ANDROID_HOME` aus der Umgebung ersetzt das fehlende `local.properties`, `./gradlew :composeApp:assembleRelease --no-daemon` läuft durch. Damit sind zwei der drei Vermutungen erledigt: Der Desktop-Zielteil des KMP-Moduls stört den Android-Release-Build nicht, und KSP/Room laufen durch. **Mit dem echten Klon wiederholt (2026-08-17, nach Schritt 4):** `git clone --branch v0.33.5 --depth 1` von GitHub in ein leeres Verzeichnis, kein `local.properties`, `ANDROID_HOME` aus der Umgebung, `./gradlew :composeApp:assembleRelease --no-daemon` — grün. Der Klon bringt 72 Korpus-WAVs und `bus.wav` mit, die Telefonnummer steht in keiner Datei. Am fertigen APK nachgesehen (`aapt2 dump badging`): `de.hexenwoche.audiolex`, versionCode **41**, versionName **0.33.5**, minSdk 29, compileSdk 35 — dieselben Werte, die die Rezeptur erwartet. Im APK liegen 72 Korpus-WAVs, `bus.wav` mit 247 004 Bytes (byte-identisch mit der Aufnahme vom Gerät) und `noise.json`; die Assets wiegen 4,3 MB, das APK 29 MB. Nebenbefund: Compose Resources packt auch die beiden Herkunfts-READMEs mit ein (10 KB) — kein Schaden, es heißt nur, dass diese Texte ausgeliefert werden.

**Probe 2 ist gelaufen (2026-08-17), mit fdroidserver 2.4.5 im Image `registry.gitlab.com/fdroid/fdroidserver:buildserver`** (2,37 GB). Ergebnis der Reihe nach:

| Schritt | Ergebnis |
| --- | --- |
| `fdroid readmeta` | ok |
| `fdroid lint de.hexenwoche.audiolex` | **Rückgabe 0**, keine Beanstandung |
| `fdroid rewritemeta` | kein Unterschied — die Rezeptur ist kanonisch formatiert |
| Scanner (`suss`-Signaturdaten) | **kein Fund** — er hätte den Build sonst abgebrochen |
| `fdroid build -v -l` | **1 build succeeded**, `BUILD SUCCESSFUL in 1m 29s` |
| Ergebnis | `unsigned/de.hexenwoche.audiolex_41.apk` (30 400 285 Bytes) + Quelltext-Tarball (4,9 MB) |

Damit sind die drei offenen Fragen beantwortet. **Gradle 8.11.1 ist verfügbar:** fdroidserver lädt es selbst nach (`Downloading missing gradle version 8.11.1`) und prüft die Summe (`gradle-8.11.1-bin.zip: OK`). Nebenbei die Antwort auf eine Frage, die gar nicht gestellt war: fdroidserver **entfernt** `gradle/wrapper/gradle-wrapper.jar` vor dem Bauen und nimmt sein eigenes, verifiziertes Gradle — die vorkompilierte Jar-Datei im Repo ist also kein Aufnahmehindernis, sie wird ignoriert. **Der Scanner schlägt nirgends an**, auch nicht bei den 73 mitgelieferten WAV-Dateien. Und der Build läuft ohne Sonderbehandlung durch: kein `prebuild`, kein `sudo`-Block, kein `rm` in der Rezeptur.

**Nebenbefund mit Tragweite:** Das APK aus dem Container ist **byte-identisch** mit dem aus Probe 1 — gleiche 30 400 285 Bytes, gleicher SHA-256, obwohl das eine mit dem SDK dieses Rechners und das andere mit F-Droids SDK und deren nachgeladenem Gradle gebaut wurde. Das ist die technische Voraussetzung für reproduzierbare Builds (Schritt 8). Beweiskraft aber mit Maß: zwei Läufe auf **einer** Maschine, gleicher Kernel, gleiche JDK-Hauptversion. Für eine Zusage an F-Droid wäre ein Vergleich über verschiedene Maschinen nötig.

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

Zur Signatur: F-Droid signiert mit eigenem Schlüssel, das ist der Normalfall und für den Anfang das Richtige. **Neu seit Probe 2 (2026-08-17):** Die technische Voraussetzung für den anderen Weg ist nachweislich erfüllt — zwei Builds desselben Tags, einmal mit dem SDK dieses Rechners und einmal im F-Droid-Container, ergaben ein byte-identisches APK. Trotzdem bleibt die Empfehlung für die erste Veröffentlichung, F-Droid signieren zu lassen: Der Nachweis stammt von einer einzigen Maschine, und eine erste Anmeldung mit zusätzlichem Reproduzierbarkeits-Versprechen hat mehr bewegliche Teile, als ein erster Merge Request braucht. Der Befund ist festgehalten, damit die Entscheidung später auf Zahlen statt auf Vermutungen fußt. Reproduzierbare Builds mit `AllowedAPKSigningKeys` (die App wird dann mit dem Schlüssel des Autors ausgeliefert und F-Droid verifiziert nur) gelten als gute Praxis, verlangen aber Bit-Gleichheit zwischen zwei Builds und kosten Einrichtung. Der Preis dafür, das später zu ändern: Nutzer können nicht von einer F-Droid-signierten auf eine anders signierte Version aktualisieren, sie müssen neu installieren. Wenn es also je passieren soll, dann besser vor der ersten Veröffentlichung als danach.

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
| 72 Korpus-WAVs im Index | `files/corpus/raw/de-DE/` | **erledigt** — 68 synthetisch, 4 eingesprochen |
| Toolchain-Plugin entfernt | `settings.gradle.kts` | **erledigt** |
| Öffentliches Repository | GitHub | **erledigt** — anonym über HTTPS geprüft |
| Git-Tag je Version (`v0.33.5`) | Repo | **erledigt** — gepusht, löst auf `754a339` auf |
| Historie ohne Telefonnummer | Repo | **erledigt** — `filter-repo`, 40 Commits |
| `short_description.txt` (≤80) | `fastlane/metadata/android/<locale>/` | **erledigt**, de + en |
| `full_description.txt` (≤4000) | dito | **erledigt**, de + en |
| `title.txt` (≤50) | dito | **erledigt** |
| `images/icon.png` (512×512) | dito | **erledigt** |
| `changelogs/41.txt` (≤500) | dito | **erledigt**, de + en |
| Korrigierter Impressum-Satz | `ImpressumScreen.kt` | **erledigt** |
| Rezeptur `de.hexenwoche.audiolex.yml` | `fdroid/metadata/`, gehört in den Fork | **erledigt**, einsetzen bleibt |
| Vier Screenshots | `images/phoneScreenshots/` | **erledigt** — vom A53, alle aus Build 0.33.4 |
| Gebündeltes Störgeräusch (Bus) | `files/noise/bus.wav` + `noise.json` | **erledigt** — vom Gerät geholt, unbearbeitet |
| Lizenz der eigenen Aufnahmen | README-Tabelle | **erledigt** — CC0-1.0 (Autor-Entscheid) |
| `fdroid lint` + `fdroid build` im Container | lokal, Schritt 7 | **erledigt** — lint 0, Build erfolgreich, Scanner ohne Fund |
| Datenschutzerklärung unter Web-Adresse | — | **nicht nötig** — das verlangt Google Play, nicht F-Droid |

Für die „Neu"-Liste im Client braucht ein Eintrag: Name, Icon, Kurz- und Langbeschreibung, Lizenz, mindestens einen Änderungstext, mindestens ein Bild und mindestens eine Übersetzung. Ohne diese Teile wird die App aufgenommen, taucht aber prominent nirgends auf.

## Textvorlagen

Die Texte selbst stehen als Dateien unter `fastlane/metadata/android/<locale>/` — dort sind sie maßgeblich, und nur dort. Bis zum 2026-08-17 standen sie hier zusätzlich als Entwurf, und die Kopien sind erwartungsgemäß auseinandergelaufen: Der Entwurf behauptete noch, die App liefere kein Störgeräusch mit, und nannte einen Änderungstext für versionCode 35. Statt zwei Fassungen zu pflegen, steht hier jetzt nur, was gemessen ist.

| Datei (je unter `fastlane/metadata/android/`) | Zeichen | Grenze |
| --- | --- | --- |
| `de-DE/title.txt` | 8 | 50 |
| `de-DE/short_description.txt` | 76 | 80 |
| `de-DE/full_description.txt` | 2599 | 4000 |
| `de-DE/changelogs/41.txt` | 398 | 500 |
| `en-US/title.txt` | 8 | 50 |
| `en-US/short_description.txt` | 79 | 80 |
| `en-US/full_description.txt` | 2309 | 4000 |
| `en-US/changelogs/41.txt` | 388 | 500 |

Gezählt werden **Zeichen, nicht Bytes**. Bei Umlauten ist das ein Unterschied, der die Kurzbeschreibung an der 80er-Grenze kippen lässt — `wc -c` liegt dort zu hoch. Nachzählen:

```bash
python3 -c "import sys;print(len(open(sys.argv[1],encoding='utf-8').read().rstrip()))" \
  fastlane/metadata/android/de-DE/short_description.txt
```

Zu jeder neuen Version kommt ein `changelogs/<versionCode>.txt` dazu; die alten bleiben liegen. Die englischen Fassungen sind Übersetzungen, keine eigenen Texte — der englische Eintrag ist die Rückfallebene, nicht die Hauptsache, und darf kürzer sein.

## Was nur der Autor kann

Stand 2026-08-17, abends. Übrig ist **ein** Punkt.

**GitLab-Konto, Fork, Merge Request** — Schritt 9 führt das für jemanden durch, der es noch nie gemacht hat. Ein Konto kann ich nicht anlegen und keinen Antrag in seinem Namen stellen. Empfehlung von dort, weil sie leicht untergeht: die Entstehung im Zusammenspiel mit einer KI im Merge Request selbst ansprechen, statt sie unerwähnt zu lassen.

Erledigt und damit nicht mehr auf dieser Liste:

- **Repository öffentlich, Tag gepusht** (2026-08-17): `master` und `v0.33.5` zeigen bei GitHub auf `754a339`, anonym über HTTPS geprüft. Die Historie ist um die Telefonnummer bereinigt, Schritt 4 hält den Ablauf und die zwei Lehren daraus fest.
- **Die Abnahme am Hörgerät** — Urteil des Autors: „Der Hörtest ist sehr gut." Das Bus-Geräusch ist bei „Fortgeschritten" sehr dominant, lässt sich aber über den Regler leicht korrigieren; von Knistern keine Rede. Damit ist der offene Messbefund geschlossen (bei SNR −5 dB laufen 1,7 % der Samples in die Begrenzung des Mixers) — **kein** Limiter-Durchgang auf `bus.wav`, die Datei bleibt, wie sie vom Gerät kam.
- **Die lokalen Doubletten auf dem A53 sind aufgeräumt:** Die vier eigenen Aufnahmen und das eigene Bus-Geräusch waren identisch mit dem, was jetzt mitgeliefert wird, und sind nach Autor-Entscheid vom Gerät entfernt worden. Auf fremden Geräten konnte die Doppelung nie auftreten.
- Außerdem: LICENSE, Toolchain-Plugin, 72 Korpus-Audios im Index, gebündeltes Bus-Geräusch, Demo-Einsprachen samt Lizenz und Einverständnis, Impressum, README, Store-Texte, Icon, vier Screenshots vom Gerät, Rezeptur, Build-Probe aus dem echten Klon.

Eine Kleinigkeit, die kein Blocker ist: **Akzent der Demo-Einsprachen.** Sie sind als `locale: de-DE` eingetragen, weil sich das am Schreibtisch nicht feststellen lässt. Sind sie österreichisch gefärbt, gehört `de-AT` hinein (und die Dateien in ein `raw/de-AT/`).

Was **ich** noch beisteuern kann: die Rezeptur auf einen späteren Tag anpassen, falls nicht `v0.33.5` veröffentlicht wird — und Rückfragen des Paketierers im Merge Request mit dir durchgehen. Probe 2 aus Schritt 7 ist gelaufen: `fdroid lint` sauber, `fdroid build` erfolgreich, Scanner ohne Fund.

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
