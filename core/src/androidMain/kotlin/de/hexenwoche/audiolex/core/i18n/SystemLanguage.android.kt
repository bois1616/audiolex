package de.hexenwoche.audiolex.core.i18n

import java.util.Locale

// Locale.getDefault() follows the device language on Android -- the process
// default is updated on a configuration change -- so this needs nothing
// observed or registered. Read per call, never cached, because the user can
// change the device language while the process lives.
actual fun systemLanguageTag(): String = Locale.getDefault().language
