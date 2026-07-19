package de.hexenwoche.audiolex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * The fixed-frame display area for a target word/sentence, shared between
 * [LernmodusScreen] (text always visible, no reveal/click) and the
 * Prüfmodus [RevealCard] (renders this once revealed) -- so the text metric
 * lives in exactly one place instead of two copies drifting apart (Backlog
 * M2 "Lernmodus: Zieltext in festem Kartenrahmen analog Prüfmodus").
 *
 * A constant-size `Surface` (280x160dp, `tonalElevation = 4.dp`), matching
 * the pre-existing `RevealCard` frame -- unified look across both training
 * modes, position/size stable across word and sentence entries (DESIGN.md
 * "positionsstabil").
 *
 * Text styling: explicitly neutral (`onSurface`), never the accent color --
 * the accent is reserved for active elements (DESIGN.md). Words
 * ([isSentence] false) keep the exact pre-existing style: one line,
 * shrink-to-fit instead of wrapping (a whole word must not wrap or move,
 * only scale down for overlength). Sentences may wrap up to three lines
 * within the same fixed frame instead -- a whole sentence on one
 * shrink-to-fit line would be illegibly small (Satz-Bogen Batch B, AC5).
 * Sentences additionally get `lineHeight = 1.2.em` instead of `displayLarge`'s
 * fixed 64sp default: `autoSize`'s step search only ever rescales the *font
 * size* it feeds into the style per trial layout, never `lineHeight` --
 * verified against the real 1.8.2 foundation jar
 * (`MultiParagraphLayoutCache$TextAutoSizeLayoutScopeImpl.performLayout`
 * copies the style with only `fontSize` overridden). A fixed sp `lineHeight`
 * would therefore stay at 64sp even once the font shrinks to fit and could
 * overflow the frame; `em` is relative and resolves against whichever
 * `fontSize` that trial step is using, so it shrinks together with the font
 * (RevealCard-Zeilenabstand-Fix, Prüfmodus-Bug 2026-07-19). The 24sp
 * `autoSize` minimum applies to both kinds.
 */
@Composable
internal fun TargetTextCard(text: String, isSentence: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.width(280.dp).height(160.dp),
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val displayLarge = MaterialTheme.typography.displayLarge
            val textStyle = if (isSentence) {
                displayLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    lineHeight = 1.2.em,
                )
            } else {
                displayLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
            BasicText(
                text = text,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                style = textStyle,
                maxLines = if (isSentence) 3 else 1,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 24.sp,
                    maxFontSize = displayLarge.fontSize,
                ),
            )
        }
    }
}
