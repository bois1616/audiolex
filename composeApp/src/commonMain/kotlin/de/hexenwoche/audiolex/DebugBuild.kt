package de.hexenwoche.audiolex

/**
 * Whether this is a development build (Autor-Requirement 2026-08-07:
 * „Kanaltest ausblenden").
 *
 * The Dev-only Kanaltest is a diagnostic tool, not a feature -- it produced
 * the ASHA volume-state evidence chain in July and stays worth having on the
 * author's own device. It has no business in a build handed to anyone else,
 * least of all with the F-Droid/Play milestones in view (M6/M7). So it is
 * hidden rather than deleted: gone from every release build, one tap away in
 * every debug build.
 */
expect fun isDebugBuild(): Boolean
