package com.arashiplayer.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.arashiplayer.ui.components.ArashiCard
import com.arashiplayer.ui.components.GradientButton
import com.arashiplayer.ui.components.XiaohongshuLogo
import com.arashiplayer.ui.theme.ArashiShape
import com.arashiplayer.ui.theme.ArashiTheme

/** 作者的小红书主页 */
private const val XHS_PROFILE_URL =
    "https://www.xiaohongshu.com/user/profile/65d47c96000000000401e154"

/** 联系作者：小红书 @琪琪 */
@Composable
fun AuthorScreen(navController: NavHostController) {
    val c = ArashiTheme.colors
    val context = LocalContext.current

    fun openProfile() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(XHS_PROFILE_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launched = runCatching { context.startActivity(intent) }.isSuccess
        if (!launched) {
            copyToClipboard(context, XHS_PROFILE_URL)
            Toast.makeText(context, "未安装小红书，已复制链接到剪贴板", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        /* ---------- 顶栏 ---------- */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 22.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(ArashiShape.pill)
                    .clickable { navController.popBackStack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = c.textSecondary,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                "联系作者",
                color = c.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
            )
        }

        /* ---------- 小红书名片 ---------- */
        Spacer(Modifier.height(36.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            XiaohongshuLogo(72.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                "小红书",
                color = c.textTertiary,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "琪琪",
                color = c.text,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
        }

        /* ---------- 主页链接 ---------- */
        Spacer(Modifier.height(30.dp))
        ArashiCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            shape = ArashiShape.lg,
            onClick = { openProfile() },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                XiaohongshuLogo(30.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "小红书主页",
                        color = c.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        XHS_PROFILE_URL,
                        color = c.textTertiary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = c.textTertiary,
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        /* ---------- 说明 ---------- */
        Spacer(Modifier.height(20.dp))
        Text(
            text = "使用中遇到问题、想要新功能，欢迎来小红书找我",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 34.dp),
            color = c.textSecondary,
            fontSize = 13.5.sp,
            textAlign = TextAlign.Center,
        )

        /* ---------- 主按钮 ---------- */
        Spacer(Modifier.height(28.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GradientButton(text = "打开小红书主页") { openProfile() }
        }

        Spacer(Modifier.height(30.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(12.dp),
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    runCatching {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        manager?.setPrimaryClip(ClipData.newPlainText("小红书主页", text))
    }
}
