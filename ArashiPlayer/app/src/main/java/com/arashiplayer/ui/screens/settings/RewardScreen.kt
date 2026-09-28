package com.arashiplayer.ui.screens.settings

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.arashiplayer.R
import com.arashiplayer.ui.components.ArashiCard
import com.arashiplayer.ui.theme.ArashiGradient
import com.arashiplayer.ui.theme.ArashiShape
import com.arashiplayer.ui.theme.ArashiTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** 赞赏作者：支付宝 + 微信赞赏码，支持保存到相册 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun RewardScreen(navController: NavHostController) {
    val c = ArashiTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 支付宝收款码：按原图比例撑满卡片宽度，避免被裁剪
    val alipayPainter = painterResource(R.drawable.ic_reward_alipay)
    val alipayRatio = alipayPainter.intrinsicSize.let { s ->
        if (s.width > 0f && s.height > 0f) s.width / s.height else 1f
    }

    fun save(drawableId: Int, fileName: String) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                saveImageToGallery(context, drawableId, fileName)
            }
            Toast.makeText(
                context,
                if (ok) "已保存到相册" else "保存失败，请检查存储权限",
                Toast.LENGTH_SHORT,
            ).show()
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
                "赞赏作者",
                color = c.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
            )
        }

        /* ---------- 附言（渐变文字） ---------- */
        Spacer(Modifier.height(30.dp))
        Text(
            text = "您的支持是我坚持的动力",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            style = TextStyle(
                brush = ArashiGradient,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(30.dp))

        /* ---------- 支付宝 ---------- */
        ArashiCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            shape = ArashiShape.xl,
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    "支付宝",
                    color = c.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "打开支付宝扫一扫",
                    color = c.textTertiary,
                    fontSize = 12.5.sp,
                )
                Spacer(Modifier.height(14.dp))
                Image(
                    painter = alipayPainter,
                    contentDescription = "支付宝赞赏码",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(alipayRatio)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White),
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    SaveChip(onClick = {
                        save(R.drawable.ic_reward_alipay, "arashi_alipay_${System.currentTimeMillis()}.png")
                    })
                }
            }
        }

        /* ---------- 微信赞赏码 ---------- */
        Spacer(Modifier.height(18.dp))
        ArashiCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            shape = ArashiShape.xl,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "微信赞赏码",
                    modifier = Modifier.fillMaxWidth(),
                    color = c.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "长按识别或扫码赞赏",
                    modifier = Modifier.fillMaxWidth(),
                    color = c.textTertiary,
                    fontSize = 12.5.sp,
                )
                Spacer(Modifier.height(16.dp))
                Image(
                    painter = painterResource(R.drawable.ic_reward_wechat),
                    contentDescription = "微信赞赏码",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White),
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    SaveChip(onClick = {
                        save(R.drawable.ic_reward_wechat, "arashi_wechat_${System.currentTimeMillis()}.png")
                    })
                }
            }
        }

        /* ---------- 页脚 ---------- */
        Spacer(Modifier.height(26.dp))
        Text(
            text = "感谢每一位愿意支持独立开发的人",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            color = c.textTertiary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(12.dp),
        )
    }
}

/* ============================ 局部组件 ============================ */

@Composable
private fun SaveChip(onClick: () -> Unit) {
    val c = ArashiTheme.colors
    Row(
        modifier = Modifier
            .clip(ArashiShape.pill)
            .background(c.cardPressed)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Download,
            contentDescription = null,
            tint = c.textSecondary,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text("保存到相册", color = c.textSecondary, fontSize = 12.sp)
    }
}

/* ============================ 保存到相册 ============================ */

@Suppress("DEPRECATION")
private fun saveImageToGallery(context: Context, drawableId: Int, fileName: String): Boolean {
    return runCatching {
        val bitmap: Bitmap = BitmapFactory.decodeResource(context.resources, drawableId)
            ?: return false

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/ArashiPlayer",
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "ArashiPlayer",
                )
                if (!dir.exists()) dir.mkdirs()
                put(MediaStore.Images.Media.DATA, File(dir, fileName).absolutePath)
            }
        }

        val uri = context.contentResolver
            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false

        context.contentResolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } ?: return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            context.contentResolver.update(uri, done, null, null)
        }

        bitmap.recycle()
        true
    }.getOrDefault(false)
}
