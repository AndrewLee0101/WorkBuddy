package com.arashiplayer.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/* ============================ 时间 ============================ */

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "00:00"
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}

fun formatClockShort(ms: Long): String {
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return String.format(Locale.US, "%d:%02d", m, s)
}

fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(epochMillis))

fun formatDateTime(epochMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(epochMillis))

/* ============================ 体积 ============================ */

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var v = bytes.toDouble()
    var i = 0
    while (v >= 1024 && i < units.size - 1) {
        v /= 1024.0
        i++
    }
    return if (i == 0) "${bytes} B" else String.format(Locale.US, "%.2f %s", v, units[i])
}

/* ============================ 文件名 ============================ */

fun fileNameOf(path: String): String = path.substringAfterLast('/').substringAfterLast('\\')

fun extensionOf(path: String): String = path.substringAfterLast('.', "").lowercase(Locale.ROOT)

fun titleWithoutExt(path: String): String = fileNameOf(path).substringBeforeLast('.')

/** 常见格式 → 展示用容器名 */
fun containerLabel(path: String): String = extensionOf(path).uppercase(Locale.ROOT)

val VIDEO_EXTENSIONS = setOf(
    "mp4", "mkv", "webm", "avi", "mov", "flv", "f4v", "ts", "m2ts", "mts", "wmv", "mpg", "mpeg",
    "3gp", "rmvb", "rm", "vob", "ogv", "m4v", "divx", "asf", "m3u8", "iso"
)

val AUDIO_EXTENSIONS = setOf(
    "mp3", "flac", "wav", "aac", "m4a", "ogg", "oga", "opus", "ape", "wma", "amr", "ac3", "eac3",
    "dts", "aiff", "aif", "mka", "mp2", "m4b", "mid", "midi", "wv", "tta"
)

val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "avif", "tiff", "dng", "raw")

val SUBTITLE_EXTENSIONS = setOf("srt", "ass", "ssa", "vtt", "sub", "smi", "ttml", "dfxp", "lrc", "txt")

/** 加密空间统一使用的混淆后缀 —— 系统扫描器不会识别为媒体 */
const val VAULT_EXTENSION = "arv"

/* ============================ 路径 ============================ */

/** 是否为 Android 11+ 的分区存储 */
val isScopedStorage: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

fun externalRoot(): String = Environment.getExternalStorageDirectory().absolutePath

/**
 * 加密空间「原地混淆」策略：
 * 文件留在原目录，只把 `视频.mp4` → `.arashi/<random>.arv`，
 * 并在所在目录写下 `.nomedia`，让相册 / 其他 App 的扫描器跳过。
 * 全程 rename，不复制字节，因此不额外占用空间。
 */
const val VAULT_DIR_NAME = ".arashi_vault"
const val NO_MEDIA = ".nomedia"

/* ============================ Content Uri ============================ */

fun Context.displayName(uri: Uri): String? = runCatching {
    contentResolver.query(uri, null, null, null, null)?.use { c ->
        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
    }
}.getOrNull()

fun Context.fileSize(uri: Uri): Long = runCatching {
    contentResolver.query(uri, null, null, null, null)?.use { c ->
        val idx = c.getColumnIndex(OpenableColumns.SIZE)
        if (idx >= 0 && c.moveToFirst()) c.getLong(idx) else 0L
    } ?: 0L
}.getOrDefault(0L)
