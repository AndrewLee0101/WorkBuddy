package com.arashiplayer.ui.screens.vault

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arashiplayer.ui.theme.ArashiGradient
import com.arashiplayer.ui.theme.ArashiShape
import com.arashiplayer.ui.theme.ArashiTheme
import com.arashiplayer.ui.theme.ArashiViolet
import kotlinx.coroutines.delay

/* ============================================================================
 *  手势九宫格
 * ========================================================================== */

/**
 * 3×3 手势解锁盘。
 *
 * - 手指按下 → 命中判定（半径＝点距 × 0.32）→ 加入路径 → 实时画连线
 * - 松手回调形如 `"0-1-2-5-8"`
 * - 外部把 [errorTick] 自增即触发一次红色抖动（Animatable translateX）
 *
 * 设计取舍：松手后**不清空路径**，这样校验失败时红色轨迹仍然可见，
 * 下一次按下才清空重来。
 */
@Composable
fun PatternLockView(
    onPattern: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorTick: Int = 0,
    enabled: Boolean = true,
) {
    val c = ArashiTheme.colors
    val selected = remember { mutableStateListOf<Int>() }
    var cursor by remember { mutableStateOf<Offset?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var showError by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }

    LaunchedEffect(errorTick) {
        if (errorTick <= 0) return@LaunchedEffect
        showError = true
        shake.snapTo(0f)
        repeat(3) {
            shake.animateTo(16f, tween(46))
            shake.animateTo(-16f, tween(46))
        }
        shake.animateTo(0f, tween(70))
        showError = false
    }

    Box(modifier = modifier.offset { IntOffset(shake.value.toInt(), 0) }) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .onSizeChanged { canvasSize = it }
                .pointerInput(enabled) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)

                            fun hit(p: Offset): Int? {
                                val side = canvasSize.width
                                if (side <= 0) return null
                                val cell = side.toFloat() / 3f
                                val limit = cell * 0.32f
                                for (i in 0 until 9) {
                                    val center = Offset(
                                        cell * (i % 3 + 0.5f),
                                        cell * (i / 3 + 0.5f),
                                    )
                                    if ((p - center).getDistance() <= limit) return i
                                }
                                return null
                            }

                            if (!enabled) continue

                            selected.clear()
                            cursor = down.position
                            hit(down.position)?.let { if (it !in selected) selected.add(it) }

                            var moving = true
                            while (moving) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null) {
                                    moving = false
                                } else {
                                    cursor = change.position
                                    hit(change.position)?.let { if (it !in selected) selected.add(it) }
                                    if (!change.pressed) moving = false
                                    change.consume()
                                }
                            }

                            cursor = null
                            if (selected.size >= 1) onPattern(selected.joinToString("-"))
                        }
                    }
                },
        ) {
            val cell = size.width / 3f
            val centers = List(9) { i ->
                Offset(cell * (i % 3 + 0.5f), cell * (i / 3 + 0.5f))
            }
            val pts = selected.mapNotNull { centers.getOrNull(it) }
            val lineBrush: Brush =
                if (showError) Brush.linearGradient(listOf(c.danger, c.danger)) else ArashiGradient

            for (i in 0 until pts.size - 1) {
                drawLine(
                    brush = lineBrush,
                    start = pts[i],
                    end = pts[i + 1],
                    strokeWidth = cell * 0.085f,
                    cap = StrokeCap.Round,
                )
            }
            cursor?.let { cur ->
                if (pts.isNotEmpty() && enabled) {
                    drawLine(
                        brush = lineBrush,
                        start = pts.last(),
                        end = cur,
                        strokeWidth = cell * 0.085f,
                        cap = StrokeCap.Round,
                    )
                }
            }

            for (i in 0 until 9) {
                val p = centers[i]
                if (selected.contains(i)) {
                    drawCircle(
                        color = if (showError) c.danger.copy(alpha = 0.30f) else ArashiViolet.copy(alpha = 0.30f),
                        radius = cell * 0.30f,
                        center = p,
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.94f),
                        radius = cell * 0.155f,
                        center = p,
                    )
                } else {
                    drawCircle(
                        color = c.textTertiary.copy(alpha = if (enabled) 0.30f else 0.16f),
                        radius = cell * 0.075f,
                        center = p,
                    )
                }
            }
        }
    }
}

/* ============================================================================
 *  数字键盘
 * ========================================================================== */

/**
 * 3×4 数字键盘（1-9 / 空 / 0 / 退格）。
 *
 * 顶部一行小圆点表示已输入位数（实心＝已输入）。
 * 输满 [pinLength] 位后回调一次 [onPin] 并自动清空。
 */
@Composable
fun PinPadView(
    onPin: (String) -> Unit,
    pinLength: Int = 4,
    modifier: Modifier = Modifier,
) {
    var pin by remember { mutableStateOf("") }
    var completed by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(completed) {
        val value = completed ?: return@LaunchedEffect
        delay(150)
        onPin(value)
        pin = ""
        completed = null
    }

    val dots = completed?.length ?: pin.length

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(pinLength) { index ->
                val filled = index < dots
                val base = Modifier
                    .size(if (filled) 14.dp else 12.dp)
                    .clip(ArashiShape.pill)
                Box(
                    if (filled) base.background(ArashiGradient)
                    else base.border(
                        width = 1.2.dp,
                        color = ArashiTheme.colors.textTertiary.copy(alpha = 0.45f),
                        shape = ArashiShape.pill,
                    )
                )
            }
        }

        Spacer(Modifier.height(26.dp))

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "<"),
        )
        rows.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                row.forEach { label ->
                    if (label.isEmpty()) {
                        Spacer(Modifier.size(72.dp))
                    } else {
                        PinKey(label = label) {
                            if (label == "<") {
                                if (pin.isNotEmpty()) pin = pin.dropLast(1)
                            } else if (completed == null && pin.length < pinLength) {
                                val next = pin + label
                                if (next.length >= pinLength) {
                                    completed = next
                                } else {
                                    pin = next
                                }
                            }
                        }
                    }
                }
            }
            if (rowIndex != rows.lastIndex) Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun PinKey(label: String, onClick: () -> Unit) {
    val c = ArashiTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.90f else 1f, label = "pinKeyScale")

    Box(
        modifier = Modifier
            .size(72.dp)
            .scale(scale)
            .clip(ArashiShape.pill)
            .background(if (pressed) c.cardPressed else c.card)
            .border(0.5.dp, c.glassStroke, ArashiShape.pill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (label == "<") {
            Icon(
                Icons.Rounded.Backspace,
                contentDescription = "退格",
                tint = c.textSecondary,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Text(
                text = label,
                color = c.text,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/* ============================================================================
 *  通用：分段控件
 *  加密空间与设置页共用（module 内可见），避免两份重复实现
 * ========================================================================== */

@Composable
internal fun SegmentedRow(
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    val c = ArashiTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.cardPressed)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (active) c.card else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (active) c.text else c.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}
