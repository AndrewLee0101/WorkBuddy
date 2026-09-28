package com.arashiplayer.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.arashiplayer.data.model.DecoderMode
import com.arashiplayer.data.model.Playable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局播放器持有者。
 *
 * 视频与音乐共用同一个 [ExoPlayer] 实例，但策略不同：
 *  - 视频：进入播放页接管画面，退出即释放 Surface
 *  - 音乐：交给 MediaSession 前台服务，锁屏与通知栏可控
 *
 * 之所以保留单一实例，是为了让「视频里暂停、去音乐里继续」这件事语义清晰 ——
 * 任何时刻只有一路音频输出。
 */
class PlayerHolder(private val context: Context) {

    private var _player: ExoPlayer? = null
    val player: ExoPlayer get() = _player ?: build(DecoderMode.HW).also { _player = it }

    private val _decoderMode = MutableStateFlow(DecoderMode.HW)
    val decoderMode: StateFlow<DecoderMode> = _decoderMode.asStateFlow()

    private val _current = MutableStateFlow<Playable?>(null)
    val current: StateFlow<Playable?> = _current.asStateFlow()

    /** 当前播放列表（音乐场景使用） */
    private val queue = mutableListOf<Playable>()
    val queueSnapshot: List<Playable> get() = queue.toList()

    private var currentIndex = -1

    @OptIn(UnstableApi::class)
    private fun build(mode: DecoderMode): ExoPlayer {
        val renderers = DefaultRenderersFactory(context)
            // 硬解失败自动回落软解，兼容老旧/冷门编码
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("ArashiPlayer/1.0")
            .setAllowCrossProtocolRedirects(true)
        val dataSource = DefaultDataSource.Factory(context, httpFactory)

        val trackSelector = DefaultTrackSelector(context).apply {
            // 字幕、音轨优先选中文
            parameters = buildUponParameters()
                .setPreferredTextLanguage("zh")
                .setSelectUndeterminedTextLanguage(true)
                .build()
        }

        return ExoPlayer.Builder(context, renderers)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSource))
            .setTrackSelector(trackSelector)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(androidx.media3.common.C.WAKE_MODE_LOCAL)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = false
            }
    }

    /** 切换解码方式：重建实例并保持进度 */
    fun switchDecoder(mode: DecoderMode) {
        if (mode == _decoderMode.value && _player != null) return
        val old = _player
        val pos = old?.currentPosition ?: 0L
        val wasPlaying = old?.isPlaying == true
        val item = old?.currentMediaItem

        old?.release()
        _player = build(mode).also { p ->
            item?.let { p.setMediaItem(it) }
            p.prepare()
            p.seekTo(pos)
            p.playWhenReady = wasPlaying
        }
        _decoderMode.value = mode
    }

    /* ============================ 播放控制 ============================ */

    fun play(playable: Playable, startPositionMs: Long = 0L, autoPlay: Boolean = true) {
        _current.value = playable
        val p = player
        val item = MediaItem.Builder()
            .setUri(Uri.parse(playable.uri))
            .setMediaId(playable.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(playable.title)
                    .setArtist(playable.subtitle)
                    .build()
            )
            .build()
        p.setMediaItem(item)
        p.prepare()
        if (startPositionMs > 0) p.seekTo(startPositionMs)
        p.playWhenReady = autoPlay
    }

    /** 以整个列表为队列开播（音乐连播） */
    fun playQueue(list: List<Playable>, startIndex: Int, autoPlay: Boolean = true) {
        if (list.isEmpty()) return
        queue.clear()
        queue.addAll(list)
        currentIndex = startIndex.coerceIn(0, list.lastIndex)
        _current.value = queue[currentIndex]

        val items = list.map { pl ->
            MediaItem.Builder()
                .setUri(Uri.parse(pl.uri))
                .setMediaId(pl.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(pl.title)
                        .setArtist(pl.subtitle)
                        .build()
                )
                .build()
        }
        val p = player
        p.setMediaItems(items, currentIndex, 0L)
        p.prepare()
        p.playWhenReady = autoPlay
    }

    fun next() {
        val p = player
        if (p.hasNextMediaItem()) p.seekToNextMediaItem()
        else if (queue.isNotEmpty()) {
            currentIndex = (currentIndex + 1).coerceAtMost(queue.lastIndex)
            _current.value = queue.getOrNull(currentIndex)
        }
    }

    fun previous() {
        val p = player
        if (p.currentPosition > 3000) { p.seekTo(0); return }
        if (p.hasPreviousMediaItem()) p.seekToPreviousMediaItem()
        else if (queue.isNotEmpty()) {
            currentIndex = (currentIndex - 1).coerceAtLeast(0)
            _current.value = queue.getOrNull(currentIndex)
        }
    }

    fun togglePlayPause() {
        val p = player
        if (p.isPlaying) p.pause() else { p.play(); }
    }

    fun pause() = player.pause()
    fun resume() = player.play()

    fun seekTo(ms: Long) = player.seekTo(ms.coerceAtLeast(0L))

    fun seekBy(deltaMs: Long) = seekTo(player.currentPosition + deltaMs)

    fun setSpeed(speed: Float) = player.setPlaybackSpeed(speed.coerceIn(0.25f, 4f))

    fun setVolume(v: Float) = player.volume = v.coerceIn(0f, 1f)

    fun stop() {
        player.stop()
        player.clearMediaItems()
        _current.value = null
    }

    fun release() {
        _player?.release()
        _player = null
    }
}
