# ADR-0008: Zeitquelle für SRS-Fälligkeiten — injizierbares `Clock`-Interface

- **Status:** akzeptiert (Autor-Entscheid 2026-07-10)
- **Datum:** 2026-07-10

## Kontext

Die SRS-Logik (`ReviewScheduler.review`, `ReviewQueue.due`, `ExamSession.rate`) rechnet mit `nowEpochMillis: Long`, das der Aufrufer liefert — bewusst so, damit `:core` datumsbibliotheksfrei und die Logik deterministisch testbar bleibt (ADR-0003-Linie, AGENTS.md §5). Beim Bau des Prüfmodus-Screens fiel im Opus-Review 2026-07-10 auf, dass es projektweit **keine** produktive „Jetzt"-Zeitquelle gibt: der Screen setzte `nowEpochMillis = 0L` hart ein, weil nichts anderes verfügbar war. Folge — die SRS-Kernfunktion war faktisch tot (jede bewertete Karte bekam eine Fälligkeit in 1970 und wurde nie wieder fällig; der Prüfmodus zeigte ab der zweiten Sitzung dauerhaft „Nichts fällig"). Kein Unit-Test fing das, weil die `:core`-Klassen isoliert mit sinnvollen `now`-Werten korrekt getestet sind — der Fehler lebte nur an der UI-Integrationsstelle.

Zwei Kräfte erzwingen den Entscheid: (1) `:core` soll die Systemzeit nicht direkt aus common code lesen (es gibt in `commonMain` kein plattformneutrales `currentTimeMillis`, und eine Fremd-Dependency wäre ein bewusster Bruch mit der bisherigen Dependency-Askese). (2) Die Zeitquelle muss **injizierbar** sein — genau das fehlte, als der Bug entstand: eine testbare Uhr hätte einen Integrationstest erlaubt („Karte bewerten → mit vorgerückter Uhr wieder fällig, mit unveränderter nicht"), der die Regression abgefangen hätte.

## Entscheidung

Wir führen ein schmales, plattformfreies `Clock`-Interface in `:core` ein:

```kotlin
// core/commonMain, Paket z. B. de.hexenwoche.audiolex.core.time
interface Clock {
    fun nowEpochMillis(): Long
}

expect fun systemClock(): Clock
```

Die `actual`-Implementierungen liefern die Systemzeit an der dünnen Plattformgrenze (analog `createAudioSink`): `jvmMain` und `androidMain` je ein `actual fun systemClock(): Clock = ... System.currentTimeMillis() ...`. Die `Clock` wird — wie das `ReviewCardRepository` und die `AudioLexDatabase` — von der App erzeugt und durch `App()` bis zu den Screens gereicht, die sie brauchen; die Screens rufen `clock.nowEpochMillis()` statt einer Konstante. Im Test wird eine `FakeClock` (settable `var now`) injiziert.

Kernpunkt: **Das `Clock`-*Interface* ist die injizierbare Abstraktion, `systemClock()` ist nur die eine plattformspezifische Fabrik.** Eine reine `expect fun nowEpochMillis(): Long` wäre einfacher, aber gerade **nicht** injizierbar (statischer Aufruf) — sie würde das Testproblem nicht lösen, das diesen ADR ausgelöst hat.

## Alternativen

- **Reine `expect fun nowEpochMillis(): Long`** (ohne Interface): minimal, führt die `createAudioSink`-Form fort, aber nicht injizierbar — Tests der Aufrufstelle müssten die Zeit weiterhin über einen Umweg fälschen. Verworfen, weil die fehlende Testbarkeit die eigentliche Ursache des Bugs war.
- **`kotlinx-datetime`** (`Clock.System.now().toEpochMilliseconds()`): gut gepflegt, KMP-nativ, bringt aber eine Fremd-Dependency und ein `Clock`-Konzept, das für den MVP-Bedarf (ein `Long`) überdimensioniert ist. Bleibt Kandidat, falls später echte Datums-/Zeitzonenlogik (Sitzungshistorie mit lokalem Datum, S12) hinzukommt — dann kann `systemClock()`s `actual` intern darauf umgestellt werden, ohne die Aufrufer zu ändern.
- **`kotlin.time.Clock`** (stdlib): bei Kotlin 2.1.21 experimentell, **stabil erst ab Kotlin 2.3** (nicht 2.2.x — Recherche 2026-07-10, KT-80778). Ein Kotlin-Bump als Mittel zur Zeitquelle lohnt nicht: Er zieht KSP (Version = Kotlin-Version), Compose-Compiler und Compose Multiplatform synchron mit (reales Regressionsrisiko, besonders Skiko unter WSL2) und brächte die *stabile* `Clock` ohnehin erst mit 2.3. Der Versionssprung ist als eigenes `[PROP]`-Backlog-Item festgehalten, entkoppelt von diesem ADR. Sobald das Projekt auf 2.3 geht, wird `systemClock()`s `actual` intern auf `kotlin.time.Clock.System` umgestellt — die Aufrufer merken nichts, genau dafür ist die eigene Abstraktion da.

## Konsequenzen

- Die SRS-Aufrufstelle wird testbar: ein Integrationstest mit `FakeClock` sichert die Regression ab, die dieser Bug war — das war vorher unmöglich.
- Zwei triviale `actual`-Dateien mehr (jvm + android), konsistent mit der bestehenden `AudioSink`-Struktur; kein neues Gradle-Modul, keine Fremd-Dependency.
- `:core` bleibt datumsbibliotheksfrei; die Umstellung auf `kotlinx-datetime` oder `kotlin.time.Clock` ist später ein reiner Austausch hinter `systemClock()`, ohne die Aufrufer anzufassen.
- `Clock` liefert bewusst nur UTC-Epoch-Millis, keine Zeitzonen-/Kalenderlogik — und das ist für die SRS-Fälligkeit ausreichend, weil dort nur **Zeitdifferenzen** zählen (wie lange seit dem letzten Zeigen der Karte), nicht absolute Genauigkeit oder lokale Kalenderdaten (Autor-Entscheid 2026-07-10). Für späteres **Reporting** (Sitzungshistorie S12, lokales Datum/Uhrzeit) ist der saubere Weg, beim Speichern eines Sitzungs-Timestamps die Zeitzone als Zusatzfeld mitzuschreiben und erst bei der Anzeige umzurechnen — die zonenlose Millis-Rechnung bleibt davon unberührt. Das ist Sache des Sitzungshistorie-Items (dort vermerkt), nicht dieser Zeitquelle; `kotlinx-datetime` wird dort neu bewertet, falls die Umrechnung mehr als triviale Formatierung braucht.
