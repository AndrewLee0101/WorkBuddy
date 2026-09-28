package com.arashiplayer.ui.screens.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arashiplayer.ArashiApp
import com.arashiplayer.data.local.OnlineMetaEntity
import com.arashiplayer.data.local.PlaylistEntity
import com.arashiplayer.data.local.PlaylistTrackEntity
import com.arashiplayer.data.model.LyricLine
import com.arashiplayer.data.model.MediaFolder
import com.arashiplayer.data.model.MusicTrack
import com.arashiplayer.data.model.OnlineMeta
import com.arashiplayer.data.model.Playable
import com.arashiplayer.data.net.OnlineMetaApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 音乐库分段 */
enum class MusicTab(val label: String) {
    SONGS("歌曲"),
    ARTISTS("歌手"),
    ALBUMS("专辑"),
    FOLDERS("文件夹"),
}

/** 音乐库整体状态 */
data class MusicUiState(
    val loading: Boolean = true,
    val tracks: List<MusicTrack> = emptyList(),
    val folders: List<MediaFolder> = emptyList(),
) {
    val artists: List<ArtistGroup> get() = tracks.groupBy { it.artist }
        .map { (name, list) -> ArtistGroup(name, list) }
        .sortedBy { it.name }

    val albums: List<AlbumGroup> get() = tracks
        .groupBy { it.albumId to it.album }
        .map { (k, list) ->
            AlbumGroup(
                albumId = k.first,
                name = k.second,
                artist = list.firstOrNull()?.artist ?: "",
                tracks = list,
            )
        }
        .sortedBy { it.name }
}

data class ArtistGroup(val name: String, val tracks: List<MusicTrack>) {
    val albumCount: Int get() = tracks.map { it.album }.distinct().size
}

data class AlbumGroup(
    val albumId: Long,
    val name: String,
    val artist: String,
    val tracks: List<MusicTrack>,
) {
    val coverUri: String? get() = tracks.firstOrNull()?.embeddedCoverUri
}

/**
 * 音乐库 + 正在播放的共享 ViewModel。
 *
 * 本地曲库来自 MediaStore；在线封面 / 歌词按需匹配，命中后写回 [com.arashiplayer.data.local.MetaDao]，
 * 下次打开直接读缓存，不再走网络。
 */
class MusicViewModel(app: Application) : AndroidViewModel(app) {

    private val arashi = ArashiApp.of(app)
    private val mediaRepo = arashi.mediaRepo
    private val onlineApi = arashi.onlineApi
    private val metaDao = arashi.database.metaDao()

    val playerHolder = arashi.playerHolder

    private val _state = MutableStateFlow(MusicUiState())
    val state: StateFlow<MusicUiState> = _state.asStateFlow()

    /** 当前曲目对应的在线元数据 */
    private val _meta = MutableStateFlow<OnlineMeta?>(null)
    val meta: StateFlow<OnlineMeta?> = _meta.asStateFlow()

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    private val _matching = MutableStateFlow(false)
    val matching: StateFlow<Boolean> = _matching.asStateFlow()

    /** 当前曲目是否已收藏（收藏 = 「我喜欢」歌单里的一条记录） */
    private val _favorite = MutableStateFlow(false)
    val favorite: StateFlow<Boolean> = _favorite.asStateFlow()

    private var metaTrackKey: String? = null

    init {
        refresh()
    }

    /* ============================ 曲库 ============================ */

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val tracks = runCatching { mediaRepo.loadMusic() }.getOrDefault(emptyList())
            val folders = runCatching { mediaRepo.musicFolders(tracks) }.getOrDefault(emptyList())
            _state.update { it.copy(loading = false, tracks = tracks, folders = folders) }
        }
    }

    fun tracksInFolder(folder: MediaFolder): List<MusicTrack> =
        _state.value.tracks.filter { it.path.substringBeforeLast('/') == folder.path }

    /* ============================ 播放 ============================ */

    /** 以 list 为整个播放队列，从 index 开始播放 */
    fun playQueue(list: List<MusicTrack>, index: Int) {
        if (list.isEmpty()) return
        playerHolder.playQueue(list.map { Playable.Music(it) }, index)
    }

    /* ============================ 在线元数据 ============================ */

    /**
     * 载入当前曲目的封面 / 歌词：优先读数据库缓存，未命中且允许联网时自动匹配。
     */
    fun loadMeta(track: MusicTrack, allowMatch: Boolean) {
        if (metaTrackKey == track.key && _meta.value != null) return
        metaTrackKey = track.key
        _meta.value = null
        _lyrics.value = emptyList()

        viewModelScope.launch {
            val cached = runCatching { metaDao.find(track.key) }.getOrNull()?.toModel()
            if (cached != null && (!cached.coverUrl.isNullOrBlank() || !cached.lyric.isNullOrBlank())) {
                applyMeta(cached)
                return@launch
            }
            if (cached != null) applyMeta(cached)
            if (allowMatch) matchingInternal(track)
        }
    }

    /** 右上角「重新匹配」 */
    fun rematch(track: MusicTrack) {
        metaTrackKey = track.key
        viewModelScope.launch { matchingInternal(track) }
    }

    private suspend fun matchingInternal(track: MusicTrack) {
        _matching.value = true
        val m = runCatching { onlineApi.match(track) }.getOrNull()
        _matching.value = false
        if (m == null) return
        if (m.coverUrl.isNullOrBlank() && m.lyric.isNullOrBlank()) return
        runCatching { metaDao.upsert(OnlineMetaEntity.from(track.key, m)) }
        applyMeta(m)
    }

    private fun applyMeta(m: OnlineMeta) {
        _meta.value = m
        _lyrics.value = OnlineMetaApi.parseLyric(m.lyric, m.translatedLyric)
    }

    /* ============================ 收藏（「我喜欢」歌单） ============================ */

    fun loadFavorite(track: MusicTrack) {
        viewModelScope.launch {
            _favorite.value = runCatching { isFavorite(track) }.getOrDefault(false)
        }
    }

    fun toggleFavorite(track: MusicTrack) {
        viewModelScope.launch {
            runCatching {
                val dao = arashi.database.playlistDao()
                val playlistId = dao.observeAll().first().firstOrNull { it.name == FAVORITE_PLAYLIST }?.id
                    ?: dao.insert(
                        PlaylistEntity(
                            name = FAVORITE_PLAYLIST,
                            createdAt = System.currentTimeMillis(),
                            coverUri = track.embeddedCoverUri,
                        )
                    )
                val existing = dao.tracksOf(playlistId)
                if (existing.any { it.trackId == track.id }) {
                    dao.removeTrack(playlistId, track.id)
                    _favorite.value = false
                } else {
                    dao.addTrack(
                        PlaylistTrackEntity(
                            playlistId = playlistId,
                            trackId = track.id,
                            orderIndex = existing.size,
                        )
                    )
                    _favorite.value = true
                }
            }
        }
    }

    private suspend fun isFavorite(track: MusicTrack): Boolean {
        val dao = arashi.database.playlistDao()
        val id = dao.observeAll().first().firstOrNull { it.name == FAVORITE_PLAYLIST }?.id ?: return false
        return dao.tracksOf(id).any { it.trackId == track.id }
    }

    companion object {
        const val FAVORITE_PLAYLIST = "我喜欢"
    }
}
