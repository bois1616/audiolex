package de.hexenwoche.audiolex

import androidx.compose.runtime.staticCompositionLocalOf
import de.hexenwoche.audiolex.core.i18n.Strings
import de.hexenwoche.audiolex.core.i18n.UiLanguage
import de.hexenwoche.audiolex.core.i18n.stringsFor

/**
 * The active string catalog, handed down from [App] (ADR-0015). Every screen
 * reads its texts through `LocalStrings.current` instead of holding literals.
 *
 * `staticCompositionLocalOf`, not `compositionLocalOf`: the value changes
 * only when the user picks another language, and when it does, everything on
 * screen has to be redrawn anyway. The static variant skips maintaining
 * fine-grained read tracking for a value that is read on nearly every line
 * and written about once a year.
 *
 * The default resolves [UiLanguage.SYSTEM] rather than throwing, so a
 * composable rendered without the provider shows the device language instead
 * of crashing. In the running app the provider in [App] always wins; the
 * default only matters if a screen is ever hosted somewhere else.
 */
val LocalStrings = staticCompositionLocalOf<Strings> { stringsFor(UiLanguage.SYSTEM) }
