package com.arashiplayer.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.arashiplayer.ArashiApp
import com.arashiplayer.BuildConfig
import com.arashiplayer.data.model.AccentPresets
import com.arashiplayer.data.model.DecoderMode
import com.arashiplayer.data.model.SubtitleStyle
import com.arashiplayer.data.model.VaultConfig
import com.arashiplayer.data.model.VaultLockMode
import com.arashiplayer.ui.components.DividerLine
import com.arashiplayer.ui.components.SectionHeader
import com.arashiplayer.ui.components.SettingsGroup
import com.arashiplayer.ui.components.SettingsRow
import com.arashiplayer.ui.components.XiaohongshuLogo
import com.arashiplayer.ui.navigation.Routes
import com.arashiplayer.ui.screens.vault.SegmentedRow
import com.arashiplayer.ui.theme.ArashiShape
import com.arashiplayer.ui.theme.ArashiTheme
import kotlinx.coroutines.launch

private val AUTO_LOCK_SECONDS = listOf(0, 15, 60, 300)
private val AUTO_LOCK_LABELS = listOf("立即", "15 秒", "1 分钟", "5 分钟")

/** 设置页：Apple 风分组列表 */
@Composable
fun SettingsScreen(navController: NavHostController) {
    val c = ArashiTheme.colors
    val context = LocalContext.current
    val app = remember(context) { ArashiApp.of(context) }
    val scope = rememberCoroutineScope()

    val themeMode by app.settings.themeMode.collectAsState(initial = "system")
    val accentIndex by app.settings.accentIndex.collectAsState(initial = 0)
    val autoMatch by app.settings.autoMatchOnline.collectAsState(initial = true)
    val decoder by app.settings.decoder.collectAsState(initial = DecoderMode.HW)
    val gesture by app.settings.gestureControl.collectAsState(initial = true)
    val resume by app.settings.resumePlayback.collectAsState(initial = true)
    val subStyle by app.settings.subtitleStyle.collectAsState(initial = SubtitleStyle.Default)
    val vaultConfig by app.settings.vaultConfig.collectAsState(initial = VaultConfig())
    val vaultCount by app.vaultRepo.count.collectAsState(initial = 0)

    var autoLockSec by remember { mutableStateOf(15) }
    var subtitleDialog by remember { mutableStateOf(false) }
    var licenseDialog by remember { mutableStateOf(false) }
    var privacyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        autoLockSec = runCatching { app.settings.vaultAutoLockSec() }.getOrDefault(15)
    }

    val themeIndex = when (themeMode) {
        "light" -> 1
        "dark" -> 2
        else -> 0
    }
    val autoLockIndex = AUTO_LOCK_SECONDS.indexOf(autoLockSec).takeIf { it >= 0 } ?: 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        /* ---------- 大标题 ---------- */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 22.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(ArashiShape.pill)
                    .clickable { navController.popBackStack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = c.textSecondary,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                "设置",
                color = c.text,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.7).sp,
            )
        }

        /* ============================ 外观 ============================ */
        SectionHeader("外观")
        SettingsGroup {
            SettingBlock("主题模式") {
                SegmentedRow(
                    options = listOf("跟随系统", "浅色", "深色"),
                    selectedIndex = themeIndex,
                    modifier = Modifier.fillMaxWidth(),
                ) { index ->
                    scope.launch {
                        app.settings.setThemeMode(
                            when (index) {
                                1 -> "light"
                                2 -> "dark"
                                else -> "system"
                            }
                        )
                    }
                }
            }
            DividerLine()
            SettingBlock("主题色", AccentPresets.getOrNull(accentIndex)?.name) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    AccentPresets.forEachIndexed { index, accent ->
                        val active = index == accentIndex
                        Box(
                            modifier = Modifier
                                .size(if (active) 30.dp else 26.dp)
                                .clip(CircleShape)
                                .background(accent.value)
                                .border(
                                    width = if (active) 2.5.dp else 0.dp,
                                    color = if (active) c.text else Color.Transparent,
                                    shape = CircleShape,
                                )
                                .clickable {
                                    scope.launch { app.settings.setAccentIndex(index) }
                                },
                        )
                    }
                }
            }
        }

        /* ============================ 播放 ============================ */
        SectionHeader("播放")
        SettingsGroup {
            SettingsRow(
                title = "自动匹配在线封面与歌词",
                subtitle = "按「标题 + 歌手」联网匹配，结果缓存在本机",
                trailing = { Switch(checked = autoMatch, onCheckedChange = null) },
                onClick = {
                    scope.launch { app.settings.setAutoMatchOnline(!autoMatch) }
                },
            )
            DividerLine()
            SettingBlock("默认解码方式") {
                SegmentedRow(
                    options = listOf("硬解", "软解"),
                    selectedIndex = if (decoder == DecoderMode.SW) 1 else 0,
                    modifier = Modifier.fillMaxWidth(),
                ) { index ->
                    scope.launch {
                        app.settings.setDecoder(if (index == 1) DecoderMode.SW else DecoderMode.HW)
                    }
                }
            }
            DividerLine()
            SettingsRow(
                title = "手势控制",
                subtitle = "亮度 / 音量 / 快进，在播放页左右滑动",
                trailing = { Switch(checked = gesture, onCheckedChange = null) },
                onClick = { scope.launch { app.settings.setGestureControl(!gesture) } },
            )
            DividerLine()
            SettingsRow(
                title = "记忆播放位置",
                subtitle = "下次打开同一影片时从上次位置继续",
                trailing = { Switch(checked = resume, onCheckedChange = null) },
                onClick = { scope.launch { app.settings.setResumePlayback(!resume) } },
            )
        }

        /* ============================ 字幕 ============================ */
        SectionHeader("字幕")
        SettingsGroup {
            SettingsRow(
                title = "默认字幕样式",
                subtitle = "字号 ${subStyle.textSizeSp.toInt()}sp · ${subtitleColorLabel(subStyle)} · " +
                    (if (subStyle.bold) "加粗" else "常规"),
                showArrow = true,
                onClick = { subtitleDialog = true },
            )
        }

        /* ============================ 安全 / 加密空间 ============================ */
        SectionHeader("安全 / 加密空间")
        SettingsGroup {
            SettingsRow(
                title = "加密空间",
                subtitle = if (vaultCount > 0) "已加密 $vaultCount 个文件" else "还没有加密任何文件",
                showArrow = true,
                onClick = { navController.navigate(Routes.VAULT) },
            )
            DividerLine()
            SettingsRow(
                title = "修改密码",
                subtitle = if (vaultConfig.enabled) "重新设置解锁密码" else "尚未设置密码",
                showArrow = true,
                onClick = { navController.navigate(Routes.VAULT) },
            )
            DividerLine()
            SettingsRow(
                title = "解锁方式",
                subtitle = if (vaultConfig.mode == VaultLockMode.PIN) "数字密码" else "手势密码",
                showArrow = true,
                onClick = { navController.navigate(Routes.VAULT) },
            )
            DividerLine()
            SettingsRow(
                title = "隐藏加密文件缩略图",
                subtitle = "在系统相册与其他播放器中不显示预览图",
                trailing = {
                    Switch(
                        checked = vaultConfig.hideThumbnail,
                        onCheckedChange = null,
                    )
                },
                onClick = {
                    scope.launch {
                        app.settings.saveVaultConfig(
                            vaultConfig.copy(hideThumbnail = !vaultConfig.hideThumbnail)
                        )
                    }
                },
            )
            DividerLine()
            SettingBlock("离开后自动回锁") {
                SegmentedRow(
                    options = AUTO_LOCK_LABELS,
                    selectedIndex = autoLockIndex,
                    modifier = Modifier.fillMaxWidth(),
                ) { index ->
                    val seconds = AUTO_LOCK_SECONDS[index]
                    autoLockSec = seconds
                    scope.launch { app.settings.setVaultAutoLockSec(seconds) }
                }
            }
        }

        /* ============================ 关于 ============================ */
        SectionHeader("关于")
        SettingsGroup {
            SettingsRow(
                title = "联系作者",
                subtitle = "小红书 @琪琪",
                trailing = { XiaohongshuLogo() },
                showArrow = true,
                onClick = { navController.navigate(Routes.AUTHOR) },
            )
            DividerLine()
            SettingsRow(
                title = "赞赏作者",
                subtitle = "请我喝一杯咖啡",
                trailing = {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFF6B8A),
                        modifier = Modifier.size(19.dp),
                    )
                },
                showArrow = true,
                onClick = { navController.navigate(Routes.REWARD) },
            )
            DividerLine()
            SettingsRow(
                title = "版本号",
                trailing = {
                    Text(
                        "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        color = c.textTertiary,
                        fontSize = 13.sp,
                    )
                },
            )
            DividerLine()
            SettingsRow(
                title = "开源许可",
                showArrow = true,
                onClick = { licenseDialog = true },
            )
            DividerLine()
            SettingsRow(
                title = "隐私说明",
                showArrow = true,
                onClick = { privacyDialog = true },
            )
        }

        Spacer(Modifier.height(30.dp))
        Text(
            "岚播放器 · ArashiPlayer",
            modifier = Modifier.fillMaxWidth(),
            color = c.textTertiary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Box(Modifier.navigationBarsPadding().height(12.dp))
    }

    /* ---------- 弹窗 ---------- */

    if (subtitleDialog) {
        AlertDialog(
            onDismissRequest = { subtitleDialog = false },
            title = { Text("默认字幕样式") },
            text = {
                Text(
                    "当前默认样式：字号 ${subStyle.textSizeSp.toInt()}sp，" +
                        "${subtitleColorLabel(subStyle)}，" +
                        (if (subStyle.bold) "加粗" else "常规") + "。\n\n" +
                        "字号、颜色、描边、位置与时间轴偏移都可以在播放页的字幕面板里实时调整，" +
                        "调整后会保存为新的默认样式。"
                )
            },
            confirmButton = {
                TextButton(onClick = { subtitleDialog = false }) { Text("知道了") }
            },
        )
    }

    if (licenseDialog) {
        AlertDialog(
            onDismissRequest = { licenseDialog = false },
            title = { Text("开源许可") },
            text = {
                Text(
                    "本应用基于以下开源项目构建：\n\n" +
                        "· AndroidX / Jetpack Compose —— Apache License 2.0\n" +
                        "· AndroidX Media3 (ExoPlayer) —— Apache License 2.0\n" +
                        "· Coil —— Apache License 2.0\n" +
                        "· OkHttp —— Apache License 2.0\n" +
                        "· Room / DataStore —— Apache License 2.0\n\n" +
                        "感谢每一位开源作者的付出。"
                )
            },
            confirmButton = {
                TextButton(onClick = { licenseDialog = false }) { Text("关闭") }
            },
        )
    }

    if (privacyDialog) {
        AlertDialog(
            onDismissRequest = { privacyDialog = false },
            title = { Text("隐私说明") },
            text = {
                Text(
                    "· 媒体扫描、播放记录、加密空间索引全部保存在本机，不会上传。\n\n" +
                        "· 加密空间只做「原地改名 + 改后缀」，不复制文件、不额外占用空间，" +
                        "也不会上传任何文件内容。\n\n" +
                        "· 只有在开启「自动匹配在线封面与歌词」时，才会把歌曲的标题与歌手名发送到在线接口，" +
                        "用于检索封面和歌词。\n\n" +
                        "· 关闭该开关后，本应用不会再发起任何联网请求。"
                )
            },
            confirmButton = {
                TextButton(onClick = { privacyDialog = false }) { Text("关闭") }
            },
        )
    }
}

/* ============================ 局部组件 ============================ */

@Composable
private fun SettingBlock(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    val c = ArashiTheme.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp)) {
        Text(title, color = c.text, fontSize = 15.5.sp, fontWeight = FontWeight.Medium)
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = c.textTertiary, fontSize = 12.5.sp)
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

private fun subtitleColorLabel(style: SubtitleStyle): String = when (style.textColor) {
    0xFFFFFFFF.toInt() -> "白色"
    0xFFFFE066.toInt() -> "黄色"
    else -> "自定义色"
}
