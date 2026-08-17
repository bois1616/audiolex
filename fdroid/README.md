# F-Droid-Rezeptur

`metadata/de.hexenwoche.audiolex.yml` ist die Build-Rezeptur für F-Droid. Sie gehört **nicht** in dieses Repository, sondern in einen Fork von [fdroiddata](https://gitlab.com/fdroid/fdroiddata/) — hier liegt sie nur als pflegbare Kopie, damit sie nicht bei jeder Version neu erfunden wird. Der eigene Build liest sie nicht.

Einsetzen:

```bash
# Fork von https://gitlab.com/fdroid/fdroiddata/ anlegen und klonen, dann darin:
git checkout -b de.hexenwoche.audiolex
cp <audiolex>/fdroid/metadata/de.hexenwoche.audiolex.yml metadata/
fdroid readmeta && fdroid lint de.hexenwoche.audiolex
git add metadata/de.hexenwoche.audiolex.yml
git commit -m "New App: de.hexenwoche.audiolex"
git push origin de.hexenwoche.audiolex
```

Danach Merge Request stellen. Die vollständige Schrittfolge samt Prüfungen steht in [../docs/fdroid-anmeldung.md](../docs/fdroid-anmeldung.md).

## Was bei jeder Version mitzupflegen ist

`versionName`, `versionCode` und `commit` im `Builds`-Block sowie `CurrentVersion`/`CurrentVersionCode` — alle vier kommen aus `composeApp/src/commonMain/kotlin/de/hexenwoche/audiolex/AppVersion.kt` und dem passenden Tag `v<VERSION_NAME>`.

Wegen `AutoUpdateMode: Version` und `UpdateCheckMode: Tags` erledigt F-Droid das nach der ersten Aufnahme selbst: Ein neuer Tag erzeugt einen neuen Build-Eintrag. Die Kopie hier bleibt trotzdem der Ort, an dem eine bewusste Änderung an der Rezeptur (neue Kategorie, geänderte Adressen, ein Anti-Feature) zuerst festgehalten wird.

## Der Text für den Merge Request

`merge-request.md` daneben enthält den Titel im vorgeschriebenen Format, die ausgefüllte Vorlage „App inclusion" und die eine Entscheidung, die vor dem Absenden fällt: reproduzierbare Builds ja oder nein. Die ist eine Einbahnstraße — wer sie auslässt, kann sie später nicht nachholen, weil das APK dann mit F-Droids Schlüssel signiert ist.
