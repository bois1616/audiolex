package de.hexenwoche.audiolex.core.corpus

/**
 * Which language a corpus entry is *filed under* (Autor-Entscheid
 * 2026-08-24, ADR-0016) -- deliberately not a claim about what the audio
 * actually contains.
 *
 * The speaker declares it when creating an entry, and that declaration only
 * decides **where the entry shows up and gets used**. If Andy files his
 * recordings as German and then slips an English sentence in, that sentence
 * stays under German. The app neither checks nor could check: it never sees
 * the text as anything but a string the user typed, and the audio is opaque
 * to it. Pretending otherwise would mean detection that is wrong sometimes
 * and unexplainable when it is.
 *
 * The built-in corpus is filed by the same rule through [Word.language],
 * which has carried a BCP-47 tag since M1 -- this enum is the small, closed
 * set of drawers the UI offers, matched against those tags by
 * [primaryLanguageSubtag].
 */
enum class CorpusLanguage(val languageTag: String) {
    DEUTSCH("de-DE"),
    ENGLISCH("en-US"),
    ;

    /**
     * Whether an entry tagged [tag] belongs in this drawer. Compared on the
     * primary subtag only: `en-US`, `en-GB` and a hand-edited `en` all file
     * under [ENGLISCH]. A tag the app has no drawer for -- someone's
     * `zh-CN` -- matches nothing and is simply not shown, which is the
     * honest outcome rather than a fake catch-all.
     */
    fun matches(tag: String): Boolean = primaryLanguageSubtag(tag) == primaryLanguageSubtag(languageTag)
}

/**
 * `de-AT` / `de_DE` / ` DE ` -> `de`; blank stays blank. Shared by
 * [CorpusLanguage.matches] and
 * [de.hexenwoche.audiolex.core.i18n.UiLanguage.resolve] -- the two answer
 * different questions (what is shown vs. what the app speaks) but read tags
 * the same way, and two copies of this would drift.
 */
fun primaryLanguageSubtag(tag: String): String =
    tag.trim().substringBefore('-').substringBefore('_').lowercase()
