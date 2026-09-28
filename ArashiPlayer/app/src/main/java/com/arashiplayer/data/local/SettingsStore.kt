package com.arashiplayer.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arashiplayer.data.model.DecoderMode
import com.arashiplayer.data.model.SubtitleStyle
import com.arashiplayer.data.model.VaultConfig
import com.arashiplayer.data.model.VaultLockMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "arashi_settings")

/**
 * 全局设置。所有键集中在 [Keys]，避免散落的字符串字面量。
 */
class SettingsStore(private val context: Context) {

    object Keys {
        // 外观
        val themeMode = stringPreferencesKey("theme_mode")           // system | light | dark
        val accentIndex = intPreferencesKey("accent_index")

        // 播放
        val autoMatchOnline = booleanPreferencesKey("auto_match_online")
        val decoder = stringPreferencesKey("decoder")               // HW | SW
        val gestureControl = booleanPreferencesKey("gesture_control")
        val resumePlayback = booleanPreferencesKey("resume_playback")
        val autoNext = booleanPreferencesKey("auto_next")
        val backgroundAudio = booleanPreferencesKey("background_audio")
        val defaultSpeed = floatPreferencesKey("default_speed")
        val aspectRatio = stringPreferencesKey("aspect_ratio")

        // 字幕默认样式
        val subColor = intPreferencesKey("sub_color")
        val subOpacity = floatPreferencesKey("sub_opacity")
        val subEdgeType = intPreferencesKey("sub_edge_type")
        val subEdgeColor = intPreferencesKey("sub_edge_color")
        val subSize = floatPreferencesKey("sub_size")
        val subBold = booleanPreferencesKey("sub_bold")
        val subBottom = floatPreferencesKey("sub_bottom")
        val subBgOpacity = floatPreferencesKey("sub_bg_opacity")
        val subOffsetMs = longPreferencesKey("sub_offset_ms")
        /** 记住最近一次使用的字幕目录，方便下次一键挂载同名字幕 */
        val lastSubtitleDir = stringPreferencesKey("last_subtitle_dir")

        // 加密空间
        val vaultEnabled = booleanPreferencesKey("vault_enabled")
        val vaultMode = stringPreferencesKey("vault_mode")           // PIN | GESTURE
        val vaultHash = stringPreferencesKey("vault_hash")
        val vaultSalt = stringPreferencesKey("vault_salt")
        val vaultHint = stringPreferencesKey("vault_hint")
        val vaultHideThumb = booleanPreferencesKey("vault_hide_thumb")
        /** 离开前台后自动回锁的秒数，0 表示立即 */
        val vaultAutoLockSec = intPreferencesKey("vault_auto_lock_sec")

        // 其他
        val firstLaunch = booleanPreferencesKey("first_launch")
        val shownReward = booleanPreferencesKey("shown_reward")
    }

    /* ---------------- 外观 ---------------- */

    val themeMode: Flow<String> = context.dataStore.data.map { it[Keys.themeMode] ?: "system" }
    val accentIndex: Flow<Int> = context.dataStore.data.map { it[Keys.accentIndex] ?: 0 }

    suspend fun setThemeMode(mode: String) = context.dataStore.edit { it[Keys.themeMode] = mode }
    suspend fun setAccentIndex(i: Int) = context.dataStore.edit { it[Keys.accentIndex] = i }

    /* ---------------- 播放 ---------------- */

    val autoMatchOnline: Flow<Boolean> = context.dataStore.data.map { it[Keys.autoMatchOnline] ?: true }
    val decoder: Flow<DecoderMode> = context.dataStore.data.map {
        runCatching { DecoderMode.valueOf(it[Keys.decoder] ?: "HW") }.getOrDefault(DecoderMode.HW)
    }
    val gestureControl: Flow<Boolean> = context.dataStore.data.map { it[Keys.gestureControl] ?: true }
    val resumePlayback: Flow<Boolean> = context.dataStore.data.map { it[Keys.resumePlayback] ?: true }
    val aspectRatio: Flow<String> = context.dataStore.data.map { it[Keys.aspectRatio] ?: "FIT" }

