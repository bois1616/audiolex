# ADR-0006: Audioquelle MVP — lokales TTS (Piper), Dialekte als vorbereitetes Datenfeld

- **Status:** akzeptiert
- **Datum:** 2026-07-07

## Kontext

Konzept Abschnitt 8.1 stellte die Audioquelle für den Wortkorpus zur Klärung: eigene Aufnahmen (mehrere Sprecher, natürlich) vs. TTS-Start für schnellen Prototyp. Für M1 wird zusätzlich der Wunsch nach mehreren Stimmlagen (männlich/weiblich) und — als Fortgeschritten-Schwierigkeitsstufe — nach Dialekten geäußert.

## Entscheidung

1. **TTS als Startquelle**, nicht eigene Aufnahmen: schneller Korpusaufbau, kein Aufnahme-Setup nötig, um M1 zu beginnen.
2. **Lokal/offline (Piper)** statt Cloud-TTS: passt zur strikten Lokalität (Konzept 4.5, AGENTS.md §5 „kein Netzwerk-/Cloud-Code"). Audio wird einmalig per Skript generiert und als WAV abgelegt — die App selbst braucht zur Laufzeit keine TTS-Engine und keine Netzwerkverbindung.
3. **M1 startet mit einer Stimme (`de_DE-thorsten-medium`, männlich, Hochdeutsch)**, statt der ursprünglich geplanten zwei. Getestet wurde zusätzlich `de_DE-kerstin-low` (weiblich): Bei isolierten Einzelwörtern (kein Satzkontext) sprach kerstin messbar zu schnell/gestaucht — z. B. „Ball" allein 0,24 s gegenüber 0,52 s bei thorsten-medium mit identischem Text, während ein voller Testsatz mit derselben Stimme normal und gut verständlich war. Ein Workaround (Wort in einen Trägersatz einbetten, dann zurückschneiden) wurde geprüft, lieferte aber keine zuverlässigen Wortgrenzen über alle Testwörter hinweg und wurde verworfen. Piper bietet für deutsche Einzelsprecherinnen nur die Qualitätsstufen `low`/`x_low` (kerstin, ramona, eva_k) — vermutlich Ursache des Effekts, da das einzige `medium`-Modell (thorsten) das Problem nicht zeigt. Eine zweite Stimme bleibt offener Backlog-Punkt (M1), bis eine bessere Qualitätsstufe verfügbar ist oder eine andere Quelle (andere TTS-Engine, echte Aufnahme) genutzt wird.
4. **Dialekte werden im Datenmodell vorbereitet, aber in M1 nicht befüllt**: `AudioRecording` bekommt ein `locale`-Feld (BCP-47 mit Regionstag, z. B. `de-DE`, `de-AT`, `de-CH`, perspektivisch auch `de-DE-bar` für Bairisch o. ä.) statt eines reinen `speaker`-Strings. Piper bietet keine belastbaren deutschen Dialektstimmen — die Befüllung des Fortgeschritten-Modus mit echten Dialekten verschiebt sich auf eine spätere Phase (eigene Aufnahmen oder eine noch zu findende TTS-Quelle), blockiert aber M1 nicht.

## Alternativen

- **Cloud-TTS (Azure/Google) für breitere Dialektauswahl:** bricht die Lokalitätsanforderung beim Korpus-Ausbau (API-Key, Kosten, Netzwerk zur Generierungszeit) und wurde für den MVP verworfen. Bleibt als `[PROP]` denkbar, falls Dialektstimmen später doch über eine Cloud-Quelle bezogen werden sollen.
- **Sofort nach dialektfähiger lokaler TTS suchen:** hätte M1 verzögert, ohne dass eine gute deutsche Dialekt-TTS in Aussicht steht. Verworfen zugunsten „Feld vorbereiten, später befüllen".
- **Gleich eigene Aufnahmen:** liefert die beste Qualität und Dialekttreue, aber Aufnahme-Logistik (Sprecher, Technik) ist für M1 zu langsam. Bleibt Ziel für später (Konzept 4.3: „Priorität auf echten Sprachaufnahmen").

## Konsequenzen

- `AudioRecording.locale` ersetzt `speaker` als Freitext; Sprecheridentität wird ein eigenes Feld (`voiceId`), damit Stimmlage und Dialekt/Region unabhängig gefiltert werden können (Lernmodus: neutrale Hochdeutsch-Stimme; Fortgeschritten: gezielt Dialekt-Pool).
- `tools/generate_tts.py` erzeugt die WAV-Dateien einmalig offline; Piper selbst ist kein Laufzeit-Dependency der App.
- Sollte später eine brauchbare Dialekt-TTS gefunden werden oder echte Dialekt-Aufnahmen entstehen, ist nur `corpus-data` zu befüllen — keine Modell- oder Architekturänderung nötig.
- Der Testkorpus ist bis auf Weiteres einstimmig (nur thorsten). Das ist für M1 (WAV-Loader, Sinks, Kanalsteuerung) unkritisch, da diese Bausteine stimmenunabhängig sind; eine zweite Stimme nachzuziehen ändert nur `corpus-data`, keinen Code.
- Sample-Rate 22050 Hz (thorsten-medium) ist jetzt korpusweit einheitlich — das eingangs befürchtete Problem unterschiedlicher Raten pro Stimme (kerstin-low lief mit 16000 Hz) stellt sich vorerst nicht.
