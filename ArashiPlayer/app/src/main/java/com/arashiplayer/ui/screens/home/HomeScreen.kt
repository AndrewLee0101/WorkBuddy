package com.arashiplayer.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.arashiplayer.ArashiApp
import com.arashiplayer.data.local.RecentEntity
import com.arashiplayer.data.model.Playable
import com.arashiplayer.ui.components.ArashiCard
import com.arashiplayer.ui.components.CoverPlaceholder
import com.arashiplayer.ui.navigation.Routes
import com.arashiplayer.ui.theme.ArashiBlueDeep
import com.arashiplayer.ui.theme.ArashiCyan
import com.arashiplayer.ui.theme.ArashiGradientSoft
import com.arashiplayer.ui.theme.ArashiShape
import com.arashiplayer.ui.theme.ArashiTheme
import com.arashiplayer.ui.theme.ArashiViolet
import com.arashiplayer.util.formatDateTime
import kotlinx.coroutines.delay

/**
 * 首页。
 *
 * 视觉结构（自上而下）：
 *  1. 「岚」巨型水印（右上出血，渐变填充）
 *  2. 品牌标题 + 设置入口
 *  3. 两张等宽大卡片：左＝视频、右＝音乐（图标分别贴在左上 / 右上）
 *  4. 媒体库三宫格入口
 *  5. 最近播放横滑
 *  6. 底部固定的迷你播放条
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val c = ArashiTheme.colors
    val context = LocalContext.current
    val app = remember(context) { ArashiApp.of(context) }

    var videoCount by remember { mutableStateOf(0) }
    var musicCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        videoCount = runCatching { app.mediaRepo.loadVideos().size }.getOrDefault(0)
        musicCount = runCatching { app.mediaRepo.loadMusic().size }.getOrDefault(0)
    }

    val vaultCount by app.vaultRepo.count.collectAsState(initial = 0)
    val recentFlow = remember { app.database.recentDao().observe(10) }
    val recents by recentFlow.collectAsState(initial = emptyList<RecentEntity>())
    val current by app.playerHolder.current.collectAsState()

    Box(Modifier.fillMaxSize().background(c.background)) {

        /* ---------- 背景水印：右上角出血 ---------- */
        Text(
            text = "岚",
            style = TextStyle(
                brush = ArashiGradientSoft,
                fontSize = 200.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 46.dp, y = (-14).dp)
                .alpha(0.85f),
        )

        /* ---------- 主体 ---------- */
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 104.dp),
        ) {
            item { HomeHeader(onSettings = { navController.navigate(Routes.SETTINGS) }) }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    MediaEntranceCard(
                        modifier = Modifier.weight(1f),
                        gradient = Brush.linearGradient(listOf(ArashiBlueDeep, ArashiViolet)),
                        title = "视频",
                        subtitle = "本地高清播放 · 外挂字幕",
                        countText = "$videoCount 个视频",
                        icon = Icons.Rounded.Videocam,
                        iconAtStart = true,
                        onClick = { navController.navigate(Routes.VIDEO) },
                    )
                    MediaEntranceCard(
                        modifier = Modifier.weight(1f),
                        gradient = Brush.linearGradient(listOf(ArashiViolet, ArashiCyan)),
                        title = "音乐",
                        subtitle = "无损音乐 · 在线封面歌词",
                        countText = "$musicCount 首歌",
                        icon = Icons.Rounded.MusicNote,
                        iconAtStart = false,
                        onClick = { navController.navigate(Routes.MUSIC) },
                    )
                }
            }

            item { HomeSectionTitle("媒体库", top = 26.dp) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LibraryEntry(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.History,
                        title = "最近播放",
                        subtitle = if (recents.isEmpty()) "暂无记录" else "${recents.size} 条记录",
                        tint = ArashiViolet,
                        onClick = { navController.navigate(Routes.MUSIC) },
                    )
                    LibraryEntry(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Favorite,
                        title = "收藏",
                        subtitle = "喜欢的都在这",
                        tint = Color(0xFFFF6B8A),
                        onClick = { navController.navigate(Routes.MUSIC) },
                    )
                    LibraryEntry(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Lock,
                        title = "加密空间",
                        subtitle = if (vaultCount > 0) "已加密 $vaultCount 个" else "未加密文件",
                        tint = ArashiCyan,
                        onClick = { navController.navigate(Routes.VAULT) },
                    )
                }
            }

            item { HomeSectionTitle("最近播放", top = 26.dp) }

            item {
                if (recents.isEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(96.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "还没有播放记录，去挑一个吧",
                            color = c.textTertiary,
                            fontSize = 13.sp,
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(recents, key = { it.mediaKey }) { item ->
                            RecentCard(
                                item = item,
                                onClick = {
                                    if (item.isVideo) {
                                        navController.navigate(Routes.videoPlayer(item.uri, item.title))
                                    } else {
                                        navController.navigate(Routes.MUSIC)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        /* ---------- 底部迷你播放条 ---------- */
        val playing = current
        if (playing != null) {
            MiniPlayerBar(
                playable = playing,
                isPlaying = {
                    runCatching { app.playerHolder.player.isPlaying }.getOrDefault(false)
                },
                onToggle = { app.playerHolder.togglePlayPause() },
                onOpen = { navController.navigate(Routes.MUSIC_PLAYER) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    }
}

/* ============================ 顶部标题 ============================ */

@Composable
private fun HomeHeader(onSettings: () -> Unit) {
    val c = ArashiTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 22.dp, end = 16.dp, top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "岚播放器",
                color = c.text,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "风起时，替你收藏每一帧声音与画面",
                color = c.textTertiary,
                fontSize = 12.5.sp,
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(ArashiShape.pill)
                .background(c.card)
                .clickable(onClick = onSettings),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = "设置",
                tint = c.textSecondary,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

@Composable
private fun HomeSectionTitle(text: String, top: androidx.compose.ui.unit.Dp = 22.dp) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 22.dp, top = top, bottom = 10.dp),
        color = ArashiTheme.colors.text,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

/* ============================ 两张主卡片 ============================ */

@Composable
private fun MediaEntranceCard(
    modifier: Modifier = Modifier,
    gradient: Brush,
    title: String,
    subtitle: String,
    countText: String,
    icon: ImageVector,
    iconAtStart: Boolean,
    onClick: () -> Unit,
) {
    val iconAlignment = if (iconAtStart) Alignment.TopStart else Alignment.TopEnd
    val blobAlignment = if (iconAtStart) Alignment.BottomEnd else Alignment.BottomStart

    Box(
        modifier = modifier
            .height(200.dp)
            .shadow(14.dp, ArashiShape.xl, clip = false)
            .clip(ArashiShape.xl)
            .background(gradient)
            .clickable(onClick = onClick),
    ) {
        // 装饰光斑
        Box(
            Modifier
                .size(132.dp)
                .align(blobAlignment)
                .offset(
                    x = if (iconAtStart) 34.dp else (-34).dp,
                    y = 40.dp,
                )
                .clip(ArashiShape.pill)
                .background(Color.White.copy(alpha = 0.10f)),
        )

        // 图标：视频贴左上、音乐贴右上
        Box(
            modifier = Modifier
                .align(iconAlignment)
                .padding(16.dp)
                .size(46.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(25.dp))
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
        ) {
            Text(
                title,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 11.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .clip(ArashiShape.pill)
                    .background(Color.White.copy(alpha = 0.20f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    countText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/* ============================ 三宫格入口 ============================ */

@Composable
private fun LibraryEntry(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit,
) {
    val c = ArashiTheme.colors
    ArashiCard(modifier = modifier, shape = ArashiShape.lg, onClick = onClick) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.height(9.dp))
            Text(title, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                color = c.textTertiary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/* ============================ 最近播放卡片 ============================ */

@Composable
private fun RecentCard(item: RecentEntity, onClick: () -> Unit) {
    val c = ArashiTheme.colors
    ArashiCard(
        modifier = Modifier.width(132.dp),
        shape = ArashiShape.md,
        onClick = onClick,
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(78.dp)) {
                CoverPlaceholder(
                    seed = item.title.hashCode(),
                    modifier = Modifier.fillMaxSize().clip(ArashiShape.md),
                )
                Icon(
                    imageVector = if (item.isVideo) Icons.Rounded.Movie else Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.align(Alignment.Center).size(26.dp),
                )
            }
            Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
                Text(
                    item.title,
                    color = c.text,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    formatDateTime(item.playedAt),
                    color = c.textTertiary,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

/* ============================ 迷你播放条 ============================ */

@Composable
private fun MiniPlayerBar(
    playable: Playable,
    isPlaying: () -> Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = ArashiTheme.colors
    var playing by remember(playable) { mutableStateOf(runCatching { isPlaying() }.getOrDefault(false)) }

    // 播放器状态由 media3 内部维护，这里做一个轻量轮询，避免把 Player.Listener 泄漏到 UI 层
    LaunchedEffect(playable) {
        while (true) {
            playing = runCatching { isPlaying() }.getOrDefault(false)
            delay(400)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, ArashiShape.lg, clip = false)
            .clip(ArashiShape.lg)
            .background(c.card)
            .clickable(onClick = onOpen)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverPlaceholder(
            seed = playable.title.hashCode(),
            modifier = Modifier.size(42.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                playable.title,
                color = c.text,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                playable.subtitle,
                color = c.textTertiary,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(ArashiShape.pill)
                .background(c.cardPressed)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (playing) "暂停" else "播放",
                tint = c.text,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}
