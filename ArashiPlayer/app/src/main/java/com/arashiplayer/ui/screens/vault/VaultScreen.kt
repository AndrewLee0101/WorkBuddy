package com.arashiplayer.ui.screens.vault

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.arashiplayer.ArashiApp
import com.arashiplayer.data.model.MusicTrack
import com.arashiplayer.data.model.Playable
import com.arashiplayer.data.model.VaultConfig
import com.arashiplayer.data.model.VaultItem
import com.arashiplayer.data.model.VaultKind
import com.arashiplayer.data.model.VaultLockMode
import com.arashiplayer.security.LockKit
import com.arashiplayer.ui.components.EmptyState
import com.arashiplayer.ui.components.GradientButton
import com.arashiplayer.ui.components.ArashiCard
import com.arashiplayer.ui.navigation.Routes
import com.arashiplayer.ui.theme.ArashiBlue
import com.arashiplayer.ui.theme.ArashiBlueDeep
import com.arashiplayer.ui.theme.ArashiGradient
import com.arashiplayer.ui.theme.ArashiShape
import com.arashiplayer.ui.theme.ArashiTheme
import com.arashiplayer.ui.theme.ArashiViolet
import com.arashiplayer.util.formatDateTime
import com.arashiplayer.util.formatSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * 加密空间。
 *
 * 三段式状态机：
 *  1. 未设置密码 → 首次设置（数字 / 手势，两次确认）
 *  2. 已设置但未解锁 → 解锁页（PIN 键盘 / 手势九宫格，5 次错误锁 30 秒）
 *  3. 已解锁 → 文件网格（分类、预览、移出、彻底删除、添加）
 *
 * 离开页面即自动回锁（[DisposableEffect] onDispose）。
 */
@Composable
fun VaultScreen(navController: NavHostController) {
    val c = ArashiTheme.colors
    val context = LocalContext.current
    val app = remember(context) { ArashiApp.of(context) }
    // 用 produceState 读首帧为 null 的配置，避免未加载时误判成「未设置密码」
    val configState by produceState<VaultConfig?>(initialValue = null) {
        app.settings.vaultConfig.collect { latest -> value = latest }
    }
    val scope = rememberCoroutineScope()

    var unlocked by remember { mutableStateOf(false) }
    var justConfigured by remember { mutableStateOf(false) }

    // 自动回锁：离开本页（返回 / 切到别的路由）即复位
    DisposableEffect(Unit) {
        onDispose {
            unlocked = false
            justConfigured = false
        }
    }

    val config = configState
    if (config == null) {
        Box(Modifier.fillMaxSize().background(c.background))
        return
    }

    val showContent = unlocked || justConfigured

    Box(Modifier.fillMaxSize().background(c.background)) {
        when {
            !config.enabled -> VaultSetupView(
                onDone = { fresh ->
                    scope.launch { app.settings.saveVaultConfig(fresh) }
                    justConfigured = true
                },
            )

            !showContent -> VaultLockView(
                config = config,
                onSuccess = { unlocked = true },
            )

            else -> VaultContentView(
                navController = navController,
                onLock = {
                    unlocked = false
                    justConfigured = false
                },
                onResetPassword = {
                    scope.launch { app.settings.saveVaultConfig(VaultConfig()) }
                    unlocked = false
                    justConfigured = false
                },
            )
        }
    }
}

/* ============================================================================
 *  1. 首次设置
 * ========================================================================== */

