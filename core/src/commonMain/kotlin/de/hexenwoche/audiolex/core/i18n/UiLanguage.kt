package de.hexenwoche.audiolex.core.i18n

import de.hexenwoche.audiolex.core.corpus.primaryLanguageSubtag

/**
 * Which language the UI is written in (ADR-0015). Deliberately *not* the
 * language of the corpus: what the app says and what it plays apart are two
 * settings, and only this one exists so far -- the corpus stays German
 * (Backlog "Sprach-Bogen", zurückgestellt).
 *
 * [SYSTEM] is the default and follows the platform locale, the same shape as
 * [de.hexenwoche.audiolex.core.settings.ThemeMode.SYSTEM]. It is what keeps
 * an existing German install German without a migration writing anything:
 * the column defaults to SYSTEM, the device says `de`, nothing visibly
 * changes. A device set to anything the app has no catalog for lands on
 * [ENGLISCH] via [resolve] -- English is the wider net of the two, not the
 * author's own language.
 *
 * [nativeName] is each language's name *in itself* ("Deutsch", "English"),
 * not translated into the currently active one. That's the convention every
 * language picker follows, and it's the only version a reader who can't read
 * the current UI language will recognize -- which is exactly the person
 * using the picker.
 */
enum class UiLanguage(val nativeName: String) {
    SYSTEM(nativeName = ""),
    DEUTSCH(nativeName = "Deutsch"),
    ENGLISCH(nativeName = "English"),
    ;

    /**
     * The concrete language to render in: [SYSTEM] asks [systemLanguageTag],
     * everything else is already concrete. Never returns [SYSTEM].
     *
     * [systemTag] is matched on its primary subtag only, so `de`, `de-AT`,
     * `de_DE` and `DE` all count as German -- BCP-47 tags carry region and
     * script the app has no opinion about, and an Austrian device must not
     * fall through to English on a technicality.
     */
    fun resolve(systemTag: String = systemLanguageTag()): UiLanguage = when (this) {
        SYSTEM -> if (primaryLanguageSubtag(systemTag) == "de") DEUTSCH else ENGLISCH
        else -> this
    }
}

/**
 * The platform's current language as a BCP-47 primary subtag (`de`, `en`,
 * ...), lowercase, or blank when the platform has nothing to say. Read fresh
 * on every call rather than cached: on Android the user can change the
 * device language while the process lives.
 */
expect fun systemLanguageTag(): String

/** The catalog for a language; [UiLanguage.SYSTEM] resolves first (see [UiLanguage.resolve]). */
fun stringsFor(language: UiLanguage, systemTag: String = systemLanguageTag()): Strings =
    when (language.resolve(systemTag)) {
        UiLanguage.DEUTSCH -> GermanStrings
        else -> EnglishStrings
    }
