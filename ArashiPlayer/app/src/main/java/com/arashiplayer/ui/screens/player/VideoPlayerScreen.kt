package com.arashiplayer.ui.screens.player

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.Typeface
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import com.arashiplayer.ArashiApp
import com.arashiplayer.data.local.BookmarkEntity
import com.arashiplayer.data.model.AspectRatioMode
import com.arashiplayer.data.model.DecoderMode
import com.arashiplayer.data.model.Playable
import com.arashiplayer.data.model.SubtitleStyle
import com.arashiplayer.data.model.VideoItem
import com.arashiplayer.player.PlayerHolder
import com.arashiplayer.ui.theme.ArashiGradient
import com.arashiplayer.ui.theme.ArashiShape
import com.arashiplayer.util.SUBTITLE_EXTENSIONS
import com.arashiplayer.util.extensionOf
import com.arashiplayer.util.fileNameOf
import com.arashiplayer.util.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/* ============================ 常量 ============================ */

private const val SEEK_STEP_DOUBLE_TAP_MS = 10_000L
private const val SEEK_STEP_BACK_MS = 15_000L
private const val SEEK_STEP_FORWARD_MS = 30_000L
private const val BOOKMARK_INTERVAL_MS = 5_000L
private const val RESUME_MIN_POSITION_MS = 10_000L

private val SPEED_OPTIONS = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f, 4f)
private val SLEEP_OPTIONS = listOf(0, 15, 30, 60, 90)

private enum class PlayerDialog { NONE, ASPECT, DECODER, AUDIO, SLEEP }

/** 手势过程中显示在屏幕正中的实时反馈 */
private sealed interface PlayerHud {
    data class Seek(val targetMs: Long, val totalMs: Long) : PlayerHud
    data class Brightness(val value: Float) : PlayerHud
    data class Volume(val value: Float) : PlayerHud
    data class Zoom(val scale: Float) : PlayerHud
}

/** 跨手势共享的双击判定状态 */
private class TapTracker {
    var lastTime = 0L
    var lastX = 0f
}

