package com.mplayer.app

import androidx.media3.exoplayer.ExoPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Owns the audio player independently of the Activity lifecycle.
 * Media3 publishes the media notification and system transport controls
 * through the MediaSession associated with this service.
 */
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var fadeHandler: Handler? = null
    private var fadeTask: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        val preferences = getSharedPreferences("mplayer_user_data", MODE_PRIVATE)
        val fadeHandler = Handler(Looper.getMainLooper())
        var fadeOutStartedAt = 0L
        var fadeInStartedAt = 0L
        var lastMediaId: String? = null
        val fadeDurationMs = 3_500L

        val fadeTask = object : Runnable {
            override fun run() {
                val enabled = preferences.getBoolean("dj_mode_enabled", false)
                if (!enabled) {
                    player.volume = 1f
                    fadeOutStartedAt = 0L
                    fadeInStartedAt = 0L
                    fadeHandler.postDelayed(this, 250L)
                    return
                }
                if (player.isPlaying) {
                    val now = SystemClock.elapsedRealtime()
                    val duration = player.duration
                    val remaining = if (duration > 0L) duration - player.currentPosition else Long.MAX_VALUE
                    if (fadeOutStartedAt == 0L && remaining in 1..fadeDurationMs &&
                        player.hasNextMediaItem() && player.repeatMode != androidx.media3.common.Player.REPEAT_MODE_ONE) {
                        fadeOutStartedAt = now
                    }
                    if (fadeOutStartedAt > 0L) {
                        val progress = ((now - fadeOutStartedAt).toFloat() / fadeDurationMs).coerceIn(0f, 1f)
                        player.volume = 1f - progress
                    } else if (fadeInStartedAt > 0L) {
                        val progress = ((now - fadeInStartedAt).toFloat() / fadeDurationMs).coerceIn(0f, 1f)
                        player.volume = progress
                        if (progress >= 1f) fadeInStartedAt = 0L
                    }
                }
                fadeHandler.postDelayed(this, 100L)
            }
        }
        this.fadeHandler = fadeHandler
        this.fadeTask = fadeTask
        fadeHandler.post(fadeTask)
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
                if (mediaItem?.mediaId != lastMediaId) {
                    lastMediaId = mediaItem?.mediaId
                    fadeOutStartedAt = 0L
                    fadeInStartedAt = if (preferences.getBoolean("dj_mode_enabled", false)) SystemClock.elapsedRealtime() else 0L
                    if (fadeInStartedAt > 0L) player.volume = 0f else player.volume = 1f
                }
            }
        })
        saveAudioSessionId()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        fadeTask?.let { fadeHandler?.removeCallbacks(it) }
        fadeHandler?.removeCallbacksAndMessages(null)
        fadeTask = null
        fadeHandler = null
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
