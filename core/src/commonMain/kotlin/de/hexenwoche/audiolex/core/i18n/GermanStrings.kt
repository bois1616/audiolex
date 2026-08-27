package de.hexenwoche.audiolex.core.i18n

import de.hexenwoche.audiolex.core.audio.OutputSetup
import de.hexenwoche.audiolex.core.audio.OwnNoiseSource
import de.hexenwoche.audiolex.core.corpus.CorpusLanguage
import de.hexenwoche.audiolex.core.corpus.EntryKind
import de.hexenwoche.audiolex.core.settings.ChannelMode
import de.hexenwoche.audiolex.core.settings.CorpusMode
import de.hexenwoche.audiolex.core.settings.SettingsProfile
import de.hexenwoche.audiolex.core.settings.ThemeMode
import de.hexenwoche.audiolex.core.srs.ReviewRating

/**
 * The German catalog -- the app's original wording, carried over unchanged
 * where it already existed (ADR-0015). Only the Kurzanleitung block at the
 * bottom is new text; everything above it was lifted out of the screens
 * verbatim, so localizing changed no German label.
 *
 * The "Aufnahme(n)" / "Eintrag/Einträge" shorthand in the backup messages is
 * kept on purpose. It reads a little clerical, but it was the author's own
 * wording and it is unambiguous -- [EnglishStrings] spells its plurals out
 * instead, which is what English needs.
 */
internal object GermanStrings : Strings {

    // ---- Shared ----

    override val back = "Zurück"
    override val backToStart = "Zurück zum Start"
    override val quit = "Beenden"
    override val cancel = "Abbrechen"
    override val save = "Speichern"
    override val delete = "Löschen"
    override val listen = "Anhören"
    override val repeatPlayback = "Wiederholen"
    override val settings = "Einstellungen"
    override val sessionHistory = "Sitzungshistorie"
    override val ownRecordings = "Eigene Aufnahmen"
    override val ownNoises = "Eigene Störgeräusche"

    override fun playbackFailed(message: String?) = "Wiedergabe fehlgeschlagen: $message"

    override fun corpusLoadFailed(message: String?) = "Korpus konnte nicht geladen werden: $message"

    override fun deleteConfirmBody(name: String) =
        "„$name“ wird endgültig entfernt, inklusive der Aufnahme. Das lässt sich nicht rückgängig machen."

    override fun joinLast(parts: List<String>) = when (parts.size) {
        0, 1 -> parts.joinToString(", ")
        else -> parts.dropLast(1).joinToString(", ") + " und " + parts.last()
    }

    // ---- Startbildschirm ----

    override val appSubtitle = "Hörtraining: Klang → Wort → Bedeutung"
    override val startLearningMode = "Lernmodus starten"
    override val startExamMode = "Prüfmodus starten"
    override val quickGuide = "Kurzanleitung"
    override val exitApp = "App beenden"
    override val imprintAndPrivacy = "Impressum & Datenschutz"
    override val languageChoice = "Sprache"

    // ---- Lernmodus ----

    override val learningFinished = "Fertig! Wörter durchlaufen."
    override val previousEntry = "Vorheriges"
    override val nextEntry = "Weiter"

    override fun noRecordingFound(text: String) = "Keine Aufnahme für „$text“ gefunden."

    override fun recordingFileMissing(fileRef: String) = "Aufnahme „$fileRef“ nicht gefunden."

    // ---- Prüfmodus ----

    override val noCardsToPractise = "Es gibt noch keine Karten zum Üben."
    override val learningModeInstead = "Stattdessen Lernmodus"
    override val examFinished = "Fertig!"
    override val newExamRound = "Neue Prüfrunde"
    override val nextCard = "Nächstes"
    override val tapToReveal = "Antippen zum Aufdecken"

    override fun cardsRatedSentence(count: Int) = "$count Karten bewertet."

    override fun wordForCardNotFound(cardId: String) = "Wort zu Karte „$cardId“ nicht gefunden."

    override fun ratingLabel(rating: ReviewRating) = when (rating) {
        ReviewRating.AGAIN -> "Sofort"
        ReviewRating.SOON -> "Bald"
        ReviewRating.LATER -> "Später"
        ReviewRating.GOOD -> "Gut"
        ReviewRating.PERFECT -> "Perfekt"
    }

    override fun intervalHint(rating: ReviewRating) = when (rating) {
        ReviewRating.AGAIN -> "1 min"
        ReviewRating.SOON -> "10 min"
        ReviewRating.LATER -> "1 Tag"
        ReviewRating.GOOD -> "1 Woche"
        ReviewRating.PERFECT -> "1 Monat"
    }

