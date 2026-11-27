package com.semyqofu.aliasyou.ui.screens

import android.app.NotificationManager
import android.media.AudioManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoundEndSoundTest {

    @Test
    fun `silent and vibrate ringer modes block sound`() {
        assertFalse(
            isSoundPlaybackAllowed(
                ringerMode = AudioManager.RINGER_MODE_SILENT,
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL,
                musicVolume = 10,
                isMusicMuted = false
            )
        )
        assertFalse(
            isSoundPlaybackAllowed(
                ringerMode = AudioManager.RINGER_MODE_VIBRATE,
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL,
                musicVolume = 10,
                isMusicMuted = false
            )
        )
    }

    @Test
    fun `zero volume or muted media stream blocks sound`() {
        assertFalse(
            isSoundPlaybackAllowed(
                ringerMode = AudioManager.RINGER_MODE_NORMAL,
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL,
                musicVolume = 0,
                isMusicMuted = false
            )
        )
        assertFalse(
            isSoundPlaybackAllowed(
                ringerMode = AudioManager.RINGER_MODE_NORMAL,
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL,
                musicVolume = 8,
                isMusicMuted = true
            )
        )
    }

    @Test
    fun `do not disturb interruption filters block sound`() {
        assertFalse(
            isSoundPlaybackAllowed(
                ringerMode = AudioManager.RINGER_MODE_NORMAL,
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                musicVolume = 8,
                isMusicMuted = false
            )
        )
        assertFalse(
            isSoundPlaybackAllowed(
                ringerMode = AudioManager.RINGER_MODE_NORMAL,
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_NONE,
                musicVolume = 8,
                isMusicMuted = false
            )
        )
    }

    @Test
    fun `normal mode with active media volume allows sound`() {
        assertTrue(
            isSoundPlaybackAllowed(
                ringerMode = AudioManager.RINGER_MODE_NORMAL,
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL,
                musicVolume = 5,
                isMusicMuted = false
            )
        )
        assertTrue(
            isSoundPlaybackAllowed(
                ringerMode = AudioManager.RINGER_MODE_NORMAL,
                interruptionFilter = null,
                musicVolume = 5,
                isMusicMuted = false
            )
        )
    }
}