@Composable
private fun VaultSetupView(onDone: (VaultConfig) -> Unit) {
    val c = ArashiTheme.colors
    var modeIndex by remember { mutableStateOf(0) }              // 0 = 数字，1 = 手势
    var pinLength by remember { mutableStateOf(4) }
    var firstCred by remember { mutableStateOf("") }
    var confirming by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var errorTick by remember { mutableStateOf(0) }

    fun reset() {
        firstCred = ""
        confirming = false
        error = null
    }

    fun accept(credential: String) {
        if (!confirming) {
            firstCred = credential
            confirming = true
            error = null
            return
        }
        if (credential == firstCred) {
            val salt = LockKit.newSalt()
            onDone(
                VaultConfig(
                    enabled = true,
                    mode = if (modeIndex == 0) VaultLockMode.PIN else VaultLockMode.GESTURE,
                    credentialHash = LockKit.hash(credential, salt),
                    credentialSalt = salt,
                    hint = hint.trim(),
                    hideThumbnail = true,
                )
            )
        } else {
            error = "两次输入不一致，请重新设置"
            errorTick++
            reset()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(30.dp))
        VaultBadge(84.dp)
        Spacer(Modifier.height(20.dp))
        Text("设置加密空间", color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "密码只保存在本机，用于解锁加密空间",
            color = c.textTertiary,
            fontSize = 12.5.sp,
        )
        Spacer(Modifier.height(26.dp))

        SegmentedRow(
            options = listOf("数字密码", "手势密码"),
            selectedIndex = modeIndex,
            modifier = Modifier.fillMaxWidth(),
        ) { index ->
            modeIndex = index
            reset()
        }

        if (modeIndex == 0) {
            Spacer(Modifier.height(12.dp))
            SegmentedRow(
                options = listOf("4 位", "5 位", "6 位"),
                selectedIndex = pinLength - 4,
                modifier = Modifier.fillMaxWidth(),
            ) { index ->
                pinLength = index + 4
                reset()
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = when {
                confirming -> "请再次输入以确认"
                modeIndex == 0 -> "请输入 $pinLength 位数字密码"
                else -> "请绘制手势，至少连接 4 个点"
            },
            color = c.textSecondary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(20.dp))

        key(confirming, modeIndex, pinLength) {
            if (modeIndex == 0) {
                PinPadView(pinLength = pinLength, onPin = { accept(it) })
            } else {
                PatternLockView(
                    modifier = Modifier.fillMaxWidth(),
                    errorTick = errorTick,
                    onPattern = { pattern ->
                        if (pattern.split("-").size < 4) {
                            error = "手势至少需要连接 4 个点"
                            errorTick++
                        } else {
                            accept(pattern)
                        }
                    },
                )
            }
        }

        error?.let {
            Spacer(Modifier.height(14.dp))
            Text(it, color = c.danger, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(26.dp))
        OutlinedTextField(
            value = hint,
            onValueChange = { hint = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("密码提示（可选）") },
            placeholder = { Text("例如：生日后四位") },
            singleLine = true,
            shape = ArashiShape.sm,
        )
        Spacer(Modifier.height(34.dp))
    }
}

/* ============================================================================
 *  2. 解锁
 * ========================================================================== */

@Composable
private fun VaultLockView(config: VaultConfig, onSuccess: () -> Unit) {
    val c = ArashiTheme.colors
    val context = LocalContext.current

    var errorTick by remember { mutableStateOf(0) }
    var failures by remember { mutableStateOf(0) }
    var lockUntil by remember { mutableStateOf(0L) }
    var remainSec by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    val shake = remember { Animatable(0f) }

    val locked = remainSec > 0

    LaunchedEffect(lockUntil) {
        while (lockUntil > System.currentTimeMillis()) {
            remainSec = ((lockUntil - System.currentTimeMillis()) / 1000L).toInt() + 1
            delay(400)
        }
        remainSec = 0
    }

    LaunchedEffect(errorTick) {
        if (errorTick <= 0) return@LaunchedEffect
        shake.snapTo(0f)
        repeat(3) {
            shake.animateTo(18f, tween(46))
            shake.animateTo(-18f, tween(46))
        }
        shake.animateTo(0f, tween(70))
    }

    fun fail(text: String) {
        errorTick++
        vibrateOnce(context)
        failures++
        message = text
        if (failures >= 5) {
            failures = 0
            lockUntil = System.currentTimeMillis() + 30_000L
            remainSec = 30
            message = "错误次数过多，请 30 秒后再试"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(46.dp))
        Box(Modifier.offset { IntOffset(shake.value.toInt(), 0) }) {
            VaultBadge(88.dp)
        }
        Spacer(Modifier.height(22.dp))
        Text(
            "加密空间",
            color = c.text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when {
                locked -> "已临时锁定，请稍候"
                else -> config.hint.ifBlank { "输入密码以继续" }
            },
            color = c.textTertiary,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(30.dp))

        if (config.mode == VaultLockMode.PIN) {
            key("vault-unlock-pin") {
                PinPadView(
                    pinLength = 6,
                    onPin = { value ->
                        if (!locked) {
                            if (LockKit.verify(value, config.credentialSalt, config.credentialHash)) {
                                message = null
                                onSuccess()
                            } else if (value.length >= 6) {
                                fail("密码错误，请重试")
                            }
                        }
                    },
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "4~6 位数字密码，输入完成后自动校验",
                color = c.textTertiary,
                fontSize = 11.5.sp,
            )
        } else {
            PatternLockView(
                modifier = Modifier.fillMaxWidth(),
                errorTick = errorTick,
                enabled = !locked,
                onPattern = { pattern ->
                    if (!locked) {
                        if (LockKit.verify(pattern, config.credentialSalt, config.credentialHash)) {
                            message = null
                            onSuccess()
                        } else {
                            fail("手势错误，请重试")
                        }
                    }
                },
            )
        }

        message?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = c.danger, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
        }
        if (locked) {
            Spacer(Modifier.height(10.dp))
            Text("$remainSec 秒后可再次尝试", color = c.textSecondary, fontSize = 12.5.sp)
        }
        if (config.hint.isNotBlank()) {
            Spacer(Modifier.height(22.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(ArashiShape.sm)
                    .background(c.card)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
            ) {
                Text("提示：${config.hint}", color = c.textSecondary, fontSize = 12.5.sp)
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

/* ============================================================================
 *  3. 已解锁：文件管理
 * ========================================================================== */

@Composable
private fun VaultContentView(
    navController: NavHostController,
    onLock: () -> Unit,
    onResetPassword: () -> Unit,
) {
    val c = ArashiTheme.colors
    val context = LocalContext.current
    val app = remember(context) { ArashiApp.of(context) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val items by app.vaultRepo.items.collectAsState(initial = emptyList<VaultItem>())
    val totalSize by app.vaultRepo.totalSize.collectAsState(initial = 0L)
    val countsFlow = remember { app.vaultRepo.countsByKind() }
    val counts by countsFlow.collectAsState(initial = emptyMap<VaultKind, Int>())

    var filter by remember { mutableStateOf<VaultKind?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var infoOpen by remember { mutableStateOf(false) }
    var previewItem by remember { mutableStateOf<VaultItem?>(null) }
    var confirmRestoreAll by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<VaultItem?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            scope.launch {
                val result = app.vaultRepo.hideUris(uris)
                snackbar.showSnackbar(
                    if (result.added > 0) {
                        "已加密 ${result.added} 个文件，未额外占用空间"
                    } else {
                        "未能加入加密空间，请确认已授予文件访问权限"
                    }
                )
            }
        }
    }

    val filtered = remember(items, filter) {
        if (filter == null) items else items.filter { it.kind == filter }
    }

    Box(Modifier.fillMaxSize().background(c.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {

            /* ---------- 顶栏 ---------- */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 12.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "加密空间",
                        color = c.text,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${items.size} 个文件 · ${formatSize(totalSize)}",
                        color = c.textTertiary,
                        fontSize = 12.5.sp,
                    )
                }
                Box {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(ArashiShape.pill)
                            .background(c.card)
                            .clickable { menuOpen = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "更多",
                            tint = c.textSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("重新设置密码") },
                            onClick = { menuOpen = false; onResetPassword() },
                        )
                        DropdownMenuItem(
                            text = { Text("全部移出加密空间") },
                            onClick = { menuOpen = false; confirmRestoreAll = true },
                        )
                        DropdownMenuItem(
                            text = { Text("立即锁定") },
                            onClick = { menuOpen = false; onLock() },
                        )
                    }
                }
            }

            /* ---------- 说明条 ---------- */
            InfoBanner(onHelp = { infoOpen = true })

            /* ---------- 分类胶囊 ---------- */
            Spacer(Modifier.height(14.dp))
            CategoryChips(
                counts = counts,
                total = items.size,
                selected = filter,
                onSelect = { filter = it },
            )

            /* ---------- 网格 ---------- */
            Spacer(Modifier.height(12.dp))
            if (filtered.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon = Icons.Rounded.Lock,
                        title = "加密空间是空的",
                        desc = "把照片、视频、音乐放进来，它们会从相册与扫描器中消失",
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 116.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filtered, key = { it.id }) { item ->
                        VaultCell(
                            item = item,
                            onPreview = { previewItem = item },
                            onRestore = {
                                scope.launch {
                                    val ok = app.vaultRepo.restore(item)
                                    snackbar.showSnackbar(
                                        if (ok) "已移出加密空间：${item.displayTitle}"
                                        else "移出失败，请检查文件权限"
                                    )
                                }
                            },
                            onDelete = { pendingDelete = item },
                        )
                    }
                }
            }
        }

        /* ---------- 悬浮添加 ---------- */
        GradientButton(
            text = "添加文件",
            icon = Icons.Rounded.Add,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
        ) {
            picker.launch(arrayOf("*/*"))
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 92.dp, start = 14.dp, end = 14.dp),
        )
    }

    /* ---------- 弹窗区 ---------- */

    previewItem?.let { item ->
        VaultPreviewDialog(
            item = item,
            onPlayVideo = { target ->
                previewItem = null
                val uri = LockKit.vaultUri(context, File(target.currentPath))
                navController.navigate(Routes.videoPlayer(uri.toString(), target.displayTitle))
            },
            onPlayAudio = { target ->
                previewItem = null
                val uri = LockKit.vaultUri(context, File(target.currentPath))
                app.playerHolder.play(
                    Playable.Music(
                        MusicTrack(
                            id = target.id,
                            title = target.displayTitle,
                            artist = "加密空间",
                            album = "加密空间",
                            albumId = 0L,
                            durationMs = 0L,
                            path = target.currentPath,
                            uri = uri.toString(),
                            sizeBytes = target.sizeBytes,
                            dateAdded = target.addedAt,
                        )
                    )
                )
                navController.navigate(Routes.MUSIC_PLAYER)
            },
            onDismiss = { previewItem = null },
        )
    }

    if (infoOpen) {
        AlertDialog(
            onDismissRequest = { infoOpen = false },
            title = { Text("加密空间是怎么工作的？") },
            text = {
                Text(
                    "1. 文件被原地改名为随机串并改成 .arv 后缀，统一放进同目录下的 " +
                        ".arashi_vault 文件夹。\n\n" +
                        "2. 该目录会写入 .nomedia，相册与第三方 App 的扫描器都会跳过，" +
                        "因此在系统相册、其他播放器里看不到它们。\n\n" +
                        "3. 全程只是一次 rename，不复制字节，所以不会额外占用空间。\n\n" +
                        "4. 随时可以「移出加密空间」，文件名与后缀会还原成原来的样子。"
                )
            },
            confirmButton = {
                TextButton(onClick = { infoOpen = false }) { Text("知道了") }
            },
        )
    }

    if (confirmRestoreAll) {
        AlertDialog(
            onDismissRequest = { confirmRestoreAll = false },
            title = { Text("全部移出？") },
            text = { Text("所有文件都会还原成原文件名并回到原目录，加密空间将被清空。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestoreAll = false
                    scope.launch {
                        val n = app.vaultRepo.restoreAll()
                        snackbar.showSnackbar("已移出 $n 个文件")
                    }
                }) { Text("全部移出") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestoreAll = false }) { Text("取消") }
            },
        )
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("彻底删除？") },
            text = { Text("「${item.displayTitle}」将被永久删除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    scope.launch {
                        val ok = app.vaultRepo.deleteForever(item)
                        snackbar.showSnackbar(if (ok) "已彻底删除" else "删除失败")
                    }
                }) { Text("删除", color = c.danger) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}

