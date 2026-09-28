package com.arashiplayer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arashiplayer.ui.theme.ArashiGradient
import com.arashiplayer.ui.theme.ArashiShape
import com.arashiplayer.ui.theme.ArashiTheme

/* ============================ 卡片 ============================ */

/** Apple 风分组卡片：大圆角、极淡描边、按压微缩 */
@Composable
fun ArashiCard(
    modifier: Modifier = Modifier,
    shape: Shape = ArashiShape.lg,
    color: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    val c = ArashiTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "cardScale")

    Box(
        modifier = modifier
            .scale(if (onClick != null) scale else 1f)
            .clip(shape)
            .background(color ?: c.card)
            .border(0.5.dp, c.glassStroke, shape)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                ) else Modifier
            )
            .padding(contentPadding)
    ) { content() }
}

/** 渐变主按钮 */
@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    gradient: Brush = ArashiGradient,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(ArashiShape.pill)
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

/* ============================ 列表行 ============================ */

@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
    showArrow: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val c = ArashiTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background((iconTint ?: c.textSecondary).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint ?: c.textSecondary, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = c.text, fontSize = 15.5.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = c.textTertiary,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) trailing()
        if (showArrow) {
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Outlined.ChevronRight, null,
                tint = c.textTertiary, modifier = Modifier.size(19.dp)
            )
        }
    }
}

/** 分组标题（Apple 风小写灰字） */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(start = 30.dp, top = 22.dp, bottom = 8.dp),
        color = ArashiTheme.colors.textTertiary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    )
}

/** 分组卡片容器：自动处理组内分隔线的缩进感 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ArashiCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = ArashiShape.lg,
    ) {
        Column { content() }
    }
}

/* ============================ 空状态 ============================ */

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    desc: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val c = ArashiTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(c.textTertiary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = c.textTertiary, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        if (desc != null) {
            Spacer(Modifier.height(6.dp))
            Text(desc, color = c.textTertiary, fontSize = 13.sp)
        }
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

/* ============================ 其他 ============================ */

@Composable
fun DividerLine(startIndent: Dp = 16.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = startIndent)
            .height(0.5.dp)
            .background(ArashiTheme.colors.divider)
    )
}

/** 小圆角标签，例如「1080P」「FLAC」 */
@Composable
fun MetaChip(text: String, tint: Color? = null) {
    val c = ArashiTheme.colors
    Box(
        Modifier
            .clip(ArashiShape.xs)
            .background((tint ?: c.textTertiary).copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = tint ?: c.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 用文字拼出的小红书标识：红底 + 白字，与官方图标的视觉语言一致 */
@Composable
fun XiaohongshuLogo(size: Dp = 28.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Color(0xFFFF2442)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "小红书",
                color = Color.White,
                fontSize = (size.value * 0.30f).sp,
                fontWeight = FontWeight.Bold,
                lineHeight = (size.value * 0.30f).sp
            )
        }
    }
}

/** 横向模糊色块，用来做封面占位 */
@Composable
fun CoverPlaceholder(seed: Int, modifier: Modifier = Modifier) {
    val hue = (seed * 47) % 360
    val brush = Brush.linearGradient(
        listOf(
            Color.hsl(hue.toFloat(), 0.55f, 0.62f),
            Color.hsl(((hue + 42) % 360).toFloat(), 0.60f, 0.48f),
        )
    )
    Box(modifier.clip(ArashiShape.md).background(brush))
}

@Composable
fun LabelText(text: String, modifier: Modifier = Modifier, alpha: Float = 1f) {
    Text(
        text,
        modifier = modifier.alpha(alpha),
        style = MaterialTheme.typography.labelMedium,
        color = ArashiTheme.colors.textSecondary
    )
}
