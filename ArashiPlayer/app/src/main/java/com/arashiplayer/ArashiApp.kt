package com.arashiplayer

import android.app.Application
import android.content.Context
import com.arashiplayer.data.local.ArashiDatabase
import com.arashiplayer.data.local.SettingsStore
import com.arashiplayer.data.net.OnlineMetaApi
import com.arashiplayer.data.repo.MediaRepository
import com.arashiplayer.data.repo.VaultRepository
import com.arashiplayer.player.PlayerHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 应用入口。用极简的 Service Locator 代替 DI 框架，
 * 目的是让这份代码在任何一台机器上 `./gradlew assembleDebug` 就能跑，不依赖注解处理顺序。
 */
class ArashiApp : Application() {

    val appScope = CoroutineScope(SupervisorJob())

    val database: ArashiDatabase by lazy { ArashiDatabase.get(this) }
    val settings: SettingsStore by lazy { SettingsStore(this) }
    val mediaRepo: MediaRepository by lazy { MediaRepository(this) }
    val vaultRepo: VaultRepository by lazy { VaultRepository(this) }
    val onlineApi: OnlineMetaApi by lazy { OnlineMetaApi() }
    val playerHolder: PlayerHolder by lazy { PlayerHolder(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        appScope.launch {
            // 启动兜底：保证历史加密目录都带 .nomedia，避免被相册重新扫出来
            runCatching { vaultRepo.ensureNoMediaAll() }
        }
    }

    companion object {
        @Volatile
        private var instance: ArashiApp? = null

        /** 从任意 Context 拿到 Application 实例 */
        fun of(context: Context): ArashiApp {
            val app = context.applicationContext
            if (app is ArashiApp) return app
            return instance ?: error("ArashiApp 尚未初始化，请检查 AndroidManifest 中的 android:name")
        }

        fun get(): ArashiApp =
            instance ?: error("ArashiApp 尚未初始化，请检查 AndroidManifest 中的 android:name")
    }
}

/** 便捷访问：`context.arashi` */
val Context.arashi: ArashiApp get() = ArashiApp.of(this)