/* ============================ 入口 ============================ */

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    navController: NavHostController,
    mediaUri: String,
    title: String,
    externalSubtitleUri: String?,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val app = remember { ArashiApp.of(context) }
    val holder: PlayerHolder = remember { app.playerHolder }
    // 切解码会重建 ExoPlayer 实例，所以这里用 state 跟随 holder.player，而不是 remember 死一个引用
    var player by remember { mutableStateOf(holder.player) }
    val scope = rememberCoroutineScope()

    val settings = remember { app.settings }
    val storedSubtitleStyle by settings.subtitleStyle.collectAsState(initial = SubtitleStyle.Default)
    val gestureEnabled by settings.gestureControl.collectAsState(initial = true)
    val savedAspect by settings.aspectRatio.collectAsState(initial = AspectRatioMode.FIT.name)
    val decoderMode by holder.decoderMode.collectAsState()

    // 字幕样式草稿：改一下立即套用到画面，落盘做 350ms 防抖，避免连续拖动时把 DataStore 写爆
    var subtitleStyle by remember { mutableStateOf(SubtitleStyle.Default) }
    var styleInitialised by remember { mutableStateOf(false) }
    LaunchedEffect(storedSubtitleStyle) {
        if (!styleInitialised) {
            subtitleStyle = storedSubtitleStyle
            styleInitialised = true
        }
    }
    LaunchedEffect(subtitleStyle, styleInitialised) {
        if (!styleInitialised) return@LaunchedEffect
        delay(350)
        settings.saveSubtitleStyle(subtitleStyle)
    }

    /* ---------------- 播放状态 ---------------- */
    var currentUri by remember { mutableStateOf(mediaUri) }
    var currentTitle by remember { mutableStateOf(title.ifBlank { fileNameOf(mediaUri) }) }
    var currentItem by remember { mutableStateOf<VideoItem?>(null) }
    var library by remember { mutableStateOf<List<VideoItem>>(emptyList()) }

    var externalSubUri by remember { mutableStateOf(externalSubtitleUri) }

    /** 导航参数里带了字幕、或用户手动选过字幕，就不再被「同目录自动匹配」覆盖 */
    var subtitlePickedByUser by remember { mutableStateOf(externalSubtitleUri != null) }

    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var bufferedMs by remember { mutableLongStateOf(0L) }
    var scrubMs by remember { mutableStateOf<Long?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var videoSizeLabel by remember { mutableStateOf("") }
    var tracks by remember { mutableStateOf<Tracks?>(null) }

    /* ---------------- 界面状态 ---------------- */
    var controlsVisible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var speed by remember { mutableFloatStateOf(1f) }
    var aspect by remember { mutableStateOf(AspectRatioMode.FIT) }
    var dialog by remember { mutableStateOf(PlayerDialog.NONE) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var speedMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var hud by remember { mutableStateOf<PlayerHud?>(null) }
    var tapBubble by remember { mutableStateOf<String?>(null) }
    var resumePosition by remember { mutableStateOf<Long?>(null) }
    var finished by remember { mutableStateOf(false) }
    var sleepMinutes by remember { mutableStateOf<Int?>(null) }
    var sleepAtEndOfEpisode by remember { mutableStateOf(false) }
    var aspectInitialised by remember { mutableStateOf(false) }

    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var subtitleView by remember { mutableStateOf<SubtitleView?>(null) }

    /* ---------------- 同目录播放队列 ---------------- */
    val playlist: List<VideoItem> = remember(library, currentItem) {
        val folder = currentItem?.folderName
        if (folder.isNullOrBlank()) emptyList()
        else library.filter { it.folderName == folder }.sortedBy { it.path }
    }
    val queueIndex = playlist.indexOfFirst { it.uri == currentUri }
    val hasNext = queueIndex in 0 until playlist.lastIndex
    val hasPrevious = queueIndex > 0

    val openEpisode: (VideoItem) -> Unit = { item ->
        finished = false
        resumePosition = null
        zoom = 1f
        currentUri = item.uri
        currentTitle = item.title
    }

    val playNextEpisode: () -> Unit = {
        if (hasNext) openEpisode(playlist[queueIndex + 1]) else finished = true
    }
    val playPreviousEpisode: () -> Unit = {
        if (hasPrevious) openEpisode(playlist[queueIndex - 1])
    }

    val nextEpisodeRef by rememberUpdatedState(playNextEpisode)
    val endedHandlerRef by rememberUpdatedState {
        if (sleepAtEndOfEpisode) {
            holder.pause()
            sleepAtEndOfEpisode = false
        } else {
            nextEpisodeRef()
        }
    }

    /* ---------------- 屏幕方向 / 系统栏 ---------------- */
    DisposableEffect(activity) {
        val window = activity?.window
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.requestedOrientation = previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            controller?.show(WindowInsetsCompat.Type.systemBars())
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            holder.pause()
        }
    }

    /* ---------------- 开播 ---------------- */
    LaunchedEffect(currentUri) {
        if (library.isEmpty()) library = app.mediaRepo.loadVideos()
        val item = library.firstOrNull { it.uri == currentUri || it.path == currentUri }
        currentItem = item

        // 字幕：用户手动挂过就沿用，否则按「同目录同名」自动匹配（没有就静默跳过）
        if (!subtitlePickedByUser) {
            externalSubUri = findSiblingSubtitle(item?.path ?: uriToPath(currentUri))
        }

        if (player.currentMediaItem?.mediaId == currentUri) {
            // 同一路媒体（旋转屏幕 / 重组），不重新 prepare，避免从头开始
            player.playWhenReady = true
        } else {
            holder.play(buildPlayable(currentUri, currentTitle, item))
        }
    }

    /* ---------------- 外挂字幕注入（不打断播放） ---------------- */
    LaunchedEffect(currentUri, externalSubUri) {
        val sub = externalSubUri ?: return@LaunchedEffect
        attachSubtitle(player, sub)
    }

    /* ---------------- 续播书签 ---------------- */
    LaunchedEffect(currentUri) {
        if (!settings.resumePlayback.first()) return@LaunchedEffect
        val key = "video:$currentUri"
        val saved = runCatching { app.database.bookmarkDao().position(key) }.getOrNull()
        if (saved != null && saved > RESUME_MIN_POSITION_MS) resumePosition = saved
    }

    /* ---------------- 每 5 秒写一次书签 ---------------- */
    LaunchedEffect(currentUri) {
        val key = "video:$currentUri"
        while (true) {
            delay(BOOKMARK_INTERVAL_MS)
            if (player.isPlaying && player.duration > 0) {
                runCatching {
                    app.database.bookmarkDao().upsert(
                        BookmarkEntity(key, player.currentPosition, player.duration, System.currentTimeMillis())
                    )
                }
            }
        }
    }

    /* ---------------- 播放器回调 ---------------- */
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) endedHandlerRef()
            }

            override fun onVideoSizeChanged(size: VideoSize) {
                if (size.width > 0 && size.height > 0) {
                    videoSizeLabel = "${size.width}×${size.height}"
                }
            }

            override fun onTracksChanged(tracksSnapshot: Tracks) {
                tracks = tracksSnapshot
            }

            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(context, "播放失败：${error.errorCodeName}", Toast.LENGTH_LONG).show()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    /* ---------------- 进度心跳 ---------------- */
    LaunchedEffect(player) {
        while (true) {
            positionMs = player.currentPosition
            val d = player.duration
            if (d > 0 && d != C.TIME_UNSET) durationMs = d
            bufferedMs = player.bufferedPosition
            delay(300)
        }
    }

    /* ---------------- 自动隐藏控制层 ---------------- */
    LaunchedEffect(controlsVisible, isPlaying, locked) {
        if (controlsVisible && isPlaying) {
            delay(4200)
            controlsVisible = false
        }
    }

    LaunchedEffect(tapBubble) {
        if (tapBubble != null) {
            delay(700)
            tapBubble = null
        }
    }

    /* ---------------- 定时关闭 ---------------- */
    LaunchedEffect(sleepMinutes) {
        val minutes = sleepMinutes ?: return@LaunchedEffect
        if (minutes <= 0) return@LaunchedEffect
        delay(minutes * 60_000L)
        holder.pause()
        sleepMinutes = null
        Toast.makeText(context, "定时关闭：已暂停播放", Toast.LENGTH_SHORT).show()
    }

    /* ---------------- 字幕样式实时套用到 SubtitleView ---------------- */
    LaunchedEffect(subtitleView, subtitleStyle) {
        subtitleView?.let { applySubtitleStyle(it, subtitleStyle) }
    }
    SubtitleCueBridge(player = player, subtitleView = subtitleView, offsetMs = subtitleStyle.timeOffsetMs)

    /* ---------------- 画面比例：只初始化一次，之后以用户本次选择为准 ---------------- */
    LaunchedEffect(savedAspect) {
        if (!aspectInitialised) {
            runCatching { AspectRatioMode.valueOf(savedAspect) }.getOrNull()?.let { aspect = it }
            aspectInitialised = true
        }
    }

    /* ---------------- 外挂字幕选择器 ---------------- */
    val pickSubtitle = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            subtitlePickedByUser = true
            externalSubUri = uri.toString()
            Toast.makeText(context, "已挂载外挂字幕", Toast.LENGTH_SHORT).show()
        }
    }
    val pickSubtitleAction: () -> Unit = { pickSubtitle.launch(arrayOf("*/*")) }

    /* ---------------- 截图 ---------------- */
    val takeScreenshot: () -> Unit = {
        val view = playerView
        if (view == null) {
            Toast.makeText(context, "画面尚未就绪", Toast.LENGTH_SHORT).show()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            captureWithPixelCopy(view) { bitmap ->
                if (bitmap == null) {
                    Toast.makeText(context, "截图失败", Toast.LENGTH_SHORT).show()
                } else {
                    scope.launch {
                        val ok = saveScreenshot(context, bitmap)
                        Toast.makeText(
                            context,
                            if (ok) "已保存到 Pictures/岚播放器" else "保存失败，请检查存储权限",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        } else {
            // API 24/25 无窗口级 PixelCopy，退回直接向视频取帧
            val path = currentItem?.path ?: uriToPath(currentUri)
            val bitmap = retrieveFrameAt(path, player.currentPosition)
            if (bitmap == null) {
                Toast.makeText(context, "当前系统版本不支持截图", Toast.LENGTH_SHORT).show()
            } else {
                scope.launch {
                    val ok = saveScreenshot(context, bitmap)
                    Toast.makeText(
                        context,
                        if (ok) "已保存到 Pictures/岚播放器" else "保存失败，请检查存储权限",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    /* ============================ 画面 ============================ */
    Box(Modifier.fillMaxSize().background(Color.Black)) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        setKeepScreenOn(true)
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        setPlayer(holder.player)
                        playerView = this
                        subtitleView = this.subtitleView
                    }
                },
                update = { view ->
                    // 切硬解/软解后 holder 会换实例，这里把画面重新绑过去
                    if (view.player !== player) view.player = player
                    view.resizeMode = aspect.resizeMode()
                    // 捏合缩放：直接作用在 PlayerView 上，SurfaceView 会跟随 View 层级一起变换
                    view.scaleX = zoom
                    view.scaleY = zoom
                },
                onRelease = { view ->
                    view.player = null
                    playerView = null
                    subtitleView = null
                },
                modifier = Modifier.matchAspect(aspect),
            )
        }

        /* ---------------- 手势层 ---------------- */
        PlayerGestureLayer(
            enabled = gestureEnabled,
            locked = locked,
            positionProvider = { player.currentPosition },
            durationProvider = { player.duration.coerceAtLeast(0L) },
            brightnessProvider = { activity.currentBrightness() },
            volumeProvider = { context.musicVolume() },
            onSingleTap = { controlsVisible = !controlsVisible },
            onDoubleTapSeek = { delta -> holder.seekBy(delta) },
            onSeekPreview = { target ->
                scrubMs = target
                hud = PlayerHud.Seek(target, durationMs)
            },
            onSeekCommit = { target ->
                holder.seekTo(target)
                scrubMs = null
            },
            onBrightness = { v -> activity.setBrightness(v) },
            onVolume = { v -> context.setMusicVolume(v) },
            onZoom = { factor -> zoom = (zoom * factor).coerceIn(1f, 3f) },
            onTransientChange = { h -> hud = h },
            onTapBubble = { text -> tapBubble = text },
            modifier = Modifier.fillMaxSize(),
        )

        /* ---------------- 控制层 ---------------- */
        if (locked) {
            AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut()) {
                Box(Modifier.fillMaxSize()) {
                    LockedBadge(
                        onUnlock = { locked = false; controlsVisible = true },
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 18.dp),
                    )
                }
            }
        } else {
            AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut()) {
                PlayerControls(
                    title = currentTitle,
                    resolution = videoSizeLabel.ifBlank { currentItem?.resolution.orEmpty() },
                    decoder = decoderMode,
                    speed = speed,
                    subtitleLabel = subtitleLabel(externalSubUri),
                    hasSubtitle = externalSubUri != null,
                    sleepMinutes = sleepMinutes,
                    isPlaying = isPlaying,
                    positionMs = scrubMs ?: positionMs,
                    durationMs = durationMs,
                    bufferedMs = bufferedMs,
                    scrubbing = scrubMs != null,
                    hasNext = hasNext,
                    hasPrevious = hasPrevious,
                    onBack = { navController.popBackStack() },
                    onTogglePlay = { holder.togglePlayPause(); controlsVisible = true },
                    onSeekDelta = { holder.seekBy(it) },
                    onPrevious = playPreviousEpisode,
                    onNext = playNextEpisode,
                    onSeek = { holder.seekTo(it) },
                    onScrub = { scrubMs = it },
                    onScrubEnd = {
                        scrubMs?.let { holder.seekTo(it) }
                        scrubMs = null
                    },
                    speedMenuOpen = speedMenu,
                    onSpeedMenuOpenChange = { speedMenu = it },
                    onSpeedPick = { s -> holder.setSpeed(s); speed = s; speedMenu = false },
                    moreMenuOpen = moreMenu,
                    onMoreMenuOpenChange = { moreMenu = it },
                    onAspectClick = { moreMenu = false; dialog = PlayerDialog.ASPECT },
                    onDecoderClick = { moreMenu = false; dialog = PlayerDialog.DECODER },
                    onAudioTrackClick = { moreMenu = false; dialog = PlayerDialog.AUDIO },
                    onSleepClick = { moreMenu = false; dialog = PlayerDialog.SLEEP },
                    onSubtitleClick = { moreMenu = false; showSubtitleSheet = true },
                    onCastClick = { Toast.makeText(context, "投屏功能开发中", Toast.LENGTH_SHORT).show() },
                    onScreenshot = takeScreenshot,
                    onLock = { locked = true; controlsVisible = false },
                    onRotate = {
                        activity?.requestedOrientation =
                            if (activity?.resources?.configuration?.orientation ==
                                android.content.res.Configuration.ORIENTATION_LANDSCAPE
                            ) {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            }
                    },
                )
            }
        }

        /* ---------------- 手势反馈 HUD ---------------- */
        hud?.let { HudOverlay(it, Modifier.fillMaxSize()) }

        /* ---------------- 双击进度气泡 ---------------- */
        tapBubble?.let { text ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(84.dp)
                        .clip(ArashiShape.pill)
                        .background(Color(0xB3000000)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        /* ---------------- 续播提示 ---------------- */
        resumePosition?.let { saved ->
            ResumeBanner(
                positionMs = saved,
                onResume = {
                    holder.seekTo(saved)
                    resumePosition = null
                },
                onRestart = {
                    holder.seekTo(0L)
                    resumePosition = null
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(bottom = 96.dp, start = 16.dp, end = 16.dp),
            )
        }

        /* ---------------- 播放完毕 ---------------- */
        if (finished) {
            FinishedOverlay(
                onReplay = {
                    finished = false
                    holder.seekTo(0L)
                    player.play()
                },
                onExit = { navController.popBackStack() },
            )
        }

        /* ---------------- 字幕面板 ---------------- */
        if (showSubtitleSheet) {
            SubtitleSettingsSheet(
                style = subtitleStyle,
                onStyleChange = { new -> subtitleStyle = new },
                onPickSubtitleFile = {
                    showSubtitleSheet = false
                    pickSubtitleAction()
                },
                onDismiss = { showSubtitleSheet = false },
            )
        }

        /* ---------------- 各种选择弹窗 ---------------- */
        when (dialog) {
            PlayerDialog.ASPECT -> ChoiceDialog(
                title = "画面比例",
                options = AspectRatioMode.entries,
                label = { it.label },
                selected = aspect,
                onPick = {
                    aspect = it
                    scope.launch { settings.setAspectRatio(it.name) }
                    dialog = PlayerDialog.NONE
                },
                onDismiss = { dialog = PlayerDialog.NONE },
            )

            PlayerDialog.DECODER -> ChoiceDialog(
                title = "解码方式",
                options = DecoderMode.entries,
                label = { if (it == DecoderMode.HW) "硬件解码（省电、流畅）" else "软件解码（兼容性更好）" },
                selected = decoderMode,
                onPick = { mode ->
                    scope.launch { settings.setDecoder(mode) }
                    // 重建解码器并保持进度；player 跟随新实例，画面与监听器都会自动重绑
                    holder.switchDecoder(mode)
                    player = holder.player
                    dialog = PlayerDialog.NONE
                },
                onDismiss = { dialog = PlayerDialog.NONE },
            )

            PlayerDialog.AUDIO -> {
                val audioGroups = tracks?.groups?.filter { it.type == C.TRACK_TYPE_AUDIO }.orEmpty()
                ChoiceDialog(
                    title = "音频轨道",
                    options = audioGroups,
                    label = { group ->
                        val count = group.length
                        (0 until count).joinToString(" / ") { i ->
                            val format = group.getTrackFormat(i)
                            val name = format.label?.toString()
                                ?: format.language?.let { "语言 $it" }
                                ?: format.sampleMimeType
                                ?: "音轨 ${i + 1}"
                            if (group.isSelected && count == 1) name else "${i + 1}. $name"
                        }
                    },
                    selected = audioGroups.firstOrNull { it.isSelected },
                    onPick = { group ->
                        val override = TrackSelectionOverride(group.mediaTrackGroup, 0)
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .setOverrideForType(override)
                            .build()
                        dialog = PlayerDialog.NONE
                    },
                    onDismiss = { dialog = PlayerDialog.NONE },
                )
            }

            PlayerDialog.SLEEP -> ChoiceDialog(
                title = "定时关闭",
                options = SLEEP_OPTIONS,
                label = { if (it == 0) "关闭定时" else "$it 分钟后暂停" },
                selected = sleepMinutes,
                onPick = {
                    sleepMinutes = it.takeIf { m -> m > 0 }
                    sleepAtEndOfEpisode = false
                    dialog = PlayerDialog.NONE
                },
                onDismiss = { dialog = PlayerDialog.NONE },
                extra = {
                    TextButton(onClick = {
                        sleepMinutes = null
                        sleepAtEndOfEpisode = true
                        dialog = PlayerDialog.NONE
                    }) {
                        Text(
                            if (sleepAtEndOfEpisode) "已设置：播完本集暂停" else "播完本集后暂停",
                            color = if (sleepAtEndOfEpisode) Color(0xFF35D6E8) else Color(0xFF4F6BFF),
                        )
                    }
                },
            )

            PlayerDialog.NONE -> Unit
        }
    }
}

/* ============================ 字幕桥接 ============================ */

/**
 * 字幕时间轴偏移。
 *
 * media3 没有公开的 offset API。这里接管 [Player.Listener.onCues]：
 * media3 的 CueGroup 是「整体替换」语义（每次投递都代表当前这一刻应该显示的全部字幕），
 * 所以把每个事件按 timeOffsetMs 平移后排队，再由 60ms 的 ticker 按播放位置取「当前应显示」
 * 的那一条喂给 SubtitleView，就得到了整条平移后的字幕轨。
 * PlayerView 自己的监听器会先按原始时间写一次，本桥接在同一个事件派发里立刻覆盖，因此不会闪。
 *
 * 说明：播放器只在播放位置到达时才投递 Cue，所以正值（字幕延后）是精确的；
 * 负值受限于管线没有「预知未来」的能力，只能表现为原速显示。
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun SubtitleCueBridge(
    player: Player,
    subtitleView: SubtitleView?,
    offsetMs: Long,
) {
    val offsetState by rememberUpdatedState(offsetMs)
    LaunchedEffect(player, subtitleView) {
        val view = subtitleView ?: return@LaunchedEffect
        // (显示时刻 us, 从该时刻起应显示的 Cue)
        val queue = ArrayDeque<Pair<Long, List<Cue>>>()
        var applied: List<Cue>? = null
        var active = false
        var lastOffset = offsetState

        fun applyIfNeeded(cues: List<Cue>) {
            if (applied !== cues) {
                view.setCues(cues)
                applied = cues
            }
        }

        fun applyDue(nowUs: Long) {
            // 队列按时间有序：丢弃已经过期的中间态，只保留「当前这一刻」生效的那一条
            while (queue.size > 1 && queue[1].first <= nowUs) queue.removeFirst()
            val head = queue.firstOrNull()
            if (head != null && head.first <= nowUs) applyIfNeeded(head.second) else applyIfNeeded(emptyList())
        }

        val listener = object : Player.Listener {
            override fun onCues(cueGroup: CueGroup) {
                val offset = offsetState
                if (offset == 0L) {
                    // 交还给 PlayerView 原生渲染
                    active = false
                    applied = null
                    queue.clear()
                    return
                }
                active = true
                queue.addLast(cueGroup.presentationTimeUs + offset * 1000L to cueGroup.cues)
                applyDue(player.currentPosition * 1000L)
            }
        }
        player.addListener(listener)
        try {
            while (true) {
                if (lastOffset != offsetState) {
                    // 用户改了偏移：旧队列按老偏移算的时间已经无意义
                    lastOffset = offsetState
                    queue.clear()
                    applied = null
                }
                if (active) applyDue(player.currentPosition * 1000L)
                delay(60)
            }
        } finally {
            player.removeListener(listener)
        }
    }
}

/** 把 [SubtitleStyle] 映射到 SubtitleView（字号 / 位置 / 颜色 / 描边 / 背景条） */
@androidx.annotation.OptIn(UnstableApi::class)
private fun applySubtitleStyle(view: SubtitleView, style: SubtitleStyle) {
    view.setBottomPaddingFraction(style.bottomPaddingFraction.coerceIn(0f, 0.5f))
    view.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * (style.textSizeSp / 20f))

    // 「背景框」占用 edgeType=3 的语义，真正渲染时用底色 + 无边线来实现
    val isBox = style.edgeType == 3
    val barAlpha = when {
        isBox -> max(style.backgroundOpacity, 0.55f)
        else -> style.backgroundOpacity
    }
    val backgroundColor = if (barAlpha > 0f) {
        android.graphics.Color.argb((barAlpha.coerceIn(0f, 1f) * 255).toInt(), 0, 0, 0)
    } else {
        android.graphics.Color.TRANSPARENT
    }
    val edgeTypeCompat = if (isBox) CaptionStyleCompat.EDGE_TYPE_NONE else style.edgeTypeCompat

    view.setStyle(
        CaptionStyleCompat(
            style.withAlpha(style.textOpacity),
            backgroundColor,
            android.graphics.Color.TRANSPARENT,
            edgeTypeCompat,
            style.edgeColor,
        )
    )
    // 加粗：开启时忽略内嵌样式并强制粗体字体
    view.setApplyEmbeddedStyles(!style.bold)
    view.setApplyEmbeddedFontSizes(!style.bold)
    view.setTypeface(if (style.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT)
}

/* ============================ 外挂字幕 ============================ */

/** 播放中追加 / 替换外挂字幕，位置保持不变 */
@androidx.annotation.OptIn(UnstableApi::class)
private suspend fun attachSubtitle(player: Player, subUri: String) {
    val uri = runCatching { Uri.parse(subUri) }.getOrNull() ?: return
    val item = waitForMediaItem(player) ?: return

    val already = item.localConfiguration?.subtitleConfigurations?.any { it.uri == uri } == true
    if (already) return

    val position = player.currentPosition
    val wasPlaying = player.isPlaying

    val config = MediaItem.SubtitleConfiguration.Builder(uri)
        .setMimeType(subtitleMimeOf(subUri))
        .setLanguage("zh")
        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
        .setLabel("外挂字幕")
        .build()

    val updated = item.buildUpon().setSubtitleConfigurations(listOf(config)).build()
    player.setMediaItem(updated, position)
    player.prepare()
    player.playWhenReady = wasPlaying
}

private suspend fun waitForMediaItem(player: Player, timeoutMs: Long = 4000L): MediaItem? {
    val start = SystemClock.elapsedRealtime()
    while (SystemClock.elapsedRealtime() - start < timeoutMs) {
        player.currentMediaItem?.let { return it }
        delay(50)
    }
    return player.currentMediaItem
}

private fun subtitleMimeOf(uri: String): String = when (extensionOf(uri.substringBefore('?'))) {
    "srt", "smi" -> MimeTypes.APPLICATION_SUBRIP
    "ass", "ssa" -> MimeTypes.TEXT_SSA
    "vtt" -> MimeTypes.TEXT_VTT
    "ttml", "dfxp", "xml" -> MimeTypes.APPLICATION_TTML
    "sub" -> MimeTypes.APPLICATION_SUBRIP
    else -> MimeTypes.APPLICATION_SUBRIP
}

/** 在视频同目录里找同名同基名的字幕文件 */
private fun findSiblingSubtitle(videoPath: String?): String? {
    val path = videoPath?.takeIf { it.isNotBlank() } ?: return null
    val file = File(path)
    val dir = file.parentFile ?: return null
    val base = file.nameWithoutExtension
    val preferred = listOf("srt", "ass", "ssa", "vtt", "smi", "sub", "ttml")
    val candidate = (dir.listFiles() ?: return null)
        .filter {
            it.isFile &&
                extensionOf(it.name) in SUBTITLE_EXTENSIONS &&
                it.nameWithoutExtension.equals(base, ignoreCase = true)
        }
        .sortedBy { preferred.indexOf(extensionOf(it.name)).let { i -> if (i < 0) 99 else i } }
        .firstOrNull()
        ?: return null
    return Uri.fromFile(candidate).toString()
}

private fun subtitleLabel(uri: String?): String =
    if (uri == null) "无字幕" else "字幕：${fileNameOf(uri).take(10)}"

/* ============================ 控制层 ============================ */

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun PlayerControls(
    title: String,
    resolution: String,
    decoder: DecoderMode,
    speed: Float,
    subtitleLabel: String,
    hasSubtitle: Boolean,
    sleepMinutes: Int?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long,
    scrubbing: Boolean,
    hasNext: Boolean,
    hasPrevious: Boolean,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeekDelta: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onScrub: (Long) -> Unit,
    onScrubEnd: () -> Unit,
    speedMenuOpen: Boolean,
    onSpeedMenuOpenChange: (Boolean) -> Unit,
    onSpeedPick: (Float) -> Unit,
    moreMenuOpen: Boolean,
    onMoreMenuOpenChange: (Boolean) -> Unit,
    onAspectClick: () -> Unit,
    onDecoderClick: () -> Unit,
    onAudioTrackClick: () -> Unit,
    onSleepClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onCastClick: () -> Unit,
    onScreenshot: () -> Unit,
    onLock: () -> Unit,
    onRotate: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {

        /* ---------- 顶部：渐变 + 标题 + 常驻状态 ---------- */
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xD9000000), Color(0x99000000), Color(0x00000000))
                    )
                )
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TapIcon(Icons.AutoMirrored.Filled.ArrowBack, "返回", onBack)
                Spacer(Modifier.width(6.dp))
                Text(
                    title,
                    color = Color.White,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TapIcon(Icons.Outlined.Cast, "投屏", onCastClick)
                Box {
                    TapIcon(Icons.Filled.MoreVert, "更多", { onMoreMenuOpenChange(true) })
                    DropdownMenu(
                        expanded = moreMenuOpen,
                        onDismissRequest = { onMoreMenuOpenChange(false) },
                    ) {
                        DropdownMenuItem(
                            text = { Text("画面比例 · ${resolution.ifBlank { "自适应" }}", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Outlined.AspectRatio, null, Modifier.size(18.dp)) },
                            onClick = onAspectClick,
                        )
                        DropdownMenuItem(
                            text = { Text("解码方式 · ${if (decoder == DecoderMode.HW) "硬解" else "软解"}", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Outlined.Memory, null, Modifier.size(18.dp)) },
                            onClick = onDecoderClick,
                        )
                        DropdownMenuItem(
                            text = { Text("字幕", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Outlined.Subtitles, null, Modifier.size(18.dp)) },
                            onClick = onSubtitleClick,
                        )
                        DropdownMenuItem(
                            text = { Text("音频轨道", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Outlined.Audiotrack, null, Modifier.size(18.dp)) },
                            onClick = onAudioTrackClick,
                        )
                        DropdownMenuItem(
                            text = { Text("定时关闭", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Outlined.Timer, null, Modifier.size(18.dp)) },
                            onClick = onSleepClick,
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (resolution.isNotBlank()) StatusChip(resolution)
                StatusChip(if (decoder == DecoderMode.HW) "硬解" else "软解", Icons.Outlined.Memory)
                StatusChip("${trimSpeed(speed)}x", Icons.Outlined.Speed)
                StatusChip(subtitleLabel, Icons.Outlined.Subtitles)
                sleepMinutes?.let { StatusChip("$it 分钟", Icons.Outlined.Timer) }
            }
        }

        /* ---------- 中间：主控 ---------- */
        Row(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(34.dp),
        ) {
            CircleIcon(
                icon = Icons.Filled.SkipPrevious,
                size = 44.dp,
                iconSize = 26.dp,
                enabled = hasPrevious,
                onClick = onPrevious,
            )
            CircleIcon(
                icon = Icons.Outlined.Replay10,
                size = 54.dp,
                iconSize = 30.dp,
                onClick = { onSeekDelta(-SEEK_STEP_BACK_MS) },
            )
            CircleIcon(
                icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                size = 74.dp,
                iconSize = 40.dp,
                onClick = onTogglePlay,
            )
            CircleIcon(
                icon = Icons.Outlined.Forward10,
                size = 54.dp,
                iconSize = 30.dp,
                onClick = { onSeekDelta(SEEK_STEP_FORWARD_MS) },
            )
            CircleIcon(
                icon = Icons.Filled.SkipNext,
                size = 44.dp,
                iconSize = 26.dp,
                enabled = hasNext,
                onClick = onNext,
            )
        }

        /* ---------- 底部：进度 + 工具 ---------- */
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x00000000), Color(0xB3000000), Color(0xE6000000))
                    )
                )
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                )
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatDuration(positionMs),
                    color = if (scrubbing) Color(0xFF35D6E8) else Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(" / ", color = Color(0x99FFFFFF), fontSize = 12.5.sp)
                Text(formatDuration(durationMs), color = Color(0xB3FFFFFF), fontSize = 12.5.sp)
                if (scrubbing) {
                    Spacer(Modifier.width(8.dp))
                    Text("松手跳转", color = Color(0xFF35D6E8), fontSize = 11.sp)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${formatDuration(bufferedMs)} 已缓存",
                    color = Color(0x80FFFFFF),
                    fontSize = 10.5.sp,
                )
            }

            PlayerProgressBar(
                positionMs = positionMs,
                durationMs = durationMs,
                bufferedMs = bufferedMs,
                scrubbing = scrubbing,
                onScrub = onScrub,
                onScrubEnd = onScrubEnd,
                onSeek = onSeek,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    CtrlButton(
                        icon = Icons.Outlined.Speed,
                        label = "${trimSpeed(speed)}x",
                        active = speed != 1f,
                        onClick = { onSpeedMenuOpenChange(true) },
                    )
                    DropdownMenu(
                        expanded = speedMenuOpen,
                        onDismissRequest = { onSpeedMenuOpenChange(false) },
                    ) {
                        SPEED_OPTIONS.forEach { s ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${trimSpeed(s)}x" + if (s == 1f) "（正常）" else "",
                                        fontSize = 14.sp,
                                        color = if (s == speed) Color(0xFF4F6BFF) else Color.Unspecified,
                                    )
                                },
                                onClick = { onSpeedPick(s) },
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                CtrlButton(
                    icon = Icons.Outlined.Subtitles,
                    label = "字幕",
                    active = hasSubtitle,
                    onClick = onSubtitleClick,
                )
                CtrlButton(Icons.Outlined.CameraAlt, "截图", onClick = onScreenshot)
                CtrlButton(Icons.Outlined.ScreenRotation, "旋转", onClick = onRotate)
                CtrlButton(
                    icon = Icons.Outlined.Fullscreen,
                    label = if (hasNext) "下一集" else "全屏",
                    onClick = { if (hasNext) onNext() else onRotate() },
                )
                CtrlButton(Icons.Outlined.Lock, "锁定", onClick = onLock)
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .clip(ArashiShape.pill)
            .background(Color(0x40FFFFFF))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Color(0xE6FFFFFF), modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = Color(0xE6FFFFFF), fontSize = 10.5.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun TapIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(ArashiShape.pill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun CircleIcon(
    icon: ImageVector,
    size: Dp,
    iconSize: Dp,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(size)
            .clip(ArashiShape.pill)
            .background(Color(0x33FFFFFF))
            .border(0.5.dp, Color(0x33FFFFFF), ArashiShape.pill)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            null,
            tint = if (enabled) Color.White else Color(0x59FFFFFF),
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun CtrlButton(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = if (active) Color(0xFF7CD4FF) else Color(0xE6FFFFFF)
    Column(
        modifier = Modifier
            .clip(ArashiShape.sm)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = tint, fontSize = 9.5.sp, maxLines = 1)
    }
}

/* ============================ 进度条 ============================ */

@Composable
private fun PlayerProgressBar(
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long,
    scrubbing: Boolean,
    onScrub: (Long) -> Unit,
    onScrubEnd: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    var width by remember { mutableIntStateOf(1) }
    val density = LocalDensity.current
    val barHeight by animateDpAsState(if (scrubbing) 6.dp else 2.5.dp, label = "progressBarHeight")
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val bufferedFraction = if (durationMs > 0) (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    val seekToX: (Float) -> Unit = { x ->
        if (durationMs > 0) {
            val f = (x / width.coerceAtLeast(1)).coerceIn(0f, 1f)
            onScrub((f * durationMs).toLong())
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(30.dp)
            .onSizeChanged { width = it.width.coerceAtLeast(1) }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> seekToX(offset.x) },
                    onDragEnd = { onScrubEnd() },
                    onDragCancel = { onScrubEnd() },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        seekToX(change.position.x)
                    },
                )
            }
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    if (durationMs > 0) {
                        val f = (offset.x / width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        onSeek((f * durationMs).toLong())
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cy = size.height / 2f
            val h = barHeight.toPx()
            val radius = CornerRadius(h / 2f, h / 2f)
            val top = cy - h / 2f

            drawRoundRect(
                color = Color(0x33FFFFFF),
                topLeft = Offset(0f, top),
                size = Size(size.width, h),
                cornerRadius = radius,
            )
            drawRoundRect(
                color = Color(0x59FFFFFF),
                topLeft = Offset(0f, top),
                size = Size(size.width * bufferedFraction, h),
                cornerRadius = radius,
            )
            drawRoundRect(
                brush = ArashiGradient,
                topLeft = Offset(0f, top),
                size = Size(size.width * fraction, h),
                cornerRadius = radius,
            )
            // 拖动时放大滑块
            drawCircle(
                color = Color.White,
                radius = with(density) { (if (scrubbing) 8.dp else 5.dp).toPx() },
                center = Offset(size.width * fraction, cy),
            )
        }

        if (scrubbing) {
            val bubbleOffset = with(density) {
                IntOffset(
                    x = (width * fraction).roundToInt() - 44.dp.roundToPx(),
                    y = -34.dp.roundToPx(),
                )
            }
            Box(
                Modifier
                    .offset { bubbleOffset }
                    .clip(ArashiShape.pill)
                    .background(Color(0xE6000000))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    "${formatDuration(positionMs)} / ${formatDuration(durationMs)}",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/* ============================ 手势 ============================ */

private const val MODE_NONE = 0
private const val MODE_BRIGHTNESS = 1
private const val MODE_VOLUME = 2
private const val MODE_SEEK = 3
private const val MODE_ZOOM = 4

/**
 * 播放器手势：
 *  - 单击 → 显隐控制层
 *  - 双击左/右半屏 → ∓10s，并弹出圆形反馈气泡
 *  - 左侧竖拖 → 亮度；右侧竖拖 → 音量（屏幕中央竖条 + 百分比）
 *  - 横拖 → 进度微调（HUD 显示「目标时间 / 总时长」）
 *  - 双指捏合 → 画面缩放
 *
 * 用单一 [awaitEachGesture] 循环统一裁决，避免多个手势检测器互相抢事件。
 */
@Composable
private fun PlayerGestureLayer(
    enabled: Boolean,
    locked: Boolean,
    positionProvider: () -> Long,
    durationProvider: () -> Long,
    brightnessProvider: () -> Float,
    volumeProvider: () -> Float,
    onSingleTap: () -> Unit,
    onDoubleTapSeek: (Long) -> Unit,
    onSeekPreview: (Long) -> Unit,
    onSeekCommit: (Long) -> Unit,
    onBrightness: (Float) -> Unit,
    onVolume: (Float) -> Unit,
    onZoom: (Float) -> Unit,
    onTransientChange: (PlayerHud?) -> Unit,
    onTapBubble: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabledState by rememberUpdatedState(enabled)
    val lockedState by rememberUpdatedState(locked)
    val positionState by rememberUpdatedState(positionProvider)
    val durationState by rememberUpdatedState(durationProvider)
    val brightnessState by rememberUpdatedState(brightnessProvider)
    val volumeState by rememberUpdatedState(volumeProvider)
    val tapState by rememberUpdatedState(onSingleTap)
    val doubleTapState by rememberUpdatedState(onDoubleTapSeek)
    val previewState by rememberUpdatedState(onSeekPreview)
    val commitState by rememberUpdatedState(onSeekCommit)
    val brightnessApply by rememberUpdatedState(onBrightness)
    val volumeApply by rememberUpdatedState(onVolume)
    val zoomState by rememberUpdatedState(onZoom)
    val hudChange by rememberUpdatedState(onTransientChange)
    val bubbleState by rememberUpdatedState(onTapBubble)
    val tapTracker = remember { TapTracker() }

    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val startX = down.position.x
                val width = size.width.toFloat()
                val height = size.height.toFloat()
                val slop = viewConfiguration.touchSlop

                var mode = MODE_NONE
                var accumDx = 0f
                var accumDy = 0f
                var moved = false
                var maxPointers = 1
                var lastZoomScale = 1f

                val startPosition = positionState()
                val startBrightness = brightnessState()
                val startVolume = volumeState()
                var seekTarget = startPosition

                while (true) {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.isEmpty()) break
                    if (pressed.size > maxPointers) maxPointers = pressed.size

                    // 双指：缩放
                    if (pressed.size >= 2) {
                        val zoomChange = event.calculateZoom()
                        if (enabledState && !lockedState && zoomChange != 1f && zoomChange > 0f) {
                            moved = true
                            mode = MODE_ZOOM
                            lastZoomScale *= zoomChange
                            zoomState(zoomChange)
                            hudChange(PlayerHud.Zoom(lastZoomScale))
                        }
                        event.changes.forEach { if (it.pressed) it.consume() }
                        continue
                    }

                    val change = pressed.first()
                    accumDx += change.position.x - change.previousPosition.x
                    accumDy += change.position.y - change.previousPosition.y

                    if (!moved && (abs(accumDx) > slop || abs(accumDy) > slop)) {
                        moved = true
                        if (enabledState && !lockedState) {
                            mode = when {
                                abs(accumDx) > abs(accumDy) -> MODE_SEEK
                                startX < width / 2f -> MODE_BRIGHTNESS
                                else -> MODE_VOLUME
                            }
                        }
                    }

                    if (moved && enabledState && !lockedState) {
                        when (mode) {
                            MODE_SEEK -> {
                                val total = durationState()
                                if (total > 0) {
                                    val delta = (accumDx / width * total).toLong()
                                    seekTarget = (startPosition + delta).coerceIn(0L, total)
                                    previewState(seekTarget)
                                }
                            }

                            MODE_BRIGHTNESS -> {
                                val value = (startBrightness - accumDy / height).coerceIn(0.02f, 1f)
                                brightnessApply(value)
                                hudChange(PlayerHud.Brightness(value))
                            }

                            MODE_VOLUME -> {
                                val value = (startVolume - accumDy / height).coerceIn(0f, 1f)
                                volumeApply(value)
                                hudChange(PlayerHud.Volume(value))
                            }
                        }
                        change.consume()
                    }
                }

                // 手势结束：清理 HUD / 提交进度
                when (mode) {
                    MODE_SEEK -> commitState(seekTarget)
                    MODE_BRIGHTNESS, MODE_VOLUME, MODE_ZOOM -> hudChange(null)
                    else -> Unit
                }

                // 点按判定
                if (!moved && maxPointers == 1) {
                    val now = System.currentTimeMillis()
                    val isDoubleTap = now - tapTracker.lastTime < 320L &&
                        abs(startX - tapTracker.lastX) < width * 0.25f
                    if (isDoubleTap) {
                        tapTracker.lastTime = 0L
                        if (enabledState && !lockedState) {
                            val forward = startX >= width / 2f
                            if (forward) {
                                doubleTapState(SEEK_STEP_DOUBLE_TAP_MS)
                                bubbleState("10s »")
                            } else {
                                doubleTapState(-SEEK_STEP_DOUBLE_TAP_MS)
                                bubbleState("« 10s")
                            }
                        }
                    } else {
                        tapTracker.lastTime = now
                        tapTracker.lastX = startX
                        tapState()
                    }
                } else {
                    // 拖动结束后短时间内不再判定为双击
                    tapTracker.lastTime = System.currentTimeMillis()
                }
            }
        },
    )
}

/* ============================ HUD / 提示 ============================ */

@Composable
private fun HudOverlay(hud: PlayerHud, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        when (hud) {
            is PlayerHud.Seek -> HudCard {
                Text(
                    "${formatDuration(hud.targetMs)} / ${formatDuration(hud.totalMs)}",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(3.dp))
                Text("松开跳转到此处", color = Color(0x99FFFFFF), fontSize = 11.sp)
            }

            is PlayerHud.Brightness -> HudCard {
                Icon(Icons.Outlined.BrightnessMedium, null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(8.dp))
                VerticalBar(hud.value)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${(hud.value * 100).roundToInt()}%",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            is PlayerHud.Volume -> HudCard {
                Icon(
                    if (hud.value <= 0.01f) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.height(8.dp))
                VerticalBar(hud.value)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${(hud.value * 100).roundToInt()}%",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            is PlayerHud.Zoom -> HudCard {
                Icon(Icons.Outlined.ZoomIn, null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(6.dp))
                Text(
                    String.format(Locale.US, "%.1fx", hud.scale),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun HudCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .clip(ArashiShape.md)
            .background(Color(0xB3000000))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

@Composable
private fun VerticalBar(value: Float) {
    Box(
        Modifier
            .width(6.dp)
            .height(120.dp)
            .clip(ArashiShape.pill)
            .background(Color(0x4DFFFFFF)),
    ) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(value.coerceIn(0f, 1f))
                .background(ArashiGradient),
        )
    }
}

@Composable
private fun LockedBadge(onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(ArashiShape.pill)
            .background(Color(0x99000000))
            .clickable(onClick = onUnlock),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.LockOpen, "解锁", tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ResumeBanner(
    positionMs: Long,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ArashiShape.pill)
            .background(Color(0xD9000000))
            .border(0.5.dp, Color(0x33FFFFFF), ArashiShape.pill)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "上次看到 ${formatDuration(positionMs)}，是否继续？",
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            "从头播",
            color = Color(0xB3FFFFFF),
            fontSize = 13.sp,
            modifier = Modifier
                .clip(ArashiShape.pill)
                .clickable(onClick = onRestart)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
        Text(
            "继续",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(ArashiShape.pill)
                .background(ArashiGradient)
                .clickable(onClick = onResume)
                .padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun FinishedOverlay(onReplay: () -> Unit, onExit: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xD9000000))
            .clickable(onClick = onReplay),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("播放完毕", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("没有找到下一个视频", color = Color(0x99FFFFFF), fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircleIcon(Icons.Filled.PlayArrow, 54.dp, 30.dp, onClick = onReplay)
                CircleIcon(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    size = 54.dp,
                    iconSize = 26.dp,
                    onClick = onExit,
                )
            }
        }
    }
}

/* ============================ 通用弹窗 ============================ */

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    label: (T) -> String,
    selected: T?,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
    extra: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF4F6BFF)) } },
        title = { Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                options.forEach { option ->
                    val active = option == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ArashiShape.sm)
                            .clickable { onPick(option) }
                            .padding(horizontal = 8.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            label(option),
                            color = if (active) Color(0xFF4F6BFF) else Color.Unspecified,
                            fontSize = 14.5.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (active) Text("✓", color = Color(0xFF4F6BFF), fontSize = 14.sp)
                    }
                }
                extra?.invoke()
            }
        },
    )
}

