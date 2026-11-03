package com.mplayer.app

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Owns the audio player independently of the Activity lifecycle.
 * Media3 publishes the media notification and system transport controls
 * through the MediaSession associated with this service.
 */
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        val audioPrefs = getSharedPreferences("mplayer_user_data", MODE_PRIVATE)
        fun saveAudioSessionId() {
            val sessionId = player.audioSessionId
            if (sessionId > 0) audioPrefs.edit().putInt("audio_session_id", sessionId).apply()
        }
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                saveAudioSessionId()
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                saveAudioSessionId()
            }
        })
        saveAudioSessionId()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
