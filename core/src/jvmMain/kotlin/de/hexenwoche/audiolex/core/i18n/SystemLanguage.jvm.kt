package de.hexenwoche.audiolex.core.i18n

import java.util.Locale

// Same one-liner as the Android actual (like the Clock pair): on the desktop
// target this is the JVM's startup locale, derived from LANG/LC_ALL.
actual fun systemLanguageTag(): String = Locale.getDefault().language