/* ============================ 工具 ============================ */

private fun buildPlayable(uri: String, title: String, item: VideoItem?): Playable =
    item?.let { Playable.Video(it) } ?: Playable.Video(
        VideoItem(
            id = uri.hashCode().toLong(),
            title = title.ifBlank { fileNameOf(uri) },
            path = uriToPath(uri).orEmpty(),
            uri = uri,
            durationMs = 0L,
            sizeBytes = 0L,
            width = 0,
            height = 0,
            dateAdded = 0L,
            dateModified = 0L,
            bucketId = 0L,
            folderName = "",
        )
    )

private fun uriToPath(uri: String): String? = runCatching {
    val parsed = Uri.parse(uri)
    when (parsed.scheme) {
        "file" -> parsed.path
        null -> uri
        else -> null
    }
}.getOrNull()

private fun trimSpeed(speed: Float): String =
    if (speed == speed.toInt().toFloat()) "${speed.toInt()}" else String.format(Locale.US, "%.2f", speed)
        .trimEnd('0').trimEnd('.')

@androidx.annotation.OptIn(UnstableApi::class)
private fun AspectRatioMode.resizeMode(): Int = when (this) {
    AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    AspectRatioMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    AspectRatioMode.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    AspectRatioMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    AspectRatioMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
}

