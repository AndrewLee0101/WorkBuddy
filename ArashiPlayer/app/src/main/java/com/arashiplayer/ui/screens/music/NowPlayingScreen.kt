package com.arashiplayer.ui.screens.music

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.arashiplayer.ArashiApp
import com.arashiplayer.data.model.LyricLine
import com.arashiplayer.data.model.MusicTrack
import com.arashiplayer.data.model.Playable
import com.arashiplayer.ui.components.DividerLine
import com.arashiplayer.ui.components.SettingsRow
import com.arashiplayer.ui.theme.ArashiBlue
import com.arashiplayer.ui.theme.ArashiGradient
import com.arashiplayer.ui.theme.ArashiShape
import com.arashiplayer.ui.theme.ArashiTheme
import com.arashiplayer.util.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

private val SpeedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

/**
 * 正在播放页。整页固定深色沉浸 —— 外层强制走深色调色板，
 * 因此内部所有取色都来自 [ArashiTheme.colors]，无需任何硬编码。
 */
@Composable
fun NowPlayingScreen(navController: NavHostController) {
    ArashiTheme(darkTheme = true) {
        PlayerScreenContent(navController)
    }
}

@Composable
private fun PlayerScreenContent(navController: NavHostController) {
    val context = LocalContext.current
    val settings = remember { ArashiApp.of(context).settings }
    val vm: MusicViewModel = viewModel()

    val currentPlayable by vm.playerHolder.current.collectAsState()
    val meta by vm.meta.collectAsState()
    val lyrics by vm.lyrics.collectAsState()
    val matching by vm.matching.collectAsState()
    val favorite by vm.favorite.collectAsState()

    var track by remember { mutableStateOf((currentPlayable as? Playable.Music)?.track) }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableStateOf(0) }
    var shuffle by remember { mutableStateOf(false) }

    var showLyrics by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf(0f) }
    var speed by remember { mutableStateOf(1f) }
    var volume by remember { mutableStateOf(1f) }
    var sheet by remember { mutableStateOf(PlayerSheet.NONE) }

    /** 播放状态轮询：位置、时长、播放中、循环/随机，并跟随队列自动切歌 */
    LaunchedEffect(Unit) {
        while (true) {
            val p = vm.playerHolder.player
            val fromQueue = vm.playerHolder.queueSnapshot
                .getOrNull(p.currentMediaItemIndex) as? Playable.Music
            if (fromQueue != null) track = fromQueue.track
            else (currentPlayable as? Playable.Music)?.let { track = it.track }

            duration = p.duration.takeIf { it > 0 } ?: (track?.durationMs ?: 0L)
            if (!dragging) position = p.currentPosition.coerceAtLeast(0L)
            isPlaying = p.isPlaying
            repeatMode = p.repeatMode
            shuffle = p.shuffleModeEnabled
            delay(200)
        }
    }

    /** 换歌时载入封面 / 歌词（先读缓存，未命中再联网） */
    LaunchedEffect(track?.key) {
        val t = track ?: return@LaunchedEffect
        vm.loadMeta(t, allowMatch = settings.autoMatchOnline.first())
        vm.loadFavorite(t)
    }

    val coverUri = meta?.coverUrl?.takeIf { it.isNotBlank() } ?: track?.embeddedCoverUri
    val fraction = if (dragging) dragFraction
    else if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f

    Box(
        Modifier
            .fillMaxSize()
            .background(ArashiTheme.colors.playerBg)
    ) {
        PlayerBackdrop(coverUri = coverUri, seed = track?.id?.toInt() ?: 0)

        val playing = track
        if (playing == null) {
            NothingPlaying(onClose = { navController.popBackStack() })
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = 24.dp)
            ) {
                PlayerTopBar(
                    matching = matching,
                    onClose = { navController.popBackStack() },
                    onRematch = { vm.rematch(playing) },
                )

                Spacer(Modifier.height(12.dp))

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (showLyrics) {
                        LyricsPanel(
                            lyrics = lyrics,
                            positionMs = position,
                            onSeek = { vm.playerHolder.seekTo(it) },
                            onRematch = { vm.rematch(playing) },
                        )
                    } else {
                        CoverArt(
                            coverUri = coverUri,
                            seed = playing.id.toInt(),
                            playing = isPlaying,
                            onClick = { showLyrics = true },
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                TitleBlock(
                    track = playing,
                    favorite = favorite,
                    onToggleFavorite = { vm.toggleFavorite(playing) },
                    onMore = { sheet = PlayerSheet.MORE },
                )

                Spacer(Modifier.height(20.dp))

                ArashiProgressSlider(
                    fraction = fraction,
                    onDragStart = { dragging = true; dragFraction = fraction },
                    onDrag = { dragFraction = it },
                    onDragEnd = {
                        dragging = false
                        vm.playerHolder.seekTo((dragFraction * duration).toLong())
                    },
                    onTap = { vm.playerHolder.seekTo((it * duration).toLong()) },
                    bubble = { f -> formatDuration((f * duration).toLong()) },
                )

                TimeRow(positionMs = position, durationMs = duration)

                Spacer(Modifier.height(14.dp))

                ControlRow(
                    isPlaying = isPlaying,
                    repeatMode = repeatMode,
                    shuffle = shuffle,
                    onPrevious = { vm.playerHolder.previous() },
                    onNext = { vm.playerHolder.next() },
                    onToggle = { vm.playerHolder.togglePlayPause() },
                    onRepeat = {
                        val next = when (repeatMode) {
                            0 -> 1
                            1 -> 2
                            else -> 0
                        }
                        vm.playerHolder.player.repeatMode = next
                        repeatMode = next
                    },
                    onShuffle = {
                        val v = !shuffle
                        vm.playerHolder.player.shuffleModeEnabled = v
                        shuffle = v
                    },
                )

                Spacer(Modifier.height(22.dp))

                ToolRow(
                    speed = speed,
                    lyricsOn = showLyrics,
                    onSpeed = { sheet = PlayerSheet.SPEED },
                    onLyrics = { showLyrics = !showLyrics },
                    onVolume = { sheet = PlayerSheet.VOLUME },
                )

                Spacer(Modifier.height(6.dp))
            }
        }
    }

    when (sheet) {
        PlayerSheet.SPEED -> PlayerSheetHost(onDismiss = { sheet = PlayerSheet.NONE }) {
            SpeedSheet(
                current = speed,
                onPick = {
                    speed = it
                    vm.playerHolder.setSpeed(it)
                    sheet = PlayerSheet.NONE
                },
            )
        }

        PlayerSheet.VOLUME -> PlayerSheetHost(onDismiss = { sheet = PlayerSheet.NONE }) {
            VolumeSheet(
                value = volume,
                onValue = {
                    volume = it
                    vm.playerHolder.setVolume(it)
                },
            )
        }

        PlayerSheet.MORE -> PlayerSheetHost(onDismiss = { sheet = PlayerSheet.NONE }) {
            MoreSheet(
                lyricsOn = showLyrics,
                onToggleLyrics = {
                    showLyrics = !showLyrics
                    sheet = PlayerSheet.NONE
                },
                onRematch = {
                    track?.let(vm::rematch)
                    sheet = PlayerSheet.NONE
                },
            )
        }

        PlayerSheet.NONE -> Unit
    }
}

