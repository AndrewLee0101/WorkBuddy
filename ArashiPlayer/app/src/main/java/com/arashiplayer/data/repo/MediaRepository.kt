package com.arashiplayer.data.repo

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.arashiplayer.data.model.MediaFolder
import com.arashiplayer.data.model.MusicTrack
import com.arashiplayer.data.model.VideoItem
import com.arashiplayer.util.AUDIO_EXTENSIONS
import com.arashiplayer.util.VIDEO_EXTENSIONS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 本地媒体扫描，全部走 MediaStore，兼容 Android 10+ 分区存储。
 *
 * 注意：加密空间里的文件后缀被改成 `.arv`，MediaStore 不会把它们识别成音视频，
 * 因此这里天然扫描不到 —— 这正是「其他软件看不到」的关键。
 */
class MediaRepository(private val context: Context) {

    /* ============================ 视频 ============================ */

    private val videoProjection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.TITLE,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DATA,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.WIDTH,
        MediaStore.Video.Media.HEIGHT,
        MediaStore.Video.Media.DATE_ADDED,
        MediaStore.Video.Media.DATE_MODIFIED,
        MediaStore.Video.Media.BUCKET_ID,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Video.Media.MIME_TYPE,
    )

    suspend fun loadVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val out = ArrayList<VideoItem>()
        runCatching {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection, null, null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { c -> while (c.moveToNext()) out += c.toVideoItem() }
        }
        if (out.isEmpty()) out += scanVideoFiles()
        out.distinctBy { it.path }
    }

    private fun Cursor.strOrEmpty(name: String): String {
        val i = getColumnIndex(name)
        return if (i >= 0 && !isNull(i)) getString(i) ?: "" else ""
    }

    private fun Cursor.longOrZero(name: String): Long {
        val i = getColumnIndex(name)
        return if (i >= 0 && !isNull(i)) getLong(i) else 0L
    }

    private fun Cursor.intOrZero(name: String): Int {
        val i = getColumnIndex(name)
        return if (i >= 0 && !isNull(i)) getInt(i) else 0
    }

    private fun Cursor.toVideoItem(): VideoItem {
        val id = longOrZero(MediaStore.Video.Media._ID)
        val path = strOrEmpty(MediaStore.Video.Media.DATA)
        val name = strOrEmpty(MediaStore.Video.Media.DISPLAY_NAME).ifBlank { File(path).name }
        return VideoItem(
            id = id,
            title = name.substringBeforeLast('.').ifBlank { strOrEmpty(MediaStore.Video.Media.TITLE) },
            path = path,
            uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString(),
            durationMs = longOrZero(MediaStore.Video.Media.DURATION),
            sizeBytes = longOrZero(MediaStore.Video.Media.SIZE),
            width = intOrZero(MediaStore.Video.Media.WIDTH),
            height = intOrZero(MediaStore.Video.Media.HEIGHT),
            dateAdded = longOrZero(MediaStore.Video.Media.DATE_ADDED) * 1000,
            dateModified = longOrZero(MediaStore.Video.Media.DATE_MODIFIED) * 1000,
            bucketId = longOrZero(MediaStore.Video.Media.BUCKET_ID),
            folderName = strOrEmpty(MediaStore.Video.Media.BUCKET_DISPLAY_NAME).ifBlank {
                path.substringBeforeLast('/').substringAfterLast('/')
            },
            mimeType = strOrEmpty(MediaStore.Video.Media.MIME_TYPE),
        )
    }

    /* ============================ 音乐 ============================ */

    private val audioProjection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.DATE_ADDED,
        MediaStore.Audio.Media.MIME_TYPE,
    )

    suspend fun loadMusic(): List<MusicTrack> = withContext(Dispatchers.IO) {
        val out = ArrayList<MusicTrack>()
        val sel = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 10000"
        runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                audioProjection, sel, null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { c -> while (c.moveToNext()) out += c.toMusicTrack() }
        }
        if (out.isEmpty()) out += scanAudioFiles()
        out.distinctBy { it.path }
    }

    private fun Cursor.toMusicTrack(): MusicTrack {
        val id = longOrZero(MediaStore.Audio.Media._ID)
        val albumId = longOrZero(MediaStore.Audio.Media.ALBUM_ID)
        val path = strOrEmpty(MediaStore.Audio.Media.DATA)
        val name = strOrEmpty(MediaStore.Audio.Media.DISPLAY_NAME).ifBlank { File(path).name }
        val rawArtist = strOrEmpty(MediaStore.Audio.Media.ARTIST)
        val artist = if (rawArtist.isBlank() || rawArtist == "<unknown>") "未知歌手" else rawArtist
        return MusicTrack(
            id = id,
            title = strOrEmpty(MediaStore.Audio.Media.TITLE).ifBlank { name.substringBeforeLast('.') },
            artist = artist,
            album = strOrEmpty(MediaStore.Audio.Media.ALBUM).ifBlank { "未知专辑" },
            albumId = albumId,
            durationMs = longOrZero(MediaStore.Audio.Media.DURATION),
            path = path,
            uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString(),
            sizeBytes = longOrZero(MediaStore.Audio.Media.SIZE),
            dateAdded = longOrZero(MediaStore.Audio.Media.DATE_ADDED) * 1000,
            embeddedCoverUri = albumCoverUri(albumId),
            mimeType = strOrEmpty(MediaStore.Audio.Media.MIME_TYPE),
        )
    }

    /** 系统媒体库中的专辑封面 Uri */
    fun albumCoverUri(albumId: Long): String? {
        if (albumId <= 0) return null
        return "content://media/external/audio/albumart/$albumId"
    }

    /* ============================ 文件夹聚合 ============================ */

    fun videoFolders(videos: List<VideoItem>): List<MediaFolder> =
        videos.groupBy { it.bucketId }
            .map { (bucket, list) ->
                MediaFolder(
                    bucketId = bucket,
                    name = list.first().folderName.ifBlank { "未知目录" },
                    path = list.first().path.substringBeforeLast('/'),
                    count = list.size,
                    coverUri = list.first().uri,
                )
            }
            .sortedByDescending { it.count }

    fun musicFolders(tracks: List<MusicTrack>): List<MediaFolder> =
        tracks.groupBy { it.path.substringBeforeLast('/') }
            .map { (dir, list) ->
                MediaFolder(
                    bucketId = dir.hashCode().toLong(),
                    name = dir.substringAfterLast('/').ifBlank { "根目录" },
                    path = dir,
                    count = list.size,
                    coverUri = list.firstOrNull()?.embeddedCoverUri,
                )
            }
            .sortedByDescending { it.count }

    /* ============================ 文件系统兜底扫描 ============================ */

    private val scanRoots: List<File> by lazy {
        listOf("/storage/emulated/0", "/sdcard", "/storage/self/primary")
            .map(::File).filter { it.exists() }.distinct()
    }

    private fun walkMedia(exts: Set<String>, limit: Int): List<File> {
        val out = ArrayList<File>()
        scanRoots.forEach { root ->
            runCatching {
                root.walkTopDown()
                    .maxDepth(8)
                    .onEnter { dir -> !dir.name.startsWith(".") && dir.name != "Android" }
                    .filter { it.isFile && it.extension.lowercase() in exts }
                    .take(limit)
                    .forEach { out += it }
            }
        }
        return out
    }

    private fun scanVideoFiles(): List<VideoItem> = walkMedia(VIDEO_EXTENSIONS, 3000).map { f ->
        VideoItem(
            id = f.absolutePath.hashCode().toLong(),
            title = f.nameWithoutExtension,
            path = f.absolutePath,
            uri = Uri.fromFile(f).toString(),
            durationMs = 0L,
            sizeBytes = f.length(),
            width = 0, height = 0,
            dateAdded = f.lastModified(),
            dateModified = f.lastModified(),
            bucketId = (f.parentFile?.absolutePath?.hashCode() ?: 0).toLong(),
            folderName = f.parentFile?.name ?: "",
        )
    }

    private fun scanAudioFiles(): List<MusicTrack> = walkMedia(AUDIO_EXTENSIONS, 5000).map { f ->
        MusicTrack(
            id = f.absolutePath.hashCode().toLong(),
            title = f.nameWithoutExtension,
            artist = "未知歌手",
            album = "未知专辑",
            albumId = 0L,
            durationMs = 0L,
            path = f.absolutePath,
            uri = Uri.fromFile(f).toString(),
            sizeBytes = f.length(),
            dateAdded = f.lastModified(),
        )
    }
}