    suspend fun setAutoMatchOnline(v: Boolean) = context.dataStore.edit { it[Keys.autoMatchOnline] = v }
    suspend fun setDecoder(v: DecoderMode) = context.dataStore.edit { it[Keys.decoder] = v.name }
    suspend fun setGestureControl(v: Boolean) = context.dataStore.edit { it[Keys.gestureControl] = v }
    suspend fun setResumePlayback(v: Boolean) = context.dataStore.edit { it[Keys.resumePlayback] = v }
    suspend fun setAspectRatio(v: String) = context.dataStore.edit { it[Keys.aspectRatio] = v }

    /* ---------------- 字幕 ---------------- */

    val subtitleStyle: Flow<SubtitleStyle> = context.dataStore.data.map { p ->
        SubtitleStyle(
            textColor = p[Keys.subColor] ?: SubtitleStyle.Default.textColor,
            textOpacity = p[Keys.subOpacity] ?: SubtitleStyle.Default.textOpacity,
            edgeType = p[Keys.subEdgeType] ?: SubtitleStyle.Default.edgeType,
            edgeColor = p[Keys.subEdgeColor] ?: SubtitleStyle.Default.edgeColor,
            textSizeSp = p[Keys.subSize] ?: SubtitleStyle.Default.textSizeSp,
            bold = p[Keys.subBold] ?: SubtitleStyle.Default.bold,
            bottomPaddingFraction = p[Keys.subBottom] ?: SubtitleStyle.Default.bottomPaddingFraction,
            backgroundOpacity = p[Keys.subBgOpacity] ?: SubtitleStyle.Default.backgroundOpacity,
            timeOffsetMs = p[Keys.subOffsetMs] ?: 0L,
        )
    }

    suspend fun saveSubtitleStyle(s: SubtitleStyle) = context.dataStore.edit { p ->
        p[Keys.subColor] = s.textColor
        p[Keys.subOpacity] = s.textOpacity
        p[Keys.subEdgeType] = s.edgeType
        p[Keys.subEdgeColor] = s.edgeColor
        p[Keys.subSize] = s.textSizeSp
        p[Keys.subBold] = s.bold
        p[Keys.subBottom] = s.bottomPaddingFraction
        p[Keys.subBgOpacity] = s.backgroundOpacity
        p[Keys.subOffsetMs] = s.timeOffsetMs
    }

    suspend fun setLastSubtitleDir(dir: String) = context.dataStore.edit { it[Keys.lastSubtitleDir] = dir }
    suspend fun lastSubtitleDir(): String? = context.dataStore.data.first()[Keys.lastSubtitleDir]

    /* ---------------- 加密空间 ---------------- */

    val vaultConfig: Flow<VaultConfig> = context.dataStore.data.map { p ->
        VaultConfig(
            enabled = p[Keys.vaultEnabled] ?: false,
            mode = runCatching { VaultLockMode.valueOf(p[Keys.vaultMode] ?: "PIN") }.getOrDefault(VaultLockMode.PIN),
            credentialHash = p[Keys.vaultHash] ?: "",
            credentialSalt = p[Keys.vaultSalt] ?: "",
            hint = p[Keys.vaultHint] ?: "",
            hideThumbnail = p[Keys.vaultHideThumb] ?: true,
        )
    }

    suspend fun saveVaultConfig(c: VaultConfig) = context.dataStore.edit { p ->
        p[Keys.vaultEnabled] = c.enabled
        p[Keys.vaultMode] = c.mode.name
        p[Keys.vaultHash] = c.credentialHash
        p[Keys.vaultSalt] = c.credentialSalt
        p[Keys.vaultHint] = c.hint
        p[Keys.vaultHideThumb] = c.hideThumbnail
    }

    suspend fun vaultAutoLockSec(): Int = context.dataStore.data.first()[Keys.vaultAutoLockSec] ?: 15
    suspend fun setVaultAutoLockSec(v: Int) = context.dataStore.edit { it[Keys.vaultAutoLockSec] = v }

    suspend fun setFirstLaunchDone() = context.dataStore.edit { it[Keys.firstLaunch] = false }
}