    // ---- Sitzungshistorie ----

    override val noSessionsYet = "Noch keine Sitzungen."
    override val noRatings = "keine Bewertungen"

    override fun sessionModeLabel(mode: String) = when (mode) {
        "PRUEFMODUS" -> "Prüfmodus"
        else -> mode
    }

    override fun sessionSummary(ratedCount: Int, ratingDetail: String) =
        "$ratedCount Karten bewertet — $ratingDetail"

    // ---- Leerer Korpus ----

    override val noContingentSelected =
        "Kein Kontingent ausgewählt. Wähle mindestens eines in den Einstellungen unter „Korpus“ aus."
    override val nothingForSelectedContingents =
        "Für die ausgewählten Kontingente gibt es dafür aktuell nichts zu trainieren. " +
            "Passe die Auswahl unter „Korpus“ oder den Trainingsinhalt in den Einstellungen an."
    override val emptyCorpus = "Kein Wort im Korpus vorhanden."
    override val emptyForLanguage =
        "In dieser Sprache gibt es noch nichts zu trainieren. Wähle in den Einstellungen eine andere " +
            "Trainingssprache, oder sprich selbst etwas in dieser Sprache ein."

    // ---- Einstellungen ----

    override val sectionTrainingLevel = "Trainingsstufe"
    override val customLevel = "Individuell eingestellt"
    override val sectionAppearance = "Erscheinungsbild"
    override val sectionTrainingContent = "Trainingsinhalt"
    override val sectionTrainingLanguage = "Trainingssprache"
    override val trainingLanguageHint =
        "Gilt für Wörter und Sätze im Training — nicht für die Sprache der Oberfläche."
    override val sectionCorpus = "Korpus"
    override val noContingentAvailable = "Kein Kontingent verfügbar."
    override val selectAll = "Alle auswählen"
    override val deselectAll = "Alle abwählen"
    override val sectionNoise = "Störgeräusch"
    override val onOff = "Ein/Aus"
    override val quieter = "leiser"
    override val louder = "lauter"
    override val sectionScenario = "Szenario"
    override val noNoiseYet = "Noch kein Geräusch vorhanden — der Ton bleibt sauber."
    override val sectionOutput = "Ausgabe"
    override val sectionChannels = "Kanäle"
    override val channelsIneffectiveHint =
        "Ohne Wirkung am Hörgerät: Es summiert Stereo automatisch zu Mono. " +
            "Verfügbar mit Stereo-Kopfhörern."
    override val noSpeaker = "Ohne Sprecher"

    override fun profileLabel(profile: SettingsProfile) = when (profile) {
        SettingsProfile.EINFACH -> "Einfach"
        SettingsProfile.SCHWIERIG -> "Schwierig"
        SettingsProfile.FORTGESCHRITTEN -> "Fortgeschritten"
    }

