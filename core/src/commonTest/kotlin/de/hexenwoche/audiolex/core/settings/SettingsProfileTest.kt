package de.hexenwoche.audiolex.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Training levels (Backlog M4 "Szenario-Presets Einfach/Schwierig/
 * Fortgeschritten", AC1): applying a level writes exactly the atomic noise
 * pair ([SettingsProfile] is the single definition of the parameters), and
 * the active level is re-derived from those atomic values -- the level
 * itself is never persisted.
 */
class SettingsProfileTest {

    private fun settings(
        noiseEnabled: Boolean = false,
        snrDb: Int = 5,
        noiseScenario: String = "restaurant",
    ): AppSettings =
        AppSettings(ThemeMode.SYSTEM, noiseEnabled = noiseEnabled, snrDb = snrDb, noiseScenario = noiseScenario)

    // --- applying a level

    @Test
    fun `einfach turns the noise off and leaves the snr untouched`() {
        for (snr in listOf(SNR_DB_MIN, 0, 5, 12, SNR_DB_MAX)) {
            val result = settings(noiseEnabled = true, snrDb = snr).applyProfile(SettingsProfile.EINFACH)

            assertEquals(false, result.noiseEnabled, "snrDb=$snr: noise must be off")
            assertEquals(snr, result.snrDb, "snrDb=$snr: the SNR must stay untouched")
        }
    }

    @Test
    fun `schwierig turns the noise on at plus five dB`() {
        val result = settings(noiseEnabled = false, snrDb = 20).applyProfile(SettingsProfile.SCHWIERIG)

        assertEquals(true, result.noiseEnabled)
        assertEquals(5, result.snrDb)
    }

    @Test
    fun `fortgeschritten turns the noise on at minus five dB`() {
        val result = settings(noiseEnabled = false, snrDb = 20).applyProfile(SettingsProfile.FORTGESCHRITTEN)

        assertEquals(true, result.noiseEnabled)
        assertEquals(-5, result.snrDb)
    }

    @Test
    fun `applying a level leaves the scenario and every other setting untouched`() {
        val base = AppSettings(
            themeMode = ThemeMode.DARK,
            corpusMode = CorpusMode.SAETZE,
            noiseEnabled = true,
            snrDb = 12,
            noiseScenario = "verkehr",
            channelMode = ChannelMode.NUR_LINKS,
            excludedSpeakers = setOf("kerstin"),
        )

        for (profile in SettingsProfile.entries) {
            val result = base.applyProfile(profile)

            assertEquals(base.themeMode, result.themeMode, "$profile: themeMode")
            assertEquals(base.corpusMode, result.corpusMode, "$profile: corpusMode")
            assertEquals(base.noiseScenario, result.noiseScenario, "$profile: noiseScenario")
            assertEquals(base.channelMode, result.channelMode, "$profile: channelMode")
            assertEquals(base.excludedSpeakers, result.excludedSpeakers, "$profile: excludedSpeakers")
        }
    }

    // --- deriving the level from the atomic values

    @Test
    fun `noise off is einfach at any snr`() {
        for (snr in listOf(SNR_DB_MIN, -1, 0, 5, 13, SNR_DB_MAX)) {
            assertEquals(
                SettingsProfile.EINFACH,
                settings(noiseEnabled = false, snrDb = snr).derivedProfile(),
                "snrDb=$snr with noise off must be EINFACH",
            )
        }
    }

    @Test
    fun `noise on at plus five dB is schwierig`() {
        assertEquals(SettingsProfile.SCHWIERIG, settings(noiseEnabled = true, snrDb = 5).derivedProfile())
    }

    @Test
    fun `noise on at minus five dB is fortgeschritten`() {
        assertEquals(SettingsProfile.FORTGESCHRITTEN, settings(noiseEnabled = true, snrDb = -5).derivedProfile())
    }

    @Test
    fun `noise on at any other snr is no level`() {
        for (snr in listOf(-4, 0, 4, 6, 12, SNR_DB_MAX)) {
            assertNull(
                settings(noiseEnabled = true, snrDb = snr).derivedProfile(),
                "snrDb=$snr with noise on must derive no level",
            )
        }
    }

    @Test
    fun `applying a level then deriving it back yields the same level`() {
        val base = settings(noiseEnabled = true, snrDb = 12)

        for (profile in SettingsProfile.entries) {
            assertEquals(profile, base.applyProfile(profile).derivedProfile(), "$profile must roundtrip")
        }
    }
}
