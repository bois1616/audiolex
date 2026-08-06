package de.hexenwoche.audiolex.core.audio

/**
 * Which physical output is currently active, as far as channel-separation
 * training is concerned (ADR-0011): a stereo pair with one driver per ear
 * ([STEREO_KOPFHOERER]), or anything that ends up mono at the trained ear
 * ([HOERGERAET], the reference setup from ADR-0007 and also the fallback for
 * everything ambiguous -- see the detection table in ADR-0011 point 2). This
 * is a plain named state, not persisted: it reflects the currently connected
 * hardware, so `:composeApp` re-detects it on every process start and on
 * every device change instead of storing a stale guess.
 */
enum class OutputSetup { HOERGERAET, STEREO_KOPFHOERER }