    override fun themeModeLabel(mode: ThemeMode) = when (mode) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Hell"
        ThemeMode.DARK -> "Dunkel"
    }

    override fun corpusModeLabel(mode: CorpusMode) = when (mode) {
        CorpusMode.WOERTER -> "Wörter"
        CorpusMode.SAETZE -> "Sätze"
    }

    override fun corpusLanguageLabel(language: CorpusLanguage) = when (language) {
        CorpusLanguage.DEUTSCH -> "Deutsch"
        CorpusLanguage.ENGLISCH -> "Englisch"
    }

    override fun channelModeLabel(mode: ChannelMode) = when (mode) {
        ChannelMode.BEIDE -> "Beide"
        ChannelMode.NUR_LINKS -> "Nur links"
        ChannelMode.NUR_RECHTS -> "Nur rechts"
    }

    override fun outputSetupDetected(setup: OutputSetup) = when (setup) {
        OutputSetup.STEREO_KOPFHOERER -> "Stereo-Kopfhörer erkannt"
        OutputSetup.HOERGERAET -> "Hörgerät erkannt"
    }

    override fun snrLabel(snrDb: Int) = if (snrDb >= 0) "SNR: +$snrDb dB" else "SNR: $snrDb dB"

    override fun wordCount(count: Int) = if (count == 1) "1 Wort" else "$count Wörter"

    override fun sentenceCount(count: Int) = if (count == 1) "1 Satz" else "$count Sätze"

    // ---- Eigene Aufnahmen ----

    override val sectionNewRecording = "Neue Aufnahme"
    override val fieldText = "Text"
    override val fieldSpeakerOptional = "Sprecher (optional)"
    override val fieldEntryLanguage = "Sprache"
    override val entryLanguageHint =
        "Legt fest, unter welcher Trainingssprache der Eintrag erscheint — nicht, was darin gesprochen wird."
    override val untranscribedHint =
        "Ohne Text lässt sich der Eintrag nicht trainieren. " +
            "Du kannst ihn später über „Text ändern“ nachtragen."
    override val sectionMyEntries = "Meine Einträge"
    override val noOwnRecordingsYet = "Noch keine eigenen Aufnahmen."
    override val editText = "Text ändern"
    override val reRecord = "Neu aufnehmen"
    override val closeRecording = "Aufnahme schließen"
    override val applyRecording = "Übernehmen"
    override val sectionBackup = "Sicherung"
    override val backupExplainer =
        "Eigene Aufnahmen, eigene Störgeräusche und dein Sitzungsverlauf liegen sonst nur auf diesem Gerät. " +
            "Der Export legt alles als ZIP-Datei in deinen Dokumenten ab — was danach damit geschieht, entscheidest du."
    override val export = "Exportieren"
    override val importAction = "Importieren"
    override val deleteEntryTitle = "Eintrag löschen?"

    override fun kindLabel(kind: EntryKind) = when (kind) {
        EntryKind.WORD -> "Wort"
        EntryKind.SENTENCE -> "Satz"
    }

    override fun noRecordingAvailable(text: String) = "Keine Aufnahme für „$text“ vorhanden."

    // ---- Sicherung: Export ----

    override val nothingToBackUp = "Nichts zu sichern: Es gibt noch keine Aufnahme, kein Geräusch und keine Sitzung."
    override val backupWriteFailed = "Sicherung fehlgeschlagen — die Datei konnte nicht geschrieben werden."

    override fun recordingCount(count: Int) = "$count Aufnahme(n)"

    override fun noiseCount(count: Int) = "$count Geräusch(e)"

    override fun sessionCount(count: Int) = "$count Sitzung(en)"

    override fun entryCount(count: Int) = "$count Eintrag/Einträge"

    override fun backupSaved(what: String, location: String) = "$what gesichert nach $location."

    override fun backupSkippedWithoutRecording(count: Int) =
        " $count Eintrag/Einträge ohne Aufnahme sind nicht enthalten."

    // ---- Sicherung: Import ----

    override val archiveUnreadable =
        "Diese Datei ist keine AudioLex-Sicherung oder beschädigt. Es wurde nichts verändert."
    override val importNothingAdded = "Nichts hinzugefügt."

    override fun importAdded(added: String) = "$added übernommen"

    override fun importAlsoAlreadyPresent(known: String) = ", $known waren schon vorhanden"

    override fun importOnlyAlreadyPresent(known: String) =
        "Nichts hinzugefügt — $known der Sicherung sind bereits vorhanden."

    override fun importSkippedEntries(count: Int) =
        " $count Eintrag/Einträge der Sicherung waren unvollständig und wurden übersprungen."

    override fun importSkippedNoises(count: Int) =
        " $count Geräusch(e) der Sicherung waren unvollständig und wurden übersprungen."

    // ---- Eigene Störgeräusche ----

    override val sectionNewNoise = "Neues Geräusch"
    override val noiseRecordingHint =
        "Beginne die Aufnahme direkt mit dem Geräusch; Stille am Anfang ist später vor jedem Wort hörbar."
    override val fieldLabel = "Bezeichnung"
    override val sectionImport = "Import"
    override val wavImportHint = "Vorhandene WAV-Dateien können übernommen werden — Format: PCM, mono, 22050 Hz."
    override val importWavFile = "WAV-Datei importieren"
    override val sectionMyNoises = "Meine Geräusche"
    override val noOwnNoisesYet = "Noch keine eigenen Geräusche."
    override val deleteNoiseTitle = "Geräusch löschen?"
    override val wavNotReadable =
        "Diese Datei konnte nicht als WAV gelesen werden. " +
            "Wähle eine WAV-Datei (PCM, 16 Bit) — oder nimm das Geräusch direkt hier auf."

    override fun recordingLabelSuggestion(timestamp: String) = "Aufnahme $timestamp"

    override fun fileChosen(name: String) = "$name gewählt"

    override fun noiseSourceLabel(source: OwnNoiseSource) = when (source) {
        OwnNoiseSource.IMPORT -> "Importiert"
        OwnNoiseSource.AUFNAHME -> "Aufgenommen"
    }

    override fun wavWrongFormat(sampleRate: Int, channels: Int) =
        "Diese Datei hat $sampleRate Hz und ${channelCount(channels)}. " +
            "AudioLex übernimmt Störgeräusche nur als WAV mit 22050 Hz, mono — " +
            "wandle die Datei entsprechend um oder nimm das Geräusch direkt hier auf."

    override fun channelCount(count: Int) = if (count == 1) "1 Kanal" else "$count Kanäle"

    // ---- Aufnahme-Steuerung ----

    override val recorderIdle = "Noch keine Aufnahme."
    override val recorderRecording = "Nimmt auf…"
    override val recorderProcessing = "Verarbeite Aufnahme…"
    override val recorderNoSignal = "Keine Aufnahme (kein Signal empfangen)."
    override val record = "Aufnehmen"
    override val stopRecording = "Stopp"
    override val micPermanentlyDenied =
        "Mikrofonzugriff wurde dauerhaft abgelehnt. Zum Aufnehmen bitte in den System-Einstellungen freigeben."
    override val openSystemSettings = "Einstellungen öffnen"
    override val micNeeded = "Zum Aufnehmen braucht AudioLex kurz Zugriff auf das Mikrofon."
    override val requestMicPermission = "Mikrofon-Berechtigung anfragen"

    override fun recorderFinished(seconds: Int) = "Aufnahme fertig (~$seconds s)."

    override fun recorderFailed(message: String?) = "Fehler bei der Aufnahme: $message"

    // ---- Impressum & Datenschutz ----

    override val imprintHeading = "Impressum"
    override val imprintResponsible =
        "Verantwortlich für diese App:\n\nStephan Reindl\nE-Mail: audiolex26@proton.me"
    override val imprintNonCommercial =
        "Nicht-kommerzielles Projekt. Der Quelltext ist offen (Apache-2.0), " +
            "die App wird ohne finanzielle Interessen bereitgestellt."
    override val imprintNotMedical =
        "AudioLex ist ein privates Übungswerkzeug, kein professionelles oder " +
            "medizinisches Produkt. Es ersetzt keine Beratung beim " +
            "Hörgeräteakustiker und keine Abklärung in der HNO-Heilkunde."
    override val imprintVibeCoded =
        "AudioLex ist im Zusammenspiel mit Claude Code entstanden — vibe-codiert: " +
            "Idee, Fachkonzept, Entscheidungen und Abnahme kommen vom Autor, ein " +
            "großer Teil des Quelltexts von einer KI. Nachprüfbar ist das im offenen " +
            "Repository: Jede wesentliche Entscheidung steht dort als ADR, jeder " +
            "Umsetzungsschritt im Journal."
    override val imprintAsIs =
        "Nutzung wie besehen: Die App wird so bereitgestellt, wie sie ist — ohne " +
            "Gewährleistung und ohne Zusage, dass das Training wirkt, die App " +
            "fehlerfrei läuft oder deine Daten erhalten bleiben. Die Nutzung " +
            "erfolgt auf eigenes Risiko; für Schäden daraus wird keine Haftung " +
            "übernommen. Dasselbe steht in der Apache-2.0-Lizenz, unter der der " +
            "Quelltext steht."
    override val privacyHeading = "Datenschutz"
    override val privacyNoTrackers = "AudioLex hat keine Tracker, keine Analyse-Dienste und keine Werbung."
    override val privacyLocalOnly =
        "Wortschatz, Bewertungen und Sitzungsverlauf liegen ausschließlich lokal " +
            "auf diesem Gerät. Es gibt keine Cloud, kein Konto, keine Übertragung ins " +
            "Internet."
    override val privacyOnePermission =
        "AudioLex fordert eine einzige Android-Berechtigung an: Mikrofon, nur für " +
            "selbst ausgelöste Aufnahmen eigener Wörter und Sätze. Eine " +
            "Internet-Berechtigung gibt es nicht. Die App kann von sich aus nichts " +
            "übertragen."
    override val privacyBackupIsYours =
        "Eigene Aufnahmen und der Sitzungsverlauf lassen sich sichern: Auf " +
            "Tastendruck schreibt AudioLex beides als ZIP-Datei in deine Dokumente. " +
            "Das geschieht nur, wenn du es auslöst. Was danach mit der Datei passiert " +
            "— kopieren, weitergeben, in eine Cloud laden — entscheidest du."
    override val privacyNoSystemBackup =
        "Androids automatische Sicherung ist für AudioLex abgeschaltet, weil sie die " +
            "Aufnahmen ungefragt in ein Google-Konto übertragen hätte. Das hat eine " +
            "Kehrseite: Ohne eigenen Export gibt es keine Kopie."

    // ---- Kurzanleitung ----

    override val guideTitle = "Kurzanleitung"
    override val guideIntro =
        "Die App spielt ein Wort, du ordnest es zu, und wie sicher das saß, steuert, wann es wiederkommt. " +
            "Mehr als Kopfhörer oder dein Hörgerät und eine ruhige Minute braucht es nicht."
    override val guideLearningHeading = "Lernmodus"
    override val guideLearningBody =
        "Wort hören, Text mitlesen. „Wiederholen“ spielt es noch einmal, „Weiter“ geht zum nächsten, " +
            "„Vorheriges“ zurück. Bewertet wird hier nichts — der Modus baut die Verbindung zwischen Klang " +
            "und Schriftbild auf. Fang damit an, wenn ein Wort dir noch fremd ist."
    override val guideExamHeading = "Prüfmodus"
    override val guideExamBody =
        "Das Wort läuft, die Karte bleibt verdeckt. Erst wenn du sie antippst, siehst du den Text — und " +
            "bewertest dich selbst. Die Karte hat immer dieselbe Größe, damit ihre Silhouette die Wortlänge " +
            "nicht verrät."
    override val guideRatingBody =
        "Fünf Stufen steuern, wann die Karte wiederkommt: Sofort nach 1 Minute, Bald nach 10 Minuten, " +
            "Später nach einem Tag, Gut nach einer Woche, Perfekt nach einem Monat. Das sind keine Noten. " +
            "„Sofort“ ist kein schlechtes Ergebnis, sondern die Ansage, dass du das Wort gleich noch einmal " +
            "hören willst."
    override val guideSettingsHeading = "Einstellungen"
    override val guideSettingsLevel =
        "Die Trainingsstufe ist die Ein-Tipp-Voreinstellung: Einfach schaltet das Störgeräusch ab, " +
            "Schwierig legt es hörbar unter die Sprache (SNR +5 dB), Fortgeschritten darüber (SNR −5 dB). " +
            "Drehst du danach selbst am Regler, steht dort „Individuell eingestellt“."
    override val guideSettingsContent =
        "Trainingsinhalt schaltet zwischen Wörtern und Sätzen. Unter „Korpus“ wählst du, welche Sprecher " +
            "mitspielen — die mitgelieferte Stimme ist einer davon."
    override val guideSettingsNoise =
        "Störgeräusch legt einen Loop unter die Sprache. Der Regler bestimmt den Abstand in Dezibel; weiter " +
            "rechts heißt lauteres Geräusch. Solange du kein eigenes Geräusch aufgenommen oder importiert " +
            "hast, bleibt der Ton sauber."
    override val guideSettingsOutput =
        "Unter „Ausgabe“ steht, was die App gerade erkennt. Am Bluetooth-Hörgerät ist die Kanalwahl " +
            "wirkungslos — es summiert Stereo zu Mono —, deshalb ist sie dort ausgegraut statt versteckt."
    override val guideOwnRecordingsHeading = "Eigene Aufnahmen"
    override val guideOwnRecordingsBody =
        "Du kannst Wörter und Sätze selbst einsprechen und den Text dazu schreiben. Ohne Text lässt sich ein " +
            "Eintrag nicht trainieren, nachtragen geht aber jederzeit. Der Sprechername hält Stimmen " +
            "auseinander; in den Einstellungen schaltest du einzelne davon zu oder ab."
    override val guideBackupHeading = "Sicherung"
    override val guideBackupBody =
        "Von allein verlässt nichts dieses Gerät — die App hat keine Internet-Berechtigung. Die Kehrseite: " +
            "Ohne eigenen Export gibt es keine Kopie. „Exportieren“ legt Aufnahmen, Geräusche und " +
            "Sitzungsverlauf als ZIP in deine Dokumente, „Importieren“ liest sie zurück und fügt nur hinzu, " +
            "was fehlt."

    // ---- Kanaltest ----

    override val channelTestTitle = "Kanaltest"
    override val channelTestExplainer =
        "Beide Ohren bekommen gleichzeitig drei verschiedene Wörter. „Nur links“ setzt den rechten " +
            "Kanal auf exakt null — was dann rechts noch zu hören ist, kommt nicht aus der App."
    override val channelTestLoading = "Lade Korpus…"
    override val channelTestReady = "Bereit"
    override val channelTestTooFewWords =
        "Für den Kanaltest fehlen Wörter: Er braucht sechs mit Aufnahme in der eingestellten " +
            "Trainingssprache."

    override fun channelTestLeftEar(words: List<String>) = "Links: ${words.joinToString(" · ")}"

    override fun channelTestRightEar(words: List<String>) = "Rechts: ${words.joinToString(" · ")}"

    override fun channelTestPlaying(channels: String) = "Kanaltest: $channels…"

    override fun channelTestPlayingWord(word: String, speaker: String) = "Spiele „$word“ ($speaker)…"
}
