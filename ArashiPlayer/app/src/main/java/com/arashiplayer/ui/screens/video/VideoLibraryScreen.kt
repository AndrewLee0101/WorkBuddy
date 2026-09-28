package com.arashiplayer.ui.screens.video

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.arashiplayer.ArashiApp
import com.arashiplayer.data.model.MediaFolder
import com.arashiplayer.data.model.VideoItem
import com.arashiplayer.ui.components.ArashiCard
import com.arashiplayer.ui.components.EmptyState
import com.arashiplayer.ui.components.MetaChip
import com.arashiplayer.ui.components.SettingsRow
import com.arashiplayer.ui.navigation.Routes
import com.arashiplayer.ui.theme.ArashiBlue
import com.arashiplayer.ui.theme.ArashiShape
import com.arashiplayer.ui.theme.ArashiTheme
import com.arashiplayer.util.fileNameOf
import com.arashiplayer.util.formatDate
import com.arashiplayer.util.formatDuration
import com.arashiplayer.util.formatSize
import kotlinx.coroutines.launch

/* ============================ 排序 ============================ */

private enum class VideoSort(val label: String) {
    RECENT("最近添加"),
    NAME("名称"),
    SIZE("大小"),
    DURATION("时长"),
}

private fun List<VideoItem>.sortedFor(sort: VideoSort): List<VideoItem> = when (sort) {
    VideoSort.RECENT -> sortedByDescending { it.dateAdded }
    VideoSort.NAME -> sortedBy { it.title.lowercase() }
    VideoSort.SIZE -> sortedByDescending { it.sizeBytes }
    VideoSort.DURATION -> sortedByDescending { it.durationMs }
}

