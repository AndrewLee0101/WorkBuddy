package com.arashiplayer.player

import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * 音乐后台播放服务。
 * 让音乐在锁屏 / 通知栏 / 蓝牙耳机上可控，也保证切到后台不断音。
 */
class ArashiPlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player: ExoPlayer = com.arashiplayer.ArashiApp.get().playerHolder.player
        session = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {})
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
