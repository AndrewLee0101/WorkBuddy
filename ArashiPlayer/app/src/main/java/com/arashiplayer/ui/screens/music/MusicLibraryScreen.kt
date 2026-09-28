package com.arashiplayer.ui.screens.music

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.arashiplayer.data.model.MediaFolder
import com.arashiplayer.data.model.MusicTrack
import com.arashiplayer.ui.components.ArashiCard
import com.arashiplayer.ui.components.CoverPlaceholder
import com.arashiplayer.ui.components.DividerLine
import com.arashiplayer.ui.components.EmptyState
import com.arashiplayer.ui.components.SectionHeader
import com.arashiplayer.ui.components.SettingsRow
import com.arashiplayer.ui.navigation.Routes
import com.arashiplayer.ui.theme.ArashiShape
import com.arashiplayer.ui.theme.ArashiTheme
import com.arashiplayer.util.formatDuration

/** 大标题完整高度 / 收缩后高度，用于滚动折叠插值 */
private val TitleExpandedHeight = 96.dp
private val TitleCollapsedHeight = 54.dp

@Composable
fun MusicLibraryScreen(navController: NavHostController) {
    val vm: MusicViewModel = viewModel()
    val state by vm.state.collectAsState()

    var tab by remember { mutableStateOf(MusicTab.SONGS) }
    var sheetTrack by remember { mutableStateOf<MusicTrack?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(tab) { listState.scrollToItem(0) }

    val collapse by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 260f).coerceIn(0f, 1f)
        }
    }

    fun play(queue: List<MusicTrack>, track: MusicTrack) {
        val index = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        vm.playQueue(queue, index)
        navController.navigate(Routes.MUSIC_PLAYER)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(ArashiTheme.colors.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            LibraryTitle(collapse = collapse)

            SegmentedControl(
                tabs = MusicTab.entries.toList(),
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Box(Modifier.fillMaxSize()) {
                when {
                    state.loading -> LoadingBlock()
                    state.tracks.isEmpty() -> EmptyBlock(vm::refresh)
                    else -> LibraryList(
                        tab = tab,
                        state = state,
                        listState = listState,
                        onPlay = { queue, track -> play(queue, track) },
                        onMore = { sheetTrack = it },
                        viewModel = vm,
                    )
                }
            }
        }
    }

    val current = sheetTrack
    if (current != null) {
        TrackActionSheet(
            track = current,
            onDismiss = { sheetTrack = null },
            onPlay = {
                play(state.tracks, current)
                sheetTrack = null
            },
            onRematch = {
                vm.rematch(current)
                sheetTrack = null
            },
        )
    }
}

/* ============================ 顶部标题 ============================ */

@Composable
private fun LibraryTitle(collapse: Float) {
    val c = ArashiTheme.colors
    val t by animateFloatAsState(targetValue = collapse, label = "titleCollapse")
    val titleSize = lerpSp(34f, 23f, t)
    val heightDp = lerpDp(TitleExpandedHeight, TitleCollapsedHeight, t)
    val compactAlpha = ((t - 0.7f) / 0.3f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            text = "音乐库",
            color = c.text,
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6f).sp,
            modifier = Modifier
                .alpha(1f - compactAlpha)
                .padding(bottom = lerpDp(16.dp, 8.dp, t))
        )
        if (compactAlpha > 0f) {
            Text(
                text = "音乐库",
                color = c.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .alpha(compactAlpha)
                    .padding(bottom = 15.dp)
            )
        }
    }
}

/* ============================ 分段控件 ============================ */

@Composable
private fun SegmentedControl(
    tabs: List<MusicTab>,
    selected: MusicTab,
    onSelect: (MusicTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = ArashiTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ArashiShape.pill)
            .background(c.cardPressed)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { t ->
            val active = t == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (active) Modifier
                            .shadow(3.dp, ArashiShape.pill)
                            .clip(ArashiShape.pill)
                            .background(c.card)
                        else Modifier
                    )
                    .clickable { onSelect(t) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = t.label,
                    color = if (active) c.text else c.textSecondary,
                    fontSize = 13.5.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

/* ============================ 加载 / 空 ============================ */

@Composable
private fun LoadingBlock() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = ArashiTheme.colors.textTertiary,
            strokeWidth = 2.dp,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun EmptyBlock(onRefresh: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            icon = Icons.Outlined.MusicNote,
            title = "还没有找到音乐",
            desc = "把歌放到手机里，岚会替你整理好",
            action = {
                Box(
                    Modifier
                        .clip(ArashiShape.pill)
                        .background(ArashiTheme.colors.cardPressed)
                        .clickable(onClick = onRefresh)
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text("重新扫描", color = ArashiTheme.colors.text, fontSize = 14.sp)
                }
            }
        )
    }
}

/* ============================ 列表分发 ============================ */

@Composable
private fun LibraryList(
    tab: MusicTab,
    state: MusicUiState,
    listState: LazyListState,
    onPlay: (List<MusicTrack>, MusicTrack) -> Unit,
    onMore: (MusicTrack) -> Unit,
    viewModel: MusicViewModel,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        when (tab) {
            MusicTab.SONGS -> {
                item {
                    SectionHeader("${state.tracks.size} 首歌曲")
                }
                items(state.tracks, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        onPlay = { onPlay(state.tracks, track) },
                        onMore = { onMore(track) },
                    )
                    DividerLine(startIndent = 88.dp)
                }
            }

            MusicTab.ARTISTS -> {
                items(state.artists, key = { it.name }) { group ->
                    ArtistSection(
                        group = group,
                        onPlay = { track -> onPlay(group.tracks, track) },
                        onMore = onMore,
                    )
                }
            }

            MusicTab.ALBUMS -> {
                items(state.albums, key = { "album-${it.albumId}-${it.name}" }) { album ->
                    AlbumSection(
                        album = album,
                        onPlay = { track -> onPlay(album.tracks, track) },
                        onMore = onMore,
                    )
                }
            }

            MusicTab.FOLDERS -> {
                items(state.folders, key = { it.bucketId }) { folder ->
                    FolderSection(
                        folder = folder,
                        tracks = viewModel.tracksInFolder(folder),
                        onPlay = { queue, track -> onPlay(queue, track) },
                        onMore = onMore,
                    )
                }
            }
        }
    }
}