/** 16:9 / 4:3 用容器先约束出真实比例，再由 FILL 填满，避免被 RESIZE_MODE 拉伸 */
private fun Modifier.matchAspect(mode: AspectRatioMode): Modifier = when (mode) {
    AspectRatioMode.RATIO_16_9 -> fillMaxHeight().aspectRatio(16f / 9f)
    AspectRatioMode.RATIO_4_3 -> fillMaxHeight().aspectRatio(4f / 3f)
    else -> fillMaxSize()
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun Activity?.currentBrightness(): Float {
    val activity = this ?: return 0.5f
    val manual = activity.window.attributes.screenBrightness
    if (manual >= 0f) return manual.coerceIn(0.02f, 1f)
    return runCatching {
        Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
    }.getOrDefault(0.5f).coerceIn(0.02f, 1f)
}

private fun Activity?.setBrightness(value: Float) {
    val activity = this ?: return
    val attrs = activity.window.attributes
    attrs.screenBrightness = value.coerceIn(0.02f, 1f)
    activity.window.attributes = attrs
}

private fun Context.musicVolume(): Float {
    val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return 0.5f
    val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    if (maxVolume <= 0) return 0f
    return am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
}

private fun Context.setMusicVolume(fraction: Float) {
    val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
    val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    if (maxVolume <= 0) return
    am.setStreamVolume(
        AudioManager.STREAM_MUSIC,
        (fraction.coerceIn(0f, 1f) * maxVolume).roundToInt(),
        0,
    )
}

/* ============================ 截图 ============================ */

private fun captureWithPixelCopy(view: View, onResult: (Bitmap?) -> Unit) {
    val activity = view.context.findActivity()
    if (activity == null || view.width <= 0 || view.height <= 0) {
        onResult(null)
        return
    }
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    val location = IntArray(2)
    view.getLocationInWindow(location)
    val rect = Rect(
        location[0], location[1],
        location[0] + view.width, location[1] + view.height,
    )
    runCatching {
        PixelCopy.request(
            activity.window,
            rect,
            bitmap,
            { result -> onResult(if (result == PixelCopy.SUCCESS) bitmap else null) },
            Handler(Looper.getMainLooper()),
        )
    }.onFailure { onResult(null) }
}

private fun retrieveFrameAt(path: String?, positionMs: Long): Bitmap? {
    if (path.isNullOrBlank()) return null
    return runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            retriever.getFrameAtTime(positionMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } finally {
            retriever.release()
        }
    }.getOrNull()
}

private suspend fun saveScreenshot(context: Context, bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val name = "Arashi_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/岚播放器")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            } else {
                // Android 10 以下没有 RELATIVE_PATH，用 DATA 指定落盘目录
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "岚播放器",
                )
                if (!dir.exists()) dir.mkdirs()
                put(MediaStore.Images.Media.DATA, File(dir, name).absolutePath)
            }
        }
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uri = context.contentResolver.insert(collection, values) ?: return@runCatching false
        context.contentResolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        } ?: return@runCatching false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            context.contentResolver.update(uri, done, null, null)
        }
        true
    }.getOrDefault(false)
}
