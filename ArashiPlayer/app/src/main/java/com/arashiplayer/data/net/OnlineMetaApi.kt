package com.arashiplayer.data.net

import com.arashiplayer.data.model.LyricLine
import com.arashiplayer.data.model.MusicTrack
import com.arashiplayer.data.model.OnlineMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 在线封面 / 歌词匹配（实现思路参考「音乐标签」这类工具）：
 *
 * 1. 用「标题 + 歌手」检索候选曲目；
 * 2. 按标题相似度、时长差、歌手匹配度打分，选最像的一首；
 * 3. 拉取该曲目的专辑封面与 LRC 歌词（含翻译）。
 *
 * 数据源为公开的网易云音乐 Web 接口，仅用于个人本地曲库补全元数据。
 * 所有网络失败都会被吞掉并返回 null —— 离线时播放器仍然完全可用。
 */
class OnlineMetaApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Mobile Safari/537.36",
        "Referer" to "https://music.163.com/",
        "Cookie" to "appver=2.0.2; os=android",
        "Accept" to "application/json, text/plain, */*",
    )

    /* ============================ 对外入口 ============================ */

    /**
     * 为单曲匹配封面与歌词。
     * @param prefer 优先字段：true 时用「歌手 - 标题」检索（对中文曲库更准）
     */
    suspend fun match(track: MusicTrack, prefer: Boolean = true): OnlineMeta? = withContext(Dispatchers.IO) {
        val kw = buildKeyword(track, prefer)
        runCatching {
            val candidates = search(kw).ifEmpty { search(track.title).ifEmpty { search(track.artist + " " + track.title) } }
            if (candidates.isEmpty()) return@withContext null

            val best = candidates.maxByOrNull { score(it, track) } ?: return@withContext null
            val lyric = fetchLyric(best.id)
            OnlineMeta(
                query = kw,
                matchedTitle = best.name,
                matchedArtist = best.artist,
                album = best.album,
                coverUrl = best.coverUrl ?: fetchCoverFromDetail(best.id),
                lyric = lyric?.first,
                translatedLyric = lyric?.second,
                source = "netease",
            )
        }.getOrNull()
    }

    /** 批量匹配，顺序执行以免触发风控，中间稍作停顿 */
    suspend fun matchAll(
        tracks: List<MusicTrack>,
        onEach: suspend (MusicTrack, OnlineMeta?) -> Unit = { _, _ -> },
    ) = withContext(Dispatchers.IO) {
        tracks.forEach { t ->
            val m = runCatching { match(t) }.getOrNull()
            onEach(t, m)
            kotlinx.coroutines.delay(260)
        }
    }

    private fun buildKeyword(t: MusicTrack, prefer: Boolean): String {
        val a = t.artist.takeIf { it.isNotBlank() && it != "未知歌手" }
        return if (prefer) {
            listOfNotNull(a, t.title).joinToString(" ")
        } else {
            listOfNotNull(t.title, a).joinToString(" ")
        }
    }

    /* ============================ 检索 ============================ */

    private data class Candidate(
        val id: Long,
        val name: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
        val coverUrl: String?,
    )

    private fun search(keyword: String): List<Candidate> {
        if (keyword.isBlank()) return emptyList()
        val url = "https://music.163.com/api/search/get/web" +
                "?csrf_token=&s=${enc(keyword)}&type=1&offset=0&total=true&limit=10"
        val body = get(url) ?: return emptyList()
        return runCatching {
            val root = json.parseToJsonElement(body).jsonObject
            val songs = root["result"]?.jsonObject?.get("songs")?.jsonArray ?: JsonArray(emptyList())
            songs.mapNotNull { el ->
                val o = el.jsonObject
                val artists = o["artists"]?.jsonArray
                    ?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
                    ?.joinToString("/") ?: ""
                val albumObj = o["album"]?.jsonObject
                Candidate(
                    id = o["id"]?.jsonPrimitive?.content?.toLongOrNull() ?: return@mapNotNull null,
                    name = o["name"]?.jsonPrimitive?.content ?: "",
                    artist = artists,
                    album = albumObj?.get("name")?.jsonPrimitive?.content ?: "",
                    durationMs = o["duration"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                    coverUrl = albumObj?.get("picUrl")?.jsonPrimitive?.content
                        ?: albumObj?.get("artist")?.jsonObject?.get("picUrl")?.jsonPrimitive?.content,
                )
            }
        }.getOrDefault(emptyList())
    }

    /** 打分：标题相似度权重最高，其次时长差，最后歌手是否命中 */
    private fun score(c: Candidate, t: MusicTrack): Double {
        var s = similarity(normalize(c.name), normalize(t.title)) * 100.0
        if (t.artist.isNotBlank() && t.artist != "未知歌手") {
            val a1 = normalize(c.artist)
            val a2 = normalize(t.artist)
            if (a1.contains(a2) || a2.contains(a1)) s += 30.0
        }
        if (t.durationMs > 0 && c.durationMs > 0) {
            val diff = kotlin.math.abs(c.durationMs - t.durationMs)
            s += (1.0 - (diff / 20_000.0).coerceIn(0.0, 1.0)) * 40.0
        }
        if (normalize(c.album) == normalize(t.album)) s += 10.0
        return s
    }

    private fun normalize(s: String): String = s.lowercase()
        .replace(Regex("[\\(（\\[【].*?[\\)）\\]】]"), "")
        .replace(Regex("[\\s·\\-—_.,'\"!?]"), "")
        .trim()

    /** 归一化编辑距离 → 相似度 0~1 */
    private fun similarity(a: String, b: String): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val tmp = dp[j]
                dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + if (a[i - 1] == b[j - 1]) 0 else 1)
                prev = tmp
            }
        }
        return 1.0 - dp[b.length].toDouble() / maxOf(a.length, b.length)
    }

    /* ============================ 封面 / 歌词 ============================ */

    private fun fetchCoverFromDetail(songId: Long): String? = runCatching {
        val body = get("https://music.163.com/api/song/detail?ids=[$songId]") ?: return null
        json.parseToJsonElement(body).jsonObject["songs"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("album")?.jsonObject
            ?.get("picUrl")?.jsonPrimitive?.content
    }.getOrNull()

    private fun fetchLyric(songId: Long): Pair<String, String?>? = runCatching {
        val body = get("https://music.163.com/api/song/lyric?id=$songId&lv=1&kv=1&tv=-1") ?: return null
        val root = json.parseToJsonElement(body).jsonObject
        val lrc = root["lrc"]?.jsonObject?.get("lyric")?.jsonPrimitive?.content
        val tlrc = root["tlyric"]?.jsonObject?.get("lyric")?.jsonPrimitive?.content
        if (lrc.isNullOrBlank()) null else lrc to tlrc
    }.getOrNull()

    /* ============================ HTTP ============================ */

    private fun get(url: String): String? = runCatching {
        val builder = Request.Builder().url(url).get()
        headers.forEach { (k, v) -> builder.header(k, v) }
        client.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) return null
            resp.body?.string()?.takeIf { it.isNotBlank() }
        }
    }.getOrNull()

    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")

    /* ============================ LRC 解析 ============================ */

    companion object {

        private val TIME_TAG = Regex("\\[(\\d{1,2}):(\\d{1,2})(?:[.:](\\d{1,3}))?]")

        /**
         * 解析 LRC 文本。
         * 支持一行多时间戳、翻译行按时间轴对齐。
         */
        fun parseLyric(lrc: String?, translation: String? = null): List<LyricLine> {
            if (lrc.isNullOrBlank()) return emptyList()

            val main = parseOne(lrc)
            val trans = parseOne(translation)
            if (trans.isEmpty()) return main

            val transMap = trans.associate { it.timeMs to it.text }
            return main.map { line ->
                val t = transMap[line.timeMs]
                    ?: trans.minByOrNull { kotlin.math.abs(it.timeMs - line.timeMs) }
                        ?.takeIf { kotlin.math.abs(it.timeMs - line.timeMs) < 400 }?.text
                line.copy(translation = t)
            }
        }

        private fun parseOne(text: String?): List<LyricLine> {
            if (text.isNullOrBlank()) return emptyList()
            val out = ArrayList<LyricLine>()
            text.lineSequence().forEach { raw ->
                val line = raw.trim()
                if (line.isEmpty()) return@forEach
                val tags = TIME_TAG.findAll(line).toList()
                if (tags.isEmpty()) return@forEach
                val content = line.substring(tags.last().range.last + 1).trim()
                if (content.isEmpty() || content == "//") return@forEach
                tags.forEach { m ->
                    val min = m.groupValues[1].toLongOrNull() ?: 0
                    val sec = m.groupValues[2].toLongOrNull() ?: 0
                    val fracRaw = m.groupValues[3]
                    val frac = when (fracRaw.length) {
                        0 -> 0L
                        1 -> fracRaw.toLong() * 100
                        2 -> fracRaw.toLong() * 10
                        else -> fracRaw.take(3).toLong()
                    }
                    out += LyricLine(timeMs = (min * 60 + sec) * 1000 + frac, text = content)
                }
            }
            return out.sortedBy { it.timeMs }
        }
    }
}
