package com.arashiplayer.data.model

import androidx.compose.ui.graphics.Color

/* ============================ 媒体模型 ============================ */

/** 本地音乐曲目 */
data class MusicTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val path: String,
    val uri: String,
    val sizeBytes: Long,
    val dateAdded: Long,
    /** 系统媒体库中的内嵌封面 Uri（可能为空） */
    val embeddedCoverUri: String? = null,
    val mimeType: String? = null,
    val sampleRate: Int = 0,
    val bitRate: Int = 0,
) {
    val key: String get() = "music:$id"
}

/** 本地视频 */
data class VideoItem(
    val id: Long,
    val title: String,
    val path: String,
    val uri: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val dateAdded: Long,
    val dateModified: Long,
    val bucketId: Long,
    val folderName: String,
    val mimeType: String? = null,
) {
    val key: String get() = "video:$id"
    val resolution: String get() = when {
        width <= 0 || height <= 0 -> ""
        height >= 2160 -> "4K"
        height >= 1440 -> "2K"
        height >= 1080 -> "1080P"
        height >= 720 -> "720P"
        height >= 480 -> "480P"
        else -> "${height}P"
    }
}

/** 文件夹聚合（视频/音乐通用） */
data class MediaFolder(
    val bucketId: Long,
    val name: String,
    val path: String,
    val count: Int,
    val coverUri: String?,
)

/** 通用可播放条目（音乐与视频统一进播放器） */
sealed interface Playable {
    val uri: String
    val title: String
    val subtitle: String

    data class Music(val track: MusicTrack) : Playable {
        override val uri: String get() = track.uri
        override val title: String get() = track.title
        override val subtitle: String get() = track.artist
    }

    data class Video(val item: VideoItem) : Playable {
        override val uri: String get() = item.uri
        override val title: String get() = item.title
        override val subtitle: String get() = item.folderName
    }
}

/* ============================ 在线元数据 ============================ */

/** 在线匹配到的封面 / 歌词（参考「音乐标签」的匹配逻辑） */
data class OnlineMeta(
    val query: String,
    val matchedTitle: String? = null,
    val matchedArtist: String? = null,
    val album: String? = null,
    val coverUrl: String? = null,
    val lyric: String? = null,
    val translatedLyric: String? = null,
    val source: String = "netease",
    val updatedAt: Long = System.currentTimeMillis(),
)

/** 解析后的一行歌词 */
data class LyricLine(
    val timeMs: Long,
    val text: String,
    val translation: String? = null,
)

/* ============================ 加密空间 ============================ */

enum class VaultKind { IMAGE, VIDEO, AUDIO, DOC, OTHER }

/** 加密空间条目：只记录「原路径 → 混淆路径」的映射，不复制文件、不额外占空间 */
data class VaultItem(
    val id: Long,
    /** 混淆后的文件名（随机串 + .arv） */
    val vaultName: String,
    val currentPath: String,
    /** 原始文件名，用于还原 */
    val originalName: String,
    val originalPath: String,
    val originalExt: String,
    val kind: VaultKind,
    val sizeBytes: Long,
    val addedAt: Long,
    val mimeType: String? = null,
) {
    val displayTitle: String get() = originalName.substringBeforeLast('.', originalName)
}

/** 加密空间解锁方式 */
enum class VaultLockMode { PIN, GESTURE }

/** 加密空间解锁配置 */
data class VaultConfig(
    val enabled: Boolean = false,
    val mode: VaultLockMode = VaultLockMode.PIN,
    /** 加密后的凭据摘要；PIN 为 4~6 位数字，手势为 9 点路径索引串（如 "0-1-2-5-8"） */
    val credentialHash: String = "",
    val credentialSalt: String = "",
    val hint: String = "",
    val hideThumbnail: Boolean = true,
)

/* ============================ 字幕样式 ============================ */

/**
 * 外挂字幕样式（颜色 / 透明度 / 位置 / 大小）
 * 直接映射到 Media3 的 CaptionStyleCompat + SubtitleView 的分数文字大小与底部内边距。
 */
data class SubtitleStyle(
    /** 文字颜色 ARGB */
    val textColor: Int = 0xFFFFFFFF.toInt(),
    /** 文字透明度 0f~1f */
    val textOpacity: Float = 1f,
    /** 描边类型：0 无、1 外描边、2 投影、3 背景框 */
    val edgeType: Int = 1,
    /** 描边颜色 */
    val edgeColor: Int = 0xFF000000.toInt(),
    /** 字号（sp），映射为 SubtitleView 的 fractional 大小 */
    val textSizeSp: Float = 20f,
    /** 是否加粗 */
    val bold: Boolean = true,
    /** 底部内边距比例 0f（贴底）~ 0.5f（偏上） */
    val bottomPaddingFraction: Float = 0.06f,
    /** 底部背景条透明度（0 表示无） */
    val backgroundOpacity: Float = 0f,
    /** 字幕时间轴整体偏移（毫秒，正数延后） */
    val timeOffsetMs: Long = 0L,
) {
    /** Media3 CaptionStyleCompat.EDGE_TYPE_* */
    val edgeTypeCompat: Int get() = edgeType

    fun withAlpha(alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        return (textColor and 0x00FFFFFF) or (a shl 24)
    }

    companion object {
        val Default = SubtitleStyle()
        /** 预设主题，供字幕面板一键套用 */
        val Presets: List<Pair<String, SubtitleStyle>> = listOf(
            "标准" to SubtitleStyle(),
            "影院描边" to SubtitleStyle(textSizeSp = 24f, edgeType = 1, edgeColor = 0xFF000000.toInt(), bold = true, bottomPaddingFraction = 0.08f),
            "柔和半透" to SubtitleStyle(textOpacity = 0.72f, edgeType = 2, edgeColor = 0xFF000000.toInt(), backgroundOpacity = 0f),
            "黄色高亮" to SubtitleStyle(textColor = 0xFFFFE066.toInt(), textSizeSp = 22f, edgeType = 1),
            "背景条" to SubtitleStyle(backgroundOpacity = 0.55f, edgeType = 0, bottomPaddingFraction = 0.05f),
        )
    }
}

/* ============================ 播放设置 ============================ */

enum class DecoderMode { HW, SW }
enum class AspectRatioMode(val label: String) {
    FIT("适应屏幕"), FILL("拉伸铺满"), CROP("裁剪铺满"), RATIO_16_9("16:9"), RATIO_4_3("4:3"), ORIGINAL("原始比例")
}

data class VideoBookmark(
    val mediaKey: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
)

/* ============================ 主题色预设 ============================ */

data class ThemeAccent(val name: String, val value: Color)

val AccentPresets = listOf(
    ThemeAccent("岚·雾蓝", Color(0xFF4F6BFF)),
    ThemeAccent("紫烟", Color(0xFF7C5CFF)),
    ThemeAccent("晨青", Color(0xFF35D6E8)),
    ThemeAccent("晚霞", Color(0xFFFF6B8A)),
    ThemeAccent("苔绿", Color(0xFF3FBF8F)),
    ThemeAccent("琥珀", Color(0xFFFFA53D)),
)
