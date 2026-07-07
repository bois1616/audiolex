# ADR-0006: Audioquelle MVP — lokales TTS (Piper), Dialekte als vorbereitetes Datenfeld

- **Status:** akzeptiert
- **Datum:** 2026-07-07

## Kontext

Konzept Abschnitt 8.1 stellte die Audioquelle für den Wortkorpus zur Klärung: eigene Aufnahmen (mehrere Sprecher, natürlich) vs. TTS-Start für schnellen Prototyp. Für M1 wird zusätzlich der Wunsch nach mehreren Stimmlagen (männlich/weiblich) und — als Fortgeschritten-Schwierigkeitsstufe — nach Dialekten geäußert.

## Entscheidung

1. **TTS als Startquelle**, nicht eigene Aufnahmen: schneller Korpusaufbau, kein Aufnahme-Setup nötig, um M1 zu beginnen.
2. **Lokal/offline (Piper)** statt Cloud-TTS: passt zur strikten Lokalität (Konzept 4.5, AGENTS.md §5 „kein Netzwerk-/Cloud-Code"). Audio wird einmalig per Skript generiert und als WAV abgelegt — die App selbst braucht zur Laufzeit keine TTS-Engine und keine Netzwerkverbindung.
3. **Zwei bis drei Stimmen für M1**: je eine männliche und weibliche deutsche Piper-Stimme (z. B. `de_DE-thorsten` und `de_DE-eva_k` oder `de_DE-kerstin`), optional eine dritte zur Varianz. Alle Hochdeutsch (`de-DE`).
4. **Dialekte werden im Datenmodell vorbereitet, aber in M1 nicht befüllt**: `AudioRecording` bekommt ein `locale`-Feld (BCP-47 mit Regionstag, z. B. `de-DE`, `de-AT`, `de-CH`, perspektivisch auch `de-DE-bar` für Bairisch o. ä.) statt eines reinen `speaker`-Strings. Piper bietet keine belastbaren deutschen Dialektstimmen — die Befüllung des Fortgeschritten-Modus mit echten Dialekten verschiebt sich auf eine spätere Phase (eigene Aufnahmen oder eine noch zu findende TTS-Quelle), blockiert aber M1 nicht.

## Alternativen

- **Cloud-TTS (Azure/Google) für breitere Dialektauswahl:** bricht die Lokalitätsanforderung beim Korpus-Ausbau (API-Key, Kosten, Netzwerk zur Generierungszeit) und wurde für den MVP verworfen. Bleibt als `[PROP]` denkbar, falls Dialektstimmen später doch über eine Cloud-Quelle bezogen werden sollen.
- **Sofort nach dialektfähiger lokaler TTS suchen:** hätte M1 verzögert, ohne dass eine gute deutsche Dialekt-TTS in Aussicht steht. Verworfen zugunsten „Feld vorbereiten, später befüllen".
- **Gleich eigene Aufnahmen:** liefert die beste Qualität und Dialekttreue, aber Aufnahme-Logistik (Sprecher, Technik) ist für M1 zu langsam. Bleibt Ziel für später (Konzept 4.3: „Priorität auf echten Sprachaufnahmen").

## Konsequenzen

- `AudioRecording.locale` ersetzt `speaker` als Freitext; Sprecheridentität wird ein eigenes Feld (`voiceId`), damit Stimmlage und Dialekt/Region unabhängig gefiltert werden können (Lernmodus: neutrale Hochdeutsch-Stimme; Fortgeschritten: gezielt Dialekt-Pool).
- Ein Generierungsskript (`tools/generate_tts.py` o. ä., Backlog M1) erzeugt die WAV-Dateien einmalig offline; Piper selbst ist kein Laufzeit-Dependency der App.
- Sollte später eine brauchbare Dialekt-TTS gefunden werden oder echte Dialekt-Aufnahmen entstehen, ist nur `corpus-data` zu befüllen — keine Modell- oder Architekturänderung nötig.