private enum class PlayerSheet { NONE, SPEED, VOLUME, MORE }

/* ============================ 背景 ============================ */

@Composable
private fun PlayerBackdrop(coverUri: String?, seed: Int) {
    val c = ArashiTheme.colors
    Box(Modifier.fillMaxSize()) {
        if (!coverUri.isNullOrBlank()) {
            AsyncImage(
                model = coverUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(54.dp)
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(seedBrush(seed))
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            c.playerBg.copy(alpha = 0.58f),
                            c.playerBg.copy(alpha = 0.86f),
                            c.playerBg,
                        )
                    )
                )
        )
    }
}

/* ============================ 顶栏 ============================ */

@Composable
private fun PlayerTopBar(
    matching: Boolean,
    onClose: () -> Unit,
    onRematch: () -> Unit,
) {
    val c = ArashiTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            Modifier
                .align(Alignment.CenterStart)
                .size(34.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.KeyboardArrowDown,
                contentDescription = "收起",
                tint = c.text.copy(alpha = 0.85f),
                modifier = Modifier.size(26.dp)
            )
        }

        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .width(38.dp)
                    .height(5.dp)
                    .clip(ArashiShape.pill)
                    .background(c.text.copy(alpha = 0.28f))
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "正在播放",
                color = c.text.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp
            )
        }

        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .size(34.dp)
                .clip(CircleShape)
                .clickable(enabled = !matching, onClick = onRematch),
            contentAlignment = Alignment.Center
        ) {
            if (matching) {
                CircularProgressIndicator(
                    color = c.text.copy(alpha = 0.7f),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = "重新匹配",
                    tint = c.text.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/* ============================ 封面 ============================ */

@Composable
private fun CoverArt(
    coverUri: String?,
    seed: Int,
    playing: Boolean,
    onClick: () -> Unit,
) {
    val c = ArashiTheme.colors
    val coverSize = LocalConfiguration.current.screenWidthDp.dp - 72.dp
    val shape = RoundedCornerShape(22.dp)

    val infinite = rememberInfiniteTransition(label = "breathe")
    val breathe by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.016f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breatheScale",
    )
    val scale = if (playing) breathe else 1f

    Box(
        Modifier
            .size(coverSize)
            .scale(scale)
            .shadow(22.dp, shape)
            .clip(shape)
            .background(c.cardPressed)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (!coverUri.isNullOrBlank()) {
            AsyncImage(
                model = coverUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(seedBrush(seed))
            )
            Icon(
                Icons.Outlined.MusicNote,
                contentDescription = null,
                tint = c.text.copy(alpha = 0.4f),
                modifier = Modifier.size(coverSize * 0.2f)
            )
        }
    }
}

/* ============================ 标题 / 歌手 ============================ */

@Composable
private fun TitleBlock(
    track: MusicTrack,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onMore: () -> Unit,
) {
    val c = ArashiTheme.colors
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                color = c.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Text(
                track.artist,
                color = c.text.copy(alpha = 0.58f),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(14.dp))
        RoundIconButton(
            icon = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            tint = if (favorite) ArashiBlue else c.text.copy(alpha = 0.55f),
            desc = "收藏",
            onClick = onToggleFavorite,
        )
        Spacer(Modifier.width(6.dp))
        RoundIconButton(
            icon = Icons.Outlined.MoreHoriz,
            tint = c.text.copy(alpha = 0.55f),
            desc = "更多",
            onClick = onMore,
        )
    }
}

/* ============================ 进度条 ============================ */

@Composable
private fun ArashiProgressSlider(
    fraction: Float,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onTap: (Float) -> Unit,
    bubble: (Float) -> String,
) {
    val c = ArashiTheme.colors
    val density = LocalDensity.current
    var trackWidth by remember { mutableStateOf(1) }
    var active by remember { mutableStateOf(false) }

    val f = fraction.coerceIn(0f, 1f)
    val thumbX = (f * trackWidth).toInt()
    val bubbleWidthPx = with(density) { 64.dp.roundToPx() }
    val bubbleX = (thumbX - bubbleWidthPx / 2).coerceIn(0, (trackWidth - bubbleWidthPx).coerceAtLeast(0))
    val knobOverhangPx = with(density) { 7.dp.roundToPx() }

    Box(
        Modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        if (active) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(bubbleX, with(density) { (-26).dp.roundToPx() }) }
                    .width(64.dp)
                    .height(24.dp)
                    .clip(ArashiShape.pill)
                    .background(c.card)
                    .alpha(0.96f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    bubble(f),
                    color = c.text,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(32.dp)
                .onSizeChanged { trackWidth = it.width }
                .pointerInput(Unit) {
                    detectTapGestures { o -> onTap((o.x / trackWidth).coerceIn(0f, 1f)) }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { o ->
                            active = true
                            onDragStart()
                            onDrag((o.x / trackWidth).coerceIn(0f, 1f))
                        },
                        onDragEnd = {
                            active = false
                            onDragEnd()
                        },
                        onDragCancel = {
                            active = false
                            onDragEnd()
                        },
                        onHorizontalDrag = { change, _ ->
                            onDrag((change.position.x / trackWidth).coerceIn(0f, 1f))
                        },
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(ArashiShape.pill)
                    .background(c.text.copy(alpha = 0.2f))
            )
            Box(
                Modifier
                    .fillMaxWidth(f)
                    .height(4.dp)
                    .clip(ArashiShape.pill)
                    .background(ArashiGradient)
            )
            if (active) {
                Box(
                    Modifier
                        .offset { IntOffset(thumbX - knobOverhangPx, 0) }
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(ArashiGradient)
                        .shadow(6.dp, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun TimeRow(positionMs: Long, durationMs: Long) {
    val c = ArashiTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, start = 2.dp, end = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            formatDuration(positionMs),
            color = c.text.copy(alpha = 0.5f),
            fontSize = 11.5.sp
        )
        Text(
            "-${formatDuration((durationMs - positionMs).coerceAtLeast(0L))}",
            color = c.text.copy(alpha = 0.5f),
            fontSize = 11.5.sp
        )
    }
}

/* ============================ 控制区 ============================ */

@Composable
private fun ControlRow(
    isPlaying: Boolean,
    repeatMode: Int,
    shuffle: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggle: () -> Unit,
    onRepeat: () -> Unit,
    onShuffle: () -> Unit,
) {
    val c = ArashiTheme.colors
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(
            icon = Icons.Outlined.Shuffle,
            tint = if (shuffle) accent else c.text.copy(alpha = 0.45f),
            desc = "随机播放",
            size = 38.dp,
            iconSize = 21.dp,
            onClick = onShuffle,
        )
        RoundIconButton(
            icon = Icons.Outlined.SkipPrevious,
            tint = c.text.copy(alpha = 0.92f),
            desc = "上一首",
            size = 52.dp,
            iconSize = 32.dp,
            onClick = onPrevious,
        )
        PlayPauseButton(isPlaying = isPlaying, onClick = onToggle)
        RoundIconButton(
            icon = Icons.Outlined.SkipNext,
            tint = c.text.copy(alpha = 0.92f),
            desc = "下一首",
            size = 52.dp,
            iconSize = 32.dp,
            onClick = onNext,
        )
        RoundIconButton(
            icon = if (repeatMode == 2) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat,
            tint = if (repeatMode != 0) accent else c.text.copy(alpha = 0.45f),
            desc = "循环模式",
            size = 38.dp,
            iconSize = 21.dp,
            onClick = onRepeat,
        )
    }
}

@Composable
private fun PlayPauseButton(isPlaying: Boolean, onClick: () -> Unit) {
    val c = ArashiTheme.colors
    Box(
        Modifier
            .size(58.dp)
            .shadow(14.dp, CircleShape)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color.White, Color.White.copy(alpha = 0.86f))))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "暂停" else "播放",
            tint = c.playerBg,
            modifier = Modifier.size(if (isPlaying) 30.dp else 34.dp)
        )
    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    tint: Color,
    desc: String?,
    onClick: () -> Unit,
    size: Dp = 34.dp,
    iconSize: Dp = 20.dp,
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/* ============================ 底部工具栏 ============================ */

@Composable
private fun ToolRow(
    speed: Float,
    lyricsOn: Boolean,
    onSpeed: () -> Unit,
    onLyrics: () -> Unit,
    onVolume: () -> Unit,
) {
    val c = ArashiTheme.colors
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolItem(
            icon = Icons.Outlined.Speed,
            label = speedLabel(speed),
            tint = c.text.copy(alpha = 0.7f),
            onClick = onSpeed,
        )
        ToolItem(
            icon = Icons.Outlined.Lyrics,
            label = if (lyricsOn) "封面" else "歌词",
            tint = if (lyricsOn) accent else c.text.copy(alpha = 0.7f),
            onClick = onLyrics,
        )
        ToolItem(
            icon = Icons.Outlined.VolumeUp,
            label = "音量",
            tint = c.text.copy(alpha = 0.7f),
            onClick = onVolume,
        )
    }
}

