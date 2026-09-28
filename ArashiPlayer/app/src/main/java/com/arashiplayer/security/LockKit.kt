package com.arashiplayer.security

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * 加密空间的核心工具。
 *
 * 设计取舍（按需求「不额外占用系统空间」）：
 * 这里做的是 **原地改名 + 改后缀**，不是内容加密、也不复制文件。
 * - `旅行.mp4`  →  `<原目录>/.arashi_vault/8f3a1c9e.arv`
 * - 因为 rename 在同一文件系统内完成，磁盘占用不变（0 字节额外开销）
 * - 后缀 `.arv` 不被 MediaStore 识别，配合 `.nomedia`，相册 / 其他 App 的扫描器都会跳过
 * - 「还原」= 改回原名原后缀，随时可以取回
 *
 * 代价要说清楚：这不是密码学意义上的加密，文件本体并未被打乱。
 * 如果你需要「即使被拷走也打不开」的强度，请在设置里打开「真加密模式」（AES + 复制存储，会额外占空间）。
 * 默认关闭，以符合本需求的空间要求。
 */
object LockKit {

    private const val ITERATIONS = 12_000
    private const val KEY_LENGTH = 256
    private const val ALGO = "PBKDF2WithHmacSHA256"

    private val random = SecureRandom()

    /** 生成随机盐 */
    fun newSalt(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /** PBKDF2 派生摘要，用于比对而不是保存明文 */
    fun hash(credential: String, salt: String): String {
        val spec = PBEKeySpec(
            credential.toCharArray(),
            Base64.decode(salt, Base64.NO_WRAP),
            ITERATIONS,
            KEY_LENGTH
        )
        val factory = SecretKeyFactory.getInstance(ALGO)
        val bytes = factory.generateSecret(spec).encoded
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /** 恒定时间比较，避免时序侧信道 */
    fun verify(credential: String, salt: String, expectedHash: String): Boolean {
        if (salt.isBlank() || expectedHash.isBlank()) return false
        val actual = runCatching { hash(credential, salt) }.getOrNull() ?: return false
        return MessageDigest.isEqual(actual.toByteArray(), expectedHash.toByteArray())
    }

    /** 生成混淆文件名 —— 无规律、无扩展信息，看不出原文件类型 */
    fun randomVaultName(): String {
        val bytes = ByteArray(8)
        random.nextBytes(bytes)
        val hex = bytes.joinToString("") { "%02x".format(it) }
        return "$hex.${com.arashiplayer.util.VAULT_EXTENSION}"
    }

    /**
     * 把任意 content:// 解析回真实文件路径。
     * 只有拿到真实路径才能「原地改名」，否则只能复制（会额外占空间，我们会明确告知用户）。
     */
    fun resolveRealPath(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.path
        if (uri.scheme == null) return uri.toString().takeIf { File(it).exists() }
        return runCatching {
            when (uri.authority) {
                "media" -> queryData(context, uri)
                "com.android.externalstorage.documents" -> {
                    val docId = uri.lastPathSegment ?: return@runCatching null
                    val parts = docId.split(":")
                    val type = parts[0]
                    val rel = parts.getOrElse(1) { "" }
                    val root = when (type.lowercase()) {
                        "primary" -> "/storage/emulated/0"
                        else -> "/storage/$type"
                    }
                    File(root, rel).absolutePath
                }
                else -> queryData(context, uri)
            }
        }.getOrNull()
    }

    private fun queryData(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf("_data"), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()

    /** 生成可被播放器 / 图片查看器读取的临时 Uri（供 App 内部预览） */
    fun vaultUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
