package com.arashiplayer.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arashiplayer.data.model.SubtitleStyle
import com.arashiplayer.ui.components.GradientButton
import com.arashiplayer.ui.theme.ArashiBlue
import com.arashiplayer.ui.theme.ArashiShape
import com.arashiplayer.ui.theme.ArashiTheme
import kotlin.math.roundToInt

/* ============================ 调色板 ============================ */

private val SubtitlePalette: List<Pair<String, Int>> = listOf(
    "白" to 0xFFFFFFFF.toInt(),
    "浅黄" to 0xFFFFF3B0.toInt(),
    "金黄" to 0xFFFFE066.toInt(),
    "橙" to 0xFFFFA53D.toInt(),
    "粉" to 0xFFFF8FB1.toInt(),
    "紫" to 0xFFB39DFF.toInt(),
    "青" to 0xFF5EE0E8.toInt(),
    "绿" to 0xFF7BE38B.toInt(),
    "蓝" to 0xFF7FB4FF.toInt(),
    "红" to 0xFFFF6B6B.toInt(),
    "浅灰" to 0xFFD6D9E0.toInt(),
    "黑" to 0xFF000000.toInt(),
)

/* ============================ 字幕设置面板 ============================ */

/**
 * 字幕样式面板。
 *
 * 面板本身不做持久化，任何改动都通过 [onStyleChange] 抛给调用方，
 * 由播放页写回 SettingsStore 并实时套用到 PlayerView 的 SubtitleView 上，
 * 因此拖动滑杆时画面里的字幕会立刻变化（所见即所得）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSettingsSheet(
    style: SubtitleStyle,
    onStyleChange: (SubtitleStyle) -> Unit,
    onPickSubtitleFile: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = ArashiTheme.colors
    var customColorOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = c.card,
        shape = ArashiShape.xl,
        scrimColor = Color(0x99000000),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 22.dp),
        ) {
            Text(
                "字幕设置",
                color = c.text,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "拖动即时生效，可边看边调",
                color = c.textTertiary,
                fontSize = 12.sp,
            )

            /* ---------------- 实时预览 ---------------- */
            Spacer(Modifier.height(14.dp))
            SubtitlePreview(style)

            /* ---------------- 预设 ---------------- */
            Spacer(Modifier.height(18.dp))
            SheetLabel("样式预设")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SubtitleStyle.Presets.forEach { (name, preset) ->
                    PresetPill(
                        label = name,
                        selected = style.withoutOffset() == preset.withoutOffset(),
                        onClick = {
                            // 套用预设但保留用户当前的时间轴偏移
                            onStyleChange(preset.copy(timeOffsetMs = style.timeOffsetMs))
                        },
                    )
                }
            }

            /* ---------------- 文字颜色 ---------------- */
            Spacer(Modifier.height(18.dp))
            SheetLabel("文字颜色")
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SubtitlePalette.forEach { (name, argb) ->
                    ColorDot(
                        argb = argb,
                        label = name,
                        selected = (style.textColor and 0x00FFFFFF) == (argb and 0x00FFFFFF),
                        onClick = { onStyleChange(style.copy(textColor = argb)) },
                    )
                }
                ColorDot(
                    argb = style.textColor,
                    label = "自定义",
                    selected = customColorOpen,
                    custom = true,
                    onClick = { customColorOpen = !customColorOpen },
                )
            }

            if (customColorOpen) {
                Spacer(Modifier.height(6.dp))
                val r = ((style.textColor shr 16) and 0xFF) / 255f
                val g = ((style.textColor shr 8) and 0xFF) / 255f
                val b = (style.textColor and 0xFF) / 255f
                RgbSlider("R", r, Color(0xFFFF6B6B)) { nr ->
                    onStyleChange(style.copy(textColor = argbOf(nr, g, b)))
                }
                RgbSlider("G", g, Color(0xFF7BE38B)) { ng ->
                    onStyleChange(style.copy(textColor = argbOf(r, ng, b)))
                }
                RgbSlider("B", b, Color(0xFF7FB4FF)) { nb ->
                    onStyleChange(style.copy(textColor = argbOf(r, g, nb)))
                }
            }

            /* ---------------- 透明度 ---------------- */
            Spacer(Modifier.height(16.dp))
            ValueSlider(
                title = "文字透明度",
                valueText = "${(style.textOpacity * 100).roundToInt()}%",
                value = style.textOpacity,
                range = 0.2f..1f,
                steps = 15,
                onChange = { onStyleChange(style.copy(textOpacity = it)) },
            )

            /* ---------------- 位置 ---------------- */
            ValueSlider(
                title = "字幕位置",
                valueText = positionLabel(style.bottomPaddingFraction),
                value = style.bottomPaddingFraction,
                range = 0f..0.45f,
                steps = 17,
                onChange = { onStyleChange(style.copy(bottomPaddingFraction = it)) },
            )

            /* ---------------- 大小 ---------------- */
            ValueSlider(
                title = "字体大小",
                valueText = "${style.textSizeSp.roundToInt()}sp",
                value = style.textSizeSp,
                range = 12f..40f,
                steps = 27,
                onChange = { onStyleChange(style.copy(textSizeSp = it)) },
            )

            /* ---------------- 加粗 ---------------- */
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("加粗", color = c.text, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                    Text("粗体字更抗画面干扰", color = c.textTertiary, fontSize = 11.5.sp)
                }
                Switch(
                    checked = style.bold,
                    onCheckedChange = { onStyleChange(style.copy(bold = it)) },
                    colors = SwitchDefaults.colors(checkedTrackColor = ArashiBlue),
                )
            }

            /* ---------------- 边缘样式 ---------------- */
            Spacer(Modifier.height(10.dp))
            SheetLabel("边缘样式")
            Spacer(Modifier.height(8.dp))
            SegmentedRow(
                options = listOf("无" to 0, "描边" to 1, "投影" to 2, "背景框" to 3),
                selected = style.edgeType,
                onSelect = { type ->
                    onStyleChange(
                        style.copy(
                            edgeType = type,
                            // 「背景框」需要一个可看见的底，其它样式默认不加底条
                            backgroundOpacity = when {
                                type == 3 && style.backgroundOpacity <= 0f -> 0.55f
                                type != 3 -> 0f
                                else -> style.backgroundOpacity
                            },
                        ),
                    )
                },
            )

            /* ---------------- 底条透明度 ---------------- */
            Spacer(Modifier.height(12.dp))
            ValueSlider(
                title = "背景条透明度",
                valueText = if (style.backgroundOpacity <= 0f) "关闭"
                else "${(style.backgroundOpacity * 100).roundToInt()}%",
                value = style.backgroundOpacity,
                range = 0f..1f,
                steps = 19,
                onChange = { onStyleChange(style.copy(backgroundOpacity = it)) },
            )

            /* ---------------- 时间轴偏移 ---------------- */
            ValueSlider(
                title = "时间轴偏移",
                valueText = offsetLabel(style.timeOffsetMs),
                value = style.timeOffsetMs / 1000f,
                range = -5f..5f,
                steps = 19,
                onChange = { onStyleChange(style.copy(timeOffsetMs = (it * 1000f).toLong())) },
            )

            /* ---------------- 操作 ---------------- */
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GradientButton(
                    text = "添加外挂字幕文件",
                    icon = Icons.Outlined.Add,
                    modifier = Modifier.weight(1f),
                    onClick = onPickSubtitleFile,
                )
                Spacer(Modifier.width(12.dp))
                Row(
                    modifier = Modifier
                        .clip(ArashiShape.pill)
                        .background(c.cardPressed)
                        .clickable { onStyleChange(SubtitleStyle.Default) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Refresh, null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("恢复默认", color = c.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/* ============================ 预览 ============================ */

/**
 * 16:9 迷你预览框：字号、颜色、透明度、位置、边缘样式都按真实比例渲染，
 * 用户调完不必退回播放页确认。
 */
@Composable
private fun SubtitlePreview(style: SubtitleStyle) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(ArashiShape.md)
            .background(Color(0xFF101218))
            .border(0.5.dp, Color(0x33FFFFFF), ArashiShape.md),
    ) {
        val boxHeight = maxHeight
        val bottomPadding = boxHeight * style.bottomPaddingFraction

        val textColor = Color(style.withAlpha(style.textOpacity))
        val edgeColor = Color(style.edgeColor)

        // edgeType: 1 描边 / 2 投影 / 3 背景框，与 SubtitleStyle 的约定一致
        val shadow = when (style.edgeType) {
            1 -> Shadow(edgeColor, Offset(1.4f, 1.4f), 0f)
            2 -> Shadow(edgeColor, Offset(0f, 2.2f), 6f)
            else -> null
        }
        val textStyle = TextStyle(
            color = textColor,
            fontSize = style.textSizeSp.sp,
            fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            shadow = shadow,
        )

        val showBar = style.backgroundOpacity > 0f || style.edgeType == 3
        val barAlpha = if (style.backgroundOpacity > 0f) style.backgroundOpacity else 0.55f

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding, start = 8.dp, end = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(ArashiShape.xs)
                    .background(if (showBar) Color.Black.copy(alpha = barAlpha) else Color.Transparent)
                    .padding(horizontal = if (showBar) 6.dp else 0.dp, vertical = if (showBar) 2.dp else 0.dp),
            ) {
                Text("岚播放器 · 字幕预览", style = textStyle)
            }
        }
    }
}

