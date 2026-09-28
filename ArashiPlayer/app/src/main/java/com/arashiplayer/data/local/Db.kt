package com.arashiplayer.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.arashiplayer.data.model.OnlineMeta
import com.arashiplayer.data.model.VaultItem
import com.arashiplayer.data.model.VaultKind
import kotlinx.coroutines.flow.Flow

/* ============================ 实体 ============================ */

/**
 * 加密索引。只存「映射关系」，媒体本体仍在原路径上（仅改名 + 改后缀）。
 * 因此不会产生任何数据副本，也就不额外占用系统空间。
 */
@Entity(tableName = "vault_items", indices = [Index(value = ["currentPath"], unique = true)])
data class VaultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vaultName: String,
    val currentPath: String,
    val originalName: String,
    val originalPath: String,
    val originalExt: String,
    val kind: String,
    val sizeBytes: Long,
    val addedAt: Long,
    val mimeType: String?,
) {
    fun toModel() = VaultItem(
        id = id,
        vaultName = vaultName,
        currentPath = currentPath,
        originalName = originalName,
        originalPath = originalPath,
        originalExt = originalExt,
        kind = runCatching { VaultKind.valueOf(kind) }.getOrDefault(VaultKind.OTHER),
        sizeBytes = sizeBytes,
        addedAt = addedAt,
        mimeType = mimeType,
    )

    companion object {
        fun from(item: VaultItem) = VaultEntity(
            id = item.id,
            vaultName = item.vaultName,
            currentPath = item.currentPath,
            originalName = item.originalName,
            originalPath = item.originalPath,
            originalExt = item.originalExt,
            kind = item.kind.name,
            sizeBytes = item.sizeBytes,
            addedAt = item.addedAt,
            mimeType = item.mimeType,
        )
    }
}

/** 在线封面 / 歌词缓存（仿「音乐标签」的匹配结果本地留存） */
@Entity(tableName = "online_meta")
data class OnlineMetaEntity(
    @PrimaryKey val trackKey: String,
    val matchedTitle: String?,
    val matchedArtist: String?,
    val album: String?,
    val coverUrl: String?,
    val lyric: String?,
    val translatedLyric: String?,
    val source: String,
    val updatedAt: Long,
) {
    fun toModel() = OnlineMeta(
        query = trackKey, matchedTitle = matchedTitle, matchedArtist = matchedArtist,
        album = album, coverUrl = coverUrl, lyric = lyric, translatedLyric = translatedLyric,
        source = source, updatedAt = updatedAt,
    )

    companion object {
        fun from(key: String, m: OnlineMeta) = OnlineMetaEntity(
            trackKey = key, matchedTitle = m.matchedTitle, matchedArtist = m.matchedArtist,
            album = m.album, coverUrl = m.coverUrl, lyric = m.lyric,
            translatedLyric = m.translatedLyric, source = m.source, updatedAt = m.updatedAt
        )
    }
}

/** 播放进度记忆（视频续播 / 音乐记位） */
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val mediaKey: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
)

/** 最近播放 */
@Entity(tableName = "recents")
data class RecentEntity(
    @PrimaryKey val mediaKey: String,
    val title: String,
    val subtitle: String,
    val uri: String,
    val isVideo: Boolean,
    val playedAt: Long,
)

/** 自建歌单 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val coverUri: String?,
)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "trackId"])
data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackId: Long,
    val orderIndex: Int,
)

/* ============================ DAO ============================ */

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vault_items ORDER BY addedAt DESC")
    suspend fun all(): List<VaultEntity>

    @Query("SELECT * FROM vault_items WHERE kind = :kind ORDER BY addedAt DESC")
    fun observeByKind(kind: String): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vault_items WHERE currentPath = :path LIMIT 1")
    suspend fun findByCurrentPath(path: String): VaultEntity?

    @Query("SELECT COUNT(*) FROM vault_items")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VaultEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VaultEntity>)

    @Update
    suspend fun update(item: VaultEntity)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM vault_items")
    fun observeTotalSize(): Flow<Long>
}

@Dao
interface MetaDao {
    @Query("SELECT * FROM online_meta WHERE trackKey = :key LIMIT 1")
    suspend fun find(key: String): OnlineMetaEntity?

    @Query("SELECT * FROM online_meta")
    fun observeAll(): Flow<List<OnlineMetaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OnlineMetaEntity)

    @Query("DELETE FROM online_meta WHERE trackKey = :key")
    suspend fun delete(key: String)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE mediaKey = :key LIMIT 1")
    suspend fun find(key: String): BookmarkEntity?

    @Query("SELECT positionMs FROM bookmarks WHERE mediaKey = :key LIMIT 1")
    suspend fun position(key: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BookmarkEntity)

    @Query("SELECT * FROM bookmarks ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<BookmarkEntity>>
}

@Dao
interface RecentDao {
    @Query("SELECT * FROM recents ORDER BY playedAt DESC LIMIT :limit")
    fun observe(limit: Int): Flow<List<RecentEntity>>

    @Query("SELECT * FROM recents WHERE isVideo = :isVideo ORDER BY playedAt DESC LIMIT :limit")
    fun observeByType(isVideo: Boolean, limit: Int): Flow<List<RecentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecentEntity)

    @Query("DELETE FROM recents")
    suspend fun clear()
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :id ORDER BY orderIndex ASC")
    suspend fun tracksOf(id: Long): List<PlaylistTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrack(entity: PlaylistTrackEntity)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :id AND trackId = :trackId")
    suspend fun removeTrack(id: Long, trackId: Long)
}

/* ============================ Database ============================ */

@Database(
    entities = [
        VaultEntity::class, OnlineMetaEntity::class, BookmarkEntity::class,
        RecentEntity::class, PlaylistEntity::class, PlaylistTrackEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ArashiDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
    abstract fun metaDao(): MetaDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun recentDao(): RecentDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile private var INSTANCE: ArashiDatabase? = null

        fun get(context: Context): ArashiDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                ArashiDatabase::class.java,
                "arashi.db"
            )
                // 加密空间索引属于隐私数据，禁用自动降级重建
                .fallbackToDestructiveMigrationOnDowngrade()
                .build().also { INSTANCE = it }
        }
    }
}