@Composable
private fun ToolItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .clip(ArashiShape.md)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(21.dp))
        Spacer(Modifier.height(5.dp))
        Text(label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

private fun speedLabel(speed: Float): String {
    val v = (speed * 100).roundToInt()
    return when {
        v % 100 == 0 -> "${v / 100}x"
        v % 10 == 0 -> "${v / 100}.${(v % 100) / 10}x"
        else -> "${v / 100}.${v % 100}x"
    }
}

/* ============================ 无播放内容 ============================ */

@Composable
private fun NothingPlaying(onClose: () -> Unit) {
    val c = ArashiTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.MusicNote,
            contentDescription = null,
            tint = c.text.copy(alpha = 0.35f),
            modifier = Modifier.size(44.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "当前没有正在播放的音乐",
            color = c.text.copy(alpha = 0.8f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(18.dp))
        Box(
            Modifier
                .clip(ArashiShape.pill)
                .background(c.cardPressed)
                .clickable(onClick = onClose)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text("返回", color = c.text, fontSize = 14.sp)
        }
    }
}

/* ============================ 歌词面板 ============================ */

@Composable
private fun LyricsPanel(
    lyrics: List<LyricLine>,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    onRematch: () -> Unit,
) {
    val c = ArashiTheme.colors

    if (lyrics.isEmpty()) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.Lyrics,
                contentDescription = null,
                tint = c.text.copy(alpha = 0.3f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "暂无歌词，点击右上角重新匹配",
                color = c.text.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .clip(ArashiShape.pill)
                    .background(c.cardPressed)
                    .clickable(onClick = onRematch)
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Text("重新匹配", color = c.text, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            }
        }
        return
    }

    val listState = rememberLazyListState()
    val activeIndex = lyrics.indexOfLast { it.timeMs <= positionMs + 120L }
    val centerOffset = with(LocalDensity.current) { 118.dp.roundToPx() }

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) listState.animateScrollToItem(activeIndex, -centerOffset)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 112.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(lyrics, key = { index, line -> "${line.timeMs}-$index" }) { index, line ->
            val active = index == activeIndex
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSeek(line.timeMs) }
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    line.text.ifBlank { "♪" },
                    color = c.text.copy(alpha = if (active) 1f else 0.45f),
                    fontSize = if (active) 20.sp else 16.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
                if (!line.translation.isNullOrBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        line.translation,
                        color = c.text.copy(alpha = if (active) 0.62f else 0.3f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/* ============================ 底部弹层 ============================ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSheetHost(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = ArashiTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.card,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            content = content,
        )
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 10.dp),
        color = ArashiTheme.colors.text,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SpeedSheet(current: Float, onPick: (Float) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    SheetTitle("播放速度")
    SpeedOptions.forEach { s ->
        SettingsRow(
            title = speedLabel(s),
            onClick = { onPick(s) },
            trailing = {
                if (kotlin.math.abs(s - current) < 0.001f) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(19.dp)
                    )
                }
            },
        )
        DividerLine(startIndent = 16.dp)
    }
}

@Composable
private fun VolumeSheet(value: Float, onValue: (Float) -> Unit) {
    val c = ArashiTheme.colors
    var v by remember { mutableStateOf(value) }
    SheetTitle("音量")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.VolumeUp,
            contentDescription = null,
            tint = c.text.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            ArashiProgressSlider(
                fraction = v,
                onDragStart = {},
                onDrag = { v = it; onValue(it) },
                onDragEnd = {},
                onTap = { v = it; onValue(it) },
                bubble = { "${(it * 100).roundToInt()}%" },
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "${(v * 100).roundToInt()}%",
            color = c.text.copy(alpha = 0.6f),
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(38.dp)
        )
    }
}

@Composable
private fun MoreSheet(
    lyricsOn: Boolean,
    onToggleLyrics: () -> Unit,
    onRematch: () -> Unit,
) {
    SheetTitle("更多")
    SettingsRow(
        title = if (lyricsOn) "查看封面" else "查看歌词",
        icon = Icons.Outlined.Lyrics,
        onClick = onToggleLyrics,
    )
    DividerLine(startIndent = 58.dp)
    SettingsRow(
        title = "重新匹配封面与歌词",
        subtitle = "从网络补全并缓存这首歌的元数据",
        icon = Icons.Outlined.Refresh,
        onClick = onRematch,
    )
}

/* ============================ 封面主色渐变 ============================ */

/** 没有封面时，用曲目 id 推出一组稳定的主色渐变（深色，配合沉浸背景） */
private fun seedBrush(seed: Int): Brush {
    val hue = ((seed * 47) % 360 + 360) % 360
    return Brush.linearGradient(
        listOf(
            Color.hsl(hue.toFloat(), 0.52f, 0.36f),
            Color.hsl(((hue + 42) % 360).toFloat(), 0.58f, 0.22f),
        )
    )
}

