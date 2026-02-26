package com.example.hexaplayer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    companion object {
        /** Audio session ID of the active ExoPlayer instance.
         *  Valid (non-zero) once the service is created. */
        var audioSessionId: Int = 0
            private set
    }

    /** Pauses playback when Bluetooth/headphones are disconnected. */
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                mediaSession?.player?.pause()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        audioSessionId = player.audioSessionId
        mediaSession = MediaSession.Builder(this, player).build()
        registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        unregisterReceiver(noisyReceiver)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        audioSessionId = 0
        super.onDestroy()
    }
}