/* ============================ 视频库 ============================ */

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VideoLibraryScreen(navController: NavHostController) {
    val context = LocalContext.current
    val app = remember { ArashiApp.of(context) }
    val scope = rememberCoroutineScope()
    val c = ArashiTheme.colors

    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var grid by remember { mutableStateOf(true) }
    var sort by remember { mutableStateOf(VideoSort.RECENT) }
    var bucket by remember { mutableStateOf<Long?>(null) }
    var sortMenu by remember { mutableStateOf(false) }

    // 长按菜单 / 详情弹窗
    var actionItem by remember { mutableStateOf<VideoItem?>(null) }
    var detailItem by remember { mutableStateOf<VideoItem?>(null) }

    LaunchedEffect(Unit) {
        videos = app.mediaRepo.loadVideos()
        loading = false
    }

    val folders: List<MediaFolder> = remember(videos) { app.mediaRepo.videoFolders(videos) }

    val shown: List<VideoItem> = remember(videos, bucket, sort, query) {
        videos
            .filter { bucket == null || it.bucketId == bucket }
            .filter {
                query.isBlank() ||
                    it.title.contains(query, true) ||
                    fileNameOf(it.path).contains(query, true) ||
                    it.folderName.contains(query, true)
            }
            .sortedFor(sort)
    }

    val play: (VideoItem) -> Unit = { item ->
        navController.navigate(Routes.videoPlayer(item.uri, item.title))
    }

    Box(Modifier.fillMaxSize().background(c.background)) {
        if (searching) {
            SearchPane(
                query = query,
                count = shown.size,
                onQueryChange = { query = it },
                onClose = { searching = false; query = "" },
            ) {
                VideoResultList(
                    items = shown,
                    query = query,
                    onClick = play,
                    onLongClick = { actionItem = it },
                )
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                LibraryHeader(
                    total = videos.size,
                    folderCount = folders.size,
                    grid = grid,
                    onSearch = { searching = true },
                    onToggleGrid = { grid = !grid },
                    onSortClick = { sortMenu = true },
                )

                Box {
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        VideoSort.entries.forEach { s ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        s.label,
                                        color = if (s == sort) ArashiBlue else c.text,
                                        fontSize = 14.sp,
                                    )
                                },
                                onClick = { sort = s; sortMenu = false },
                            )
                        }
                    }
                }

                FolderChips(
                    folders = folders,
                    selected = bucket,
                    onSelect = { bucket = it },
                )

                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("正在扫描本地视频…", color = c.textTertiary, fontSize = 13.sp)
                    }

                    shown.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        EmptyState(
                            icon = Icons.Outlined.VideoLibrary,
                            title = "还没找到视频",
                            desc = "把视频拷进手机，或检查「媒体权限」是否已开启",
                        )
                    }

                    grid -> LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(shown, key = { it.uri }) { item ->
                            VideoGridCard(
                                item = item,
                                onClick = { play(item) },
                                onLongClick = { actionItem = item },
                            )
                        }
                    }

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(shown, key = { it.uri }) { item ->
                            VideoListRow(
                                item = item,
                                onClick = { play(item) },
                                onLongClick = { actionItem = item },
                            )
                        }
                    }
                }
            }
        }

        /* ---- 长按操作面板 ---- */
        val sheetItem = actionItem
        if (sheetItem != null) {
            ModalBottomSheet(
                onDismissRequest = { actionItem = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = c.card,
            ) {
                Column(Modifier.padding(bottom = 18.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VideoThumb(sheetItem, Modifier.width(96.dp).aspectRatio(16f / 9f).clip(ArashiShape.sm))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                sheetItem.title,
                                color = c.text,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${sheetItem.resolution.ifBlank { "未知" }} · ${formatSize(sheetItem.sizeBytes)}",
                                color = c.textTertiary,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    SettingsRow(
                        title = "播放",
                        icon = Icons.Filled.PlayArrow,
                        iconTint = ArashiBlue,
                        onClick = {
                            actionItem = null
                            play(sheetItem)
                        },
                    )
                    SettingsRow(
                        title = "添加到加密空间",
                        subtitle = "原地加密，不额外占用空间",
                        icon = Icons.Outlined.Lock,
                        onClick = {
                            actionItem = null
                            scope.launch {
                                val r = app.vaultRepo.hide(listOf(sheetItem.path))
                                Toast.makeText(
                                    context,
                                    if (r.added > 0) "已加入加密空间" else "加入失败：文件不可写或已被移动",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                videos = app.mediaRepo.loadVideos()
                            }
                        },
                    )
                    SettingsRow(
                        title = "详情",
                        icon = Icons.Outlined.Info,
                        onClick = {
                            actionItem = null
                            detailItem = sheetItem
                        },
                    )
                }
            }
        }

        /* ---- 详情弹窗 ---- */
        val detail = detailItem
        if (detail != null) {
            AlertDialog(
                onDismissRequest = { detailItem = null },
                confirmButton = {
                    TextButton(onClick = { detailItem = null }) { Text("好", color = ArashiBlue) }
                },
                title = { Text(detail.title, color = c.text, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailLine("分辨率", detail.resolution.ifBlank { "未知" })
                        DetailLine("时长", formatDuration(detail.durationMs))
                        DetailLine("大小", formatSize(detail.sizeBytes))
                        DetailLine("目录", detail.folderName.ifBlank { "未知" })
                        DetailLine("修改时间", formatDate(detail.dateModified))
                        DetailLine("路径", detail.path)
                    }
                },
                containerColor = c.card,
            )
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    val c = ArashiTheme.colors
    Row {
        Text(label, color = c.textTertiary, fontSize = 13.sp, modifier = Modifier.width(72.dp))
        Text(value, color = c.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

/* ============================ 头部 ============================ */

@Composable
private fun LibraryHeader(
    total: Int,
    folderCount: Int,
    grid: Boolean,
    onSearch: () -> Unit,
    onToggleGrid: () -> Unit,
    onSortClick: () -> Unit,
) {
    val c = ArashiTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 22.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("视频库", color = c.text, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
            Spacer(Modifier.height(2.dp))
            Text(
                "共 $total 个视频 · $folderCount 个文件夹",
                color = c.textTertiary,
                fontSize = 12.5.sp,
            )
        }
        IconButton(onClick = onSearch) {
            Icon(Icons.Outlined.Search, "搜索", tint = c.text, modifier = Modifier.size(22.dp))
        }
        IconButton(onClick = onSortClick) {
            Icon(Icons.Outlined.Sort, "排序", tint = c.text, modifier = Modifier.size(22.dp))
        }
        IconButton(onClick = onToggleGrid) {
            Icon(
                if (grid) Icons.Outlined.List else Icons.Outlined.GridView,
                if (grid) "列表视图" else "网格视图",
                tint = c.text,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/* ============================ 文件夹筛选 ============================ */

@Composable
private fun FolderChips(
    folders: List<MediaFolder>,
    selected: Long?,
    onSelect: (Long?) -> Unit,
) {
    if (folders.isEmpty()) return
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterPill(
                label = "全部",
                selected = selected == null,
                onClick = { onSelect(null) },
            )
        }
        items(folders, key = { it.bucketId }) { folder ->
            FilterPill(
                label = "${folder.name} ${folder.count}",
                selected = selected == folder.bucketId,
                onClick = { onSelect(folder.bucketId) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = ArashiTheme.colors
    Box(
        modifier = Modifier
            .clip(ArashiShape.pill)
            .background(if (selected) ArashiBlue else c.card)
            .border(0.5.dp, if (selected) Color.Transparent else c.glassStroke, ArashiShape.pill)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            color = if (selected) Color.White else c.textSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

/* ============================ 卡片 / 行 ============================ */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoGridCard(item: VideoItem, onClick: () -> Unit, onLongClick: () -> Unit) {
    val c = ArashiTheme.colors
    ArashiCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = ArashiShape.md,
    ) {
        Column {
            VideoThumb(item, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
                Text(
                    item.title,
                    color = c.text,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.resolution.isNotBlank()) {
                        MetaChip(item.resolution)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        formatSize(item.sizeBytes),
                        color = c.textTertiary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoListRow(item: VideoItem, onClick: () -> Unit, onLongClick: () -> Unit) {
    val c = ArashiTheme.colors
    ArashiCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = ArashiShape.md,
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            VideoThumb(item, Modifier.width(124.dp).aspectRatio(16f / 9f).clip(ArashiShape.sm))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    color = c.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.resolution.isNotBlank()) {
                        MetaChip(item.resolution)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        "${formatSize(item.sizeBytes)} · ${item.folderName.ifBlank { "未知目录" }}",
                        color = c.textTertiary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** 16:9 缩略图 + 右下角时长胶囊 */
@Composable
private fun VideoThumb(item: VideoItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val request = remember(item.uri) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .decoderFactory(VideoFrameDecoder.Factory())
            .crossfade(true)
            .build()
    }
    Box(modifier.background(Color.Black)) {
        AsyncImage(
            model = request,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        if (item.durationMs > 0) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .clip(ArashiShape.xs)
                    .background(Color(0xCC000000))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Text(
                    formatDuration(item.durationMs),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/* ============================ 全屏搜索 ============================ */

@Composable
private fun SearchPane(
    query: String,
    count: Int,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    results: @Composable () -> Unit,
) {
    val c = ArashiTheme.colors
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, "关闭搜索", tint = c.text, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(4.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(ArashiShape.pill)
                    .background(c.card)
                    .border(0.5.dp, c.glassStroke, ArashiShape.pill)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, null, tint = c.textTertiary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("搜索视频名称", color = c.textTertiary, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(color = c.text, fontSize = 14.sp),
                        cursorBrush = SolidColor(ArashiBlue),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Text(
            if (query.isBlank()) "输入关键词，按文件名实时过滤" else "找到 $count 个结果",
            color = c.textTertiary,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 20.dp, bottom = 6.dp),
        )
        Box(Modifier.fillMaxSize()) { results() }
    }
}

@Composable
private fun VideoResultList(
    items: List<VideoItem>,
    query: String,
    onClick: (VideoItem) -> Unit,
    onLongClick: (VideoItem) -> Unit,
) {
    val c = ArashiTheme.colors
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            EmptyState(
                icon = Icons.Outlined.Search,
                title = if (query.isBlank()) "输入关键词开始搜索" else "没有匹配的视频",
                desc = if (query.isBlank()) null else "试试「${query.take(8)}」的其他写法",
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items, key = { it.uri }) { item ->
            VideoListRow(item = item, onClick = { onClick(item) }, onLongClick = { onLongClick(item) })
        }
    }
}