/* ============================ 小组件 ============================ */

@Composable
private fun SheetLabel(text: String) {
    Text(text, color = ArashiTheme.colors.textTertiary, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun PresetPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = ArashiTheme.colors
    Box(
        modifier = Modifier
            .clip(ArashiShape.pill)
            .background(if (selected) ArashiBlue else c.cardPressed)
            .clickable(onClick = onClick)
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

@Composable
private fun ColorDot(
    argb: Int,
    label: String,
    selected: Boolean,
    custom: Boolean = false,
    onClick: () -> Unit,
) {
    val c = ArashiTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(argb))
                .border(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) ArashiBlue else c.glassStroke,
                    shape = CircleShape,
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            if (custom) {
                Text("+", color = c.textSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            color = if (selected) ArashiBlue else c.textTertiary,
            fontSize = 10.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun ValueSlider(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
) {
    val c = ArashiTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(valueText, color = ArashiBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = ArashiBlue,
                activeTrackColor = ArashiBlue,
                inactiveTrackColor = c.cardPressed,
            ),
        )
    }
}

@Composable
private fun RgbSlider(channel: String, value: Float, tint: Color, onChange: (Float) -> Unit) {
    val c = ArashiTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(channel, color = c.textTertiary, fontSize = 12.sp, modifier = Modifier.width(16.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = tint,
                activeTrackColor = tint,
                inactiveTrackColor = c.cardPressed,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SegmentedRow(
    options: List<Pair<String, Int>>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val c = ArashiTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ArashiShape.sm)
            .background(c.cardPressed)
            .padding(3.dp),
    ) {
        options.forEach { (label, value) ->
            val active = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(ArashiShape.xs)
                    .background(if (active) ArashiBlue else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (active) Color.White else c.textSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

/* ============================ 工具 ============================ */

private fun argbOf(r: Float, g: Float, b: Float): Int {
    val ri = (r.coerceIn(0f, 1f) * 255).roundToInt()
    val gi = (g.coerceIn(0f, 1f) * 255).roundToInt()
    val bi = (b.coerceIn(0f, 1f) * 255).roundToInt()
    return (0xFF shl 24) or (ri shl 16) or (gi shl 8) or bi
}

/** 位置比例 → 人话，0 贴底、0.45 接近屏幕中部 */
private fun positionLabel(fraction: Float): String = when {
    fraction <= 0.02f -> "贴底"
    fraction <= 0.12f -> "偏低"
    fraction <= 0.24f -> "中间偏下"
    else -> "偏高"
}

private fun offsetLabel(ms: Long): String {
    if (ms == 0L) return "不偏移"
    val sign = if (ms > 0) "+" else "-"
    val abs = kotlin.math.abs(ms)
    return "$sign${abs / 1000}.${(abs % 1000) / 100}s"
}

/** 用于「预设是否选中」的比较：忽略时间轴偏移 */
private fun SubtitleStyle.withoutOffset(): SubtitleStyle = copy(timeOffsetMs = 0L)
