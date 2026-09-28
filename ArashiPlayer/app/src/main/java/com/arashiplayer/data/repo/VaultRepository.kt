package com.arashiplayer.data.repo

import android.content.Context
import com.arashiplayer.data.local.ArashiDatabase
import com.arashiplayer.data.local.VaultEntity
import com.arashiplayer.data.model.VaultItem
import com.arashiplayer.data.model.VaultKind
import com.arashiplayer.security.LockKit
import com.arashiplayer.util.AUDIO_EXTENSIONS
import com.arashiplayer.util.IMAGE_EXTENSIONS
import com.arashiplayer.util.NO_MEDIA
import com.arashiplayer.util.VAULT_DIR_NAME
import com.arashiplayer.util.VIDEO_EXTENSIONS
import com.arashiplayer.util.extensionOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 加密空间仓库。
 *
 * 核心动作只有两个：
 *  - [hide]    把文件 rename 成 `<目录>/.arashi_vault/<随机>.arv`
 *  - [restore] 把 `.arv` 改回原文件名与后缀
 *
 * 全程不复制字节，所以**不额外占用系统空间**；
 * 同时在目录里放 `.nomedia`，让相册与第三方扫描器直接跳过。
 */
class VaultRepository(private val context: Context) {

    private val dao = ArashiDatabase.get(context).vaultDao()

    val items: Flow<List<VaultItem>> = dao.observeAll().map { list -> list.map(VaultEntity::toModel) }
    val count: Flow<Int> = dao.observeCount()
    val totalSize: Flow<Long> = dao.observeTotalSize()

    fun itemsOf(kind: VaultKind): Flow<List<VaultItem>> =
        dao.observeByKind(kind.name).map { list -> list.map(VaultEntity::toModel) }

    /** 按类型分布统计，用于加密空间首页 */
    fun countsByKind(): Flow<Map<VaultKind, Int>> = items.map { list ->
        list.groupingBy { it.kind }.eachCount()
    }

    /* ============================ 加入加密空间 ============================ */

    /**
     * @param paths 真实文件路径
     * @return 成功加入的条目数；无法原地改名（跨文件系统 / 无权限）的会被跳过
     */
    suspend fun hide(paths: List<String>): VaultResult = withContext(Dispatchers.IO) {
        var ok = 0
        var skipped = 0
        var freedHint = 0L

        paths.forEach { raw ->
            runCatching {
                val src = File(raw)
                if (!src.exists() || !src.isFile) { skipped++; return@runCatching }

                val parent = src.parentFile ?: run { skipped++; return@runCatching }
                val vaultDir = File(parent, VAULT_DIR_NAME).apply { if (!exists()) mkdirs() }
                writeNoMedia(vaultDir)
                writeNoMedia(parent)

                val target = File(vaultDir, LockKit.randomVaultName())
                if (!src.renameTo(target)) { skipped++; return@runCatching }

                val ext = extensionOf(src.name)
                val entity = VaultEntity(
                    vaultName = target.name,
                    currentPath = target.absolutePath,
                    originalName = src.name,
                    originalPath = src.absolutePath,
                    originalExt = ext,
                    kind = kindOf(ext).name,
                    sizeBytes = target.length(),
                    addedAt = System.currentTimeMillis(),
                    mimeType = null,
                )
                dao.insert(entity)
                freedHint += target.length()
                ok++
            }.onFailure { skipped++ }
        }
        VaultResult(ok, skipped, freedHint)
    }

    /** 从外部 App 分享 / 选择进来的 Uri */
    suspend fun hideUris(uris: List<android.net.Uri>): VaultResult {
        val paths = uris.mapNotNull { LockKit.resolveRealPath(context, it) }
        return hide(paths)
    }

    /* ============================ 移出 / 还原 ============================ */

    suspend fun restore(item: VaultItem): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val current = File(item.currentPath)
            val targetDir = File(item.originalPath).parentFile ?: current.parentFile?.parentFile ?: current.parentFile
            val target = File(targetDir, item.originalName)

            // 同名冲突时补时间戳
            val safeTarget = if (target.exists()) {
                val base = item.originalName.substringBeforeLast('.')
                val ext = item.originalExt
                File(targetDir, "${base}_${System.currentTimeMillis()}.$ext")
            } else target

            val moved = current.renameTo(safeTarget)
            if (moved) dao.delete(item.id)
            moved
        }.getOrDefault(false)
    }

    suspend fun restoreAll(): Int = withContext(Dispatchers.IO) {
        var n = 0
        dao.all().forEach { e -> if (restore(e.toModel())) n++ }
        n
    }

    suspend fun deleteForever(item: VaultItem): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val f = File(item.currentPath)
            val gone = !f.exists() || f.delete()
            if (gone) dao.delete(item.id)
            gone
        }.getOrDefault(false)
    }

    suspend fun findByPath(path: String): VaultItem? = dao.findByCurrentPath(path)?.toModel()

    /* ============================ 工具 ============================ */

    /** 写入 .nomedia，让系统相册与第三方扫描器跳过该目录 */
    private fun writeNoMedia(dir: File) {
        runCatching {
            val f = File(dir, NO_MEDIA)
            if (!f.exists()) f.createNewFile()
        }
    }

    /** App 首次启动时清扫：确保所有加密目录都带 .nomedia */
    suspend fun ensureNoMediaAll() = withContext(Dispatchers.IO) {
        dao.all().forEach { e ->
            runCatching { File(e.currentPath).parentFile?.let { writeNoMedia(it) } }
            runCatching { File(e.originalPath).parentFile?.let { writeNoMedia(it) } }
        }
    }

    private fun kindOf(ext: String): VaultKind = when (ext.lowercase()) {
        in IMAGE_EXTENSIONS -> VaultKind.IMAGE
        in VIDEO_EXTENSIONS -> VaultKind.VIDEO
        in AUDIO_EXTENSIONS -> VaultKind.AUDIO
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md" -> VaultKind.DOC
        else -> VaultKind.OTHER
    }

    data class VaultResult(val added: Int, val skipped: Int, val bytesMoved: Long)
}