/* ============================================================================
 *  局部组件
 * ========================================================================== */

/** 用 Canvas 画出的 App 图标：渐变圆角方块 + 「岚」字 */
@Composable
private fun VaultBadge(size: androidx.compose.ui.unit.Dp = 84.dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                brush = ArashiGradient,
                cornerRadius = CornerRadius(this.size.minDimension * 0.28f),
            )
        }
        Text(
            text = "岚",
            color = Color.White,
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun InfoBanner(onHelp: () -> Unit) {
    val c = ArashiTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(top = 14.dp)
            .clip(ArashiShape.sm)
            .background(c.card)
            .padding(start = 13.dp, end = 5.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Info,
            contentDescription = null,
            tint = ArashiBlue,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "仅改名换后缀，不复制、不额外占用空间",
            color = c.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(ArashiShape.pill)
                .clickable(onClick = onHelp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.HelpOutline,
                contentDescription = "说明",
                tint = c.textTertiary,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Composable
private fun CategoryChips(
    counts: Map<VaultKind, Int>,
    total: Int,
    selected: VaultKind?,
    onSelect: (VaultKind?) -> Unit,
) {
    val c = ArashiTheme.colors
    val entries: List<Pair<String, VaultKind?>> = listOf(
        "全部" to null,
        "图片" to VaultKind.IMAGE,
        "视频" to VaultKind.VIDEO,
        "音频" to VaultKind.AUDIO,
        "文档" to VaultKind.DOC,
        "其他" to VaultKind.OTHER,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        entries.forEach { (label, kind) ->
            val count = if (kind == null) total else (counts[kind] ?: 0)
            val active = selected == kind
            Box(
                modifier = Modifier
                    .clip(ArashiShape.pill)
                    .background(if (active) c.text else c.cardPressed)
                    .clickable { onSelect(kind) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = if (count > 0) "$label $count" else label,
                    color = if (active) c.background else c.textSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun VaultCell(
    item: VaultItem,
    onPreview: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = ArashiTheme.colors
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(ArashiShape.md)
                .background(c.cardPressed)
                .clickable(onClick = onPreview),
        ) {
            if (item.kind == VaultKind.IMAGE) {
                AsyncImage(
                    model = LockKit.vaultUri(context, File(item.currentPath)),
                    contentDescription = item.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    Modifier.fillMaxSize().background(kindGradient(item.kind)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        kindVector(item.kind),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(30.dp),
                    )
                }
            }

            // 类型角标
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(ArashiShape.xs)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    kindLabel(item.kind),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // 「…」菜单
            Box(Modifier.align(Alignment.TopEnd).padding(2.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(ArashiShape.pill)
                        .background(Color.Black.copy(alpha = 0.32f))
                        .clickable { menu = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "更多操作",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("预览") },
                        onClick = { menu = false; onPreview() },
                    )
                    DropdownMenuItem(
                        text = { Text("移出加密空间") },
                        onClick = { menu = false; onRestore() },
                    )
                    DropdownMenuItem(
                        text = { Text("彻底删除") },
                        onClick = { menu = false; onDelete() },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = c.danger,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            item.displayTitle,
            color = c.text,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            formatSize(item.sizeBytes),
            color = c.textTertiary,
            fontSize = 10.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun VaultPreviewDialog(
    item: VaultItem,
    onPlayVideo: (VaultItem) -> Unit,
    onPlayAudio: (VaultItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = ArashiTheme.colors
    val context = LocalContext.current

    if (item.kind == VaultKind.IMAGE) {
        var scale by remember { mutableStateOf(1f) }
        var pan by remember { mutableStateOf(Offset.Zero) }

        Dialog(onDismissRequest = onDismiss) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .clip(ArashiShape.lg)
                    .background(Color.Black),
            ) {
                AsyncImage(
                    model = LockKit.vaultUri(context, File(item.currentPath)),
                    contentDescription = item.displayTitle,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, panChange, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 6f)
                                pan = if (scale > 1f) pan + panChange else Offset.Zero
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = pan.x
                            translationY = pan.y
                        },
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(34.dp)
                        .clip(ArashiShape.pill)
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "关闭",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    } else {
        Dialog(onDismissRequest = onDismiss) {
            ArashiCard(Modifier.fillMaxWidth(), shape = ArashiShape.lg) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        item.displayTitle,
                        color = c.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${kindLabel(item.kind)} · ${formatSize(item.sizeBytes)}",
                        color = c.textSecondary,
                        fontSize = 12.5.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "加入时间：${formatDateTime(item.addedAt)}",
                        color = c.textTertiary,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "当前路径：${item.currentPath}",
                        color = c.textTertiary,
                        fontSize = 10.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (item.kind == VaultKind.VIDEO || item.kind == VaultKind.AUDIO) {
                        Spacer(Modifier.height(18.dp))
                        GradientButton(
                            text = "用播放器打开",
                            icon = Icons.Rounded.PlayArrow,
                        ) {
                            if (item.kind == VaultKind.VIDEO) onPlayVideo(item) else onPlayAudio(item)
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
            }
        }
    }
}

/* ============================================================================
 *  小工具
 * ========================================================================== */

private fun kindLabel(kind: VaultKind): String = when (kind) {
    VaultKind.IMAGE -> "图片"
    VaultKind.VIDEO -> "视频"
    VaultKind.AUDIO -> "音频"
    VaultKind.DOC -> "文档"
    VaultKind.OTHER -> "其他"
}

private fun kindVector(kind: VaultKind): ImageVector = when (kind) {
    VaultKind.IMAGE -> Icons.Rounded.Image
    VaultKind.VIDEO -> Icons.Rounded.Movie
    VaultKind.AUDIO -> Icons.Rounded.MusicNote
    VaultKind.DOC -> Icons.Rounded.Description
    VaultKind.OTHER -> Icons.Rounded.Folder
}

private fun kindGradient(kind: VaultKind): Brush = when (kind) {
    VaultKind.IMAGE -> Brush.linearGradient(listOf(Color(0xFF3FBF8F), Color(0xFF35D6E8)))
    VaultKind.VIDEO -> Brush.linearGradient(listOf(ArashiBlueDeep, ArashiViolet))
    VaultKind.AUDIO -> Brush.linearGradient(listOf(ArashiViolet, Color(0xFFFF6B8A)))
    VaultKind.DOC -> Brush.linearGradient(listOf(Color(0xFFFFA53D), Color(0xFFFF6B8A)))
    VaultKind.OTHER -> Brush.linearGradient(listOf(Color(0xFF5A6070), Color(0xFF2F3444)))
}

@Suppress("DEPRECATION")
private fun vibrateOnce(context: Context) {
    runCatching {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(60L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(60L)
        }
    }
}