/* ============================ 分组区段（歌手 / 专辑 / 文件夹） ============================ */

@Composable
private fun ArtistSection(
    group: ArtistGroup,
    onPlay: (MusicTrack) -> Unit,
    onMore: (MusicTrack) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val c = ArashiTheme.colors
    ArashiCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = ArashiShape.lg,
        onClick = { expanded = !expanded },
    ) {
        Column(Modifier.animateContentSize()) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverBox(uri = group.tracks.firstOrNull()?.embeddedCoverUri, seed = group.name.hashCode(), size = 54.dp, radius = 10.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        group.name,
                        color = c.text,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${group.tracks.size} 首 · ${group.albumCount} 张专辑",
                        color = c.textSecondary,
                        fontSize = 12.5.sp
                    )
                }
                ExpandChevron(expanded)
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    DividerLine(startIndent = 16.dp)
                    group.tracks.forEach { t ->
                        TrackRow(track = t, onPlay = { onPlay(t) }, onMore = { onMore(t) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumSection(
    album: AlbumGroup,
    onPlay: (MusicTrack) -> Unit,
    onMore: (MusicTrack) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val c = ArashiTheme.colors
    ArashiCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = ArashiShape.lg,
        onClick = { expanded = !expanded },
    ) {
        Column(Modifier.animateContentSize()) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverBox(uri = album.coverUri, seed = album.albumId.toInt(), size = 54.dp, radius = 10.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        album.name,
                        color = c.text,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${album.artist} · ${album.tracks.size} 首",
                        color = c.textSecondary,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                ExpandChevron(expanded)
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    DividerLine(startIndent = 16.dp)
                    album.tracks.forEach { t ->
                        TrackRow(track = t, onPlay = { onPlay(t) }, onMore = { onMore(t) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderSection(
    folder: MediaFolder,
    tracks: List<MusicTrack>,
    onPlay: (List<MusicTrack>, MusicTrack) -> Unit,
    onMore: (MusicTrack) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val c = ArashiTheme.colors
    SettingsRow(
        title = folder.name,
        subtitle = "${folder.count} 首 · ${folder.path}",
        icon = Icons.Outlined.Folder,
        showArrow = false,
        onClick = { expanded = !expanded },
        trailing = { ExpandChevron(expanded) },
    )
    AnimatedVisibility(visible = expanded) {
        Column {
            tracks.forEach { t ->
                TrackRow(track = t, onPlay = { onPlay(tracks, t) }, onMore = { onMore(t) })
            }
        }
    }
}

@Composable
private fun ExpandChevron(expanded: Boolean) {
    val c = ArashiTheme.colors
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, label = "chevron")
    Icon(
        Icons.Outlined.ChevronRight,
        contentDescription = null,
        tint = c.textTertiary,
        modifier = Modifier
            .size(20.dp)
            .rotate(rotation)
    )
}

/* ============================ 单曲行 ============================ */

@Composable
private fun TrackRow(
    track: MusicTrack,
    onPlay: () -> Unit,
    onMore: () -> Unit,
) {
    val c = ArashiTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverBox(uri = track.embeddedCoverUri, seed = track.id.toInt(), size = 56.dp, radius = 10.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                color = c.text,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${track.artist} · ${track.album}",
                color = c.textSecondary,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            formatDuration(track.durationMs),
            color = c.textTertiary,
            fontSize = 12.5.sp
        )
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(ArashiShape.pill)
                .clickable(onClick = onMore),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.MoreHoriz,
                contentDescription = "更多",
                tint = c.textTertiary,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

/* ============================ 封面 ============================ */

@Composable
private fun CoverBox(uri: String?, seed: Int, size: Dp, radius: Dp) {
    val shape = RoundedCornerShape(radius)
    val base = Modifier
        .size(size)
        .clip(shape)
    if (!uri.isNullOrBlank()) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = base.background(ArashiTheme.colors.cardPressed)
        )
    } else {
        CoverPlaceholder(seed = seed, modifier = base)
    }
}

/* ============================ 尺寸插值 ============================ */

private fun lerpDp(a: Dp, b: Dp, t: Float): Dp = (a.value + (b.value - a.value) * t).dp

private fun lerpSp(a: Float, b: Float, t: Float): TextUnit = (a + (b - a) * t).sp

/* ============================ 更多操作 ============================ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackActionSheet(
    track: MusicTrack,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onRematch: () -> Unit,
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
                .padding(bottom = 26.dp)
        ) {
            Row(
                Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverBox(uri = track.embeddedCoverUri, seed = track.id.toInt(), size = 52.dp, radius = 12.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title,
                        color = c.text,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${track.artist} · ${track.album}",
                        color = c.textSecondary,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            DividerLine(startIndent = 20.dp)
            SettingsRow(
                title = "立即播放",
                icon = Icons.Outlined.PlayArrow,
                onClick = onPlay,
            )
            DividerLine(startIndent = 58.dp)
            SettingsRow(
                title = "在线匹配封面与歌词",
                subtitle = "从网络补全这首歌的元数据，并记住结果",
                icon = Icons.Outlined.Refresh,
                onClick = onRematch,
            )
        }
    }
}

