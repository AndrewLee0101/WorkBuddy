package com.arashiplayer.ui.navigation

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arashiplayer.ui.screens.home.HomeScreen
import com.arashiplayer.ui.screens.music.MusicLibraryScreen
import com.arashiplayer.ui.screens.music.NowPlayingScreen
import com.arashiplayer.ui.screens.player.VideoPlayerScreen
import com.arashiplayer.ui.screens.settings.AuthorScreen
import com.arashiplayer.ui.screens.settings.RewardScreen
import com.arashiplayer.ui.screens.settings.SettingsScreen
import com.arashiplayer.ui.screens.vault.VaultScreen
import com.arashiplayer.ui.screens.video.VideoLibraryScreen

/** 全部路由集中在这里，避免字符串散落各处 */
object Routes {
    const val HOME = "home"
    const val MUSIC = "music"
    const val VIDEO = "video"
    const val VIDEO_PLAYER = "player/video"
    const val MUSIC_PLAYER = "player/music"
    const val VAULT = "vault"
    const val SETTINGS = "settings"
    const val AUTHOR = "settings/author"
    const val REWARD = "settings/reward"

    /** 视频播放页：uri + 标题 + 可选的外挂字幕 uri */
    fun videoPlayer(uri: String, title: String, subtitleUri: String? = null): String {
        val s = Uri.encode(uri)
        val t = Uri.encode(title)
        val sub = subtitleUri?.let { Uri.encode(it) } ?: ""
        return "$VIDEO_PLAYER?uri=$s&title=$t&sub=$sub"
    }

    const val ARG_URI = "uri"
    const val ARG_TITLE = "title"
    const val ARG_SUB = "sub"
}

@Composable
fun ArashiNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.HOME,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(tween(280)) { it / 12 } + fadeIn(tween(220))
        },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(200)) },
        popExitTransition = {
            slideOutHorizontally(tween(240)) { it / 12 } + fadeOut(tween(180))
        },
    ) {
        composable(Routes.HOME) {
            HomeScreen(navController)
        }

        composable(Routes.MUSIC) {
            MusicLibraryScreen(navController)
        }

        composable(Routes.VIDEO) {
            VideoLibraryScreen(navController)
        }

        composable(Routes.MUSIC_PLAYER) {
            NowPlayingScreen(navController)
        }

        composable(
            route = "${Routes.VIDEO_PLAYER}?${Routes.ARG_URI}={${Routes.ARG_URI}}&" +
                    "${Routes.ARG_TITLE}={${Routes.ARG_TITLE}}&${Routes.ARG_SUB}={${Routes.ARG_SUB}}",
            arguments = listOf(
                navArgument(Routes.ARG_URI) { type = NavType.StringType; defaultValue = "" },
                navArgument(Routes.ARG_TITLE) { type = NavType.StringType; defaultValue = "" },
                navArgument(Routes.ARG_SUB) { type = NavType.StringType; defaultValue = "" },
            ),
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(200)) },
        ) { entry ->
            val rawUri = entry.arguments?.getString(Routes.ARG_URI).orEmpty()
            val rawTitle = entry.arguments?.getString(Routes.ARG_TITLE).orEmpty()
            val rawSub = entry.arguments?.getString(Routes.ARG_SUB).orEmpty()
            VideoPlayerScreen(
                navController = navController,
                mediaUri = Uri.decode(rawUri),
                title = Uri.decode(rawTitle),
                externalSubtitleUri = Uri.decode(rawSub).takeIf { it.isNotBlank() },
            )
        }

        composable(Routes.VAULT) { VaultScreen(navController) }
        composable(Routes.SETTINGS) { SettingsScreen(navController) }
        composable(Routes.AUTHOR) { AuthorScreen(navController) }
        composable(Routes.REWARD) { RewardScreen(navController) }
    }
}
