package com.arashiplayer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/* ============================ 品牌色 ============================ */

/** 「岚」主色 —— 雾蓝紫 */
val ArashiBlue = Color(0xFF4F6BFF)
val ArashiBlueDeep = Color(0xFF2F49D1)
val ArashiViolet = Color(0xFF7C5CFF)
val ArashiCyan = Color(0xFF35D6E8)
val ArashiInk = Color(0xFF0B0D14)

/** 品牌渐变：用作风、光、进度条的视觉语言 */
val ArashiGradient = Brush.linearGradient(
    listOf(Color(0xFF5B6BFF), Color(0xFF7C5CFF), Color(0xFF35D6E8))
)
val ArashiGradientSoft = Brush.linearGradient(
    listOf(Color(0x335B6BFF), Color(0x337C5CFF), Color(0x3335D6E8))
)

/* ============================ 扩展语义色 ============================ */

/**
 * Material3 之外补充的语义色。
 * 浅色/深色各一套，组件统一通过 [ArashiTheme.colors] 取用，避免散落的硬编码。
 */
data class ArashiPalette(
    val isDark: Boolean,
    /** 页面底色 */
    val background: Color,
    /** 分组卡片底色（Apple 风「分组列表」） */
    val card: Color,
    /** 卡片上浮/按压色 */
    val cardPressed: Color,
    /** 悬浮毛玻璃层 */
    val glass: Color,
    val glassStroke: Color,
    /** 主文字 / 次文字 / 三级文字 */
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    /** 分割线 */
    val divider: Color,
    /** 播放器沉浸背景（黑） */
    val playerBg: Color,
    val danger: Color,
    val success: Color,
    val warning: Color,
)

private val LightPalette = ArashiPalette(
    isDark = false,
    background = Color(0xFFF4F5F9),
    card = Color(0xFFFFFFFF),
    cardPressed = Color(0xFFECEEF5),
    glass = Color(0xE6FFFFFF),
    glassStroke = Color(0x1A000000),
    text = Color(0xFF12141C),
    textSecondary = Color(0xFF6B7080),
    textTertiary = Color(0xFF9BA0B0),
    divider = Color(0x14000000),
    playerBg = Color(0xFF06070B),
    danger = Color(0xFFFF3B30),
    success = Color(0xFF34C759),
    warning = Color(0xFFFF9500),
)

private val DarkPalette = ArashiPalette(
    isDark = true,
    background = Color(0xFF0B0D14),
    card = Color(0xFF161923),
    cardPressed = Color(0xFF1E2230),
    glass = Color(0xB3161923),
    glassStroke = Color(0x1FFFFFFF),
    text = Color(0xFFF2F3F7),
    textSecondary = Color(0xFFA0A5B5),
    textTertiary = Color(0xFF6C7285),
    divider = Color(0x1FFFFFFF),
    playerBg = Color(0xFF04050A),
    danger = Color(0xFFFF453A),
    success = Color(0xFF30D158),
    warning = Color(0xFFFF9F0A),
)

val LocalArashiPalette = staticCompositionLocalOf { LightPalette }

/* ============================ 圆角 ============================ */

object ArashiShape {
    val xs = RoundedCornerShape(8.dp)
    val sm = RoundedCornerShape(12.dp)
    val md = RoundedCornerShape(16.dp)
    val lg = RoundedCornerShape(20.dp)
    val xl = RoundedCornerShape(26.dp)
    val xxl = RoundedCornerShape(32.dp)
    val pill = RoundedCornerShape(percent = 50)
}

/* ============================ 字体 ============================ */

private val ArashiTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 40.sp, letterSpacing = (-0.8).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.6).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 10.sp),
)

/* ============================ 主题入口 ============================ */

/**
 * @param darkTheme 是否深色。null 表示跟随系统。
 */
@Composable
fun ArashiTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val dark = darkTheme ?: isSystemInDarkTheme()
    val palette = if (dark) DarkPalette else LightPalette

    val scheme = if (dark) {
        darkColorScheme(
            primary = ArashiBlue,
            onPrimary = Color.White,
            primaryContainer = ArashiBlueDeep,
            secondary = ArashiViolet,
            tertiary = ArashiCyan,
            background = palette.background,
            onBackground = palette.text,
            surface = palette.card,
            onSurface = palette.text,
            surfaceVariant = palette.cardPressed,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.divider,
            error = palette.danger,
        )
    } else {
        lightColorScheme(
            primary = ArashiBlue,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE3E8FF),
            secondary = ArashiViolet,
            tertiary = ArashiCyan,
            background = palette.background,
            onBackground = palette.text,
            surface = palette.card,
            onSurface = palette.text,
            surfaceVariant = palette.cardPressed,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.divider,
            error = palette.danger,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalArashiPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = ArashiTypography,
            shapes = MaterialTheme.shapes.copy(
                extraSmall = ArashiShape.xs,
                small = ArashiShape.sm,
                medium = ArashiShape.md,
                large = ArashiShape.lg,
                extraLarge = ArashiShape.xl,
            ),
            content = content,
        )
    }
}

/** 语法糖：`ArashiTheme.colors.card` */
object ArashiTheme {
    val colors: ArashiPalette
        @Composable get() = LocalArashiPalette.current
}
