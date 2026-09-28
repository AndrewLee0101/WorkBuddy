package com.arashiplayer

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.arashiplayer.ui.navigation.ArashiNavHost
import com.arashiplayer.ui.navigation.Routes
import com.arashiplayer.ui.theme.ArashiTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {

    /** 被系统「打开方式」唤起的媒体 Uri，交给 Compose 侧决定怎么进播放器 */
    private var pendingMediaUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingMediaUri = extractMediaUri(intent)
        setContent { ArashiRoot(deepLinkUri = pendingMediaUri) }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingMediaUri = extractMediaUri(intent)
    }

    private fun extractMediaUri(intent: android.content.Intent?): Uri? {
        if (intent == null) return null
        if (intent.action != android.content.Intent.ACTION_VIEW) return null
        return intent.data
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ArashiRoot(deepLinkUri: Uri?) {
    val context = LocalContext.current
    val settings = remember { ArashiApp.of(context).settings }

    val themeMode by settings.themeMode.collectAsState(initial = "system")
    val darkTheme: Boolean? = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> null
    }

    // 媒体读取权限：Android 13+ 按类型申请，以下用旧的读写权限
    val permList = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
    val perms = rememberMultiplePermissionsState(permList)

    LaunchedEffect(Unit) {
        if (!perms.allPermissionsGranted) perms.launchMultiplePermissionRequest()
    }

    val navController = rememberNavController()

    // 从「打开方式」进来 → 直接播这个文件
    LaunchedEffect(deepLinkUri) {
        val uri = deepLinkUri ?: return@LaunchedEffect
        navController.navigate(Routes.videoPlayer(uri.toString(), ""))
    }

    ArashiTheme(darkTheme = darkTheme) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            ArashiNavHost(navController = navController)
        }
    }
}
