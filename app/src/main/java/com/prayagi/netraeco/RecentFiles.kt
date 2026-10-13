package com.prayagi.netraplayer

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors

@Entity(tableName = "recent_files")
data class RecentFile(@PrimaryKey val uri: String, val name: String, val positionMs: Long, val playedAtMs: Long)

@Dao
interface RecentDao {
    @Query("SELECT * FROM recent_files ORDER BY playedAtMs DESC, uri ASC LIMIT 20") fun list(): List<RecentFile>
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun put(file: RecentFile)
    @Query("DELETE FROM recent_files WHERE uri = :uri") fun remove(uri: String)
    @Query("DELETE FROM recent_files") fun clear()
    @Query("DELETE FROM recent_files WHERE uri NOT IN (SELECT uri FROM recent_files ORDER BY playedAtMs DESC, uri ASC LIMIT 20)") fun trim()
}

@Database(entities = [RecentFile::class], version = 1, exportSchema = true)
abstract class RecentDatabase : RoomDatabase() { abstract fun recent(): RecentDao }

/** One local-only repository for UI, service and widget. Room I/O never runs on the UI thread. */
object RecentFiles {
    private val queue = Executors.newSingleThreadExecutor()
    private val lock = Any()
    private var db: RecentDatabase? = null
    private var started = false
    private val entries = MutableStateFlow<List<RecentFile>>(emptyList())
    val items: StateFlow<List<RecentFile>> = entries
    private val privateState = MutableStateFlow(false)
    val privatePlayback: StateFlow<Boolean> = privateState
    private const val SETTINGS = "netra_history_settings"
    private const val LEGACY = "netra_last_played"
    private var generation = 0L

    fun start(context: Context) = synchronized(lock) {
        if (started) return@synchronized
        started = true
        val c = context.applicationContext
        privateState.value = c.getSharedPreferences(SETTINGS, 0).getBoolean("private", false)
        queue.execute {
            try {
                val database = Room.databaseBuilder(c, RecentDatabase::class.java, "recent-files.db").build()
                db = database
                migrateLegacy(c, database)
                publish(c)
            } catch (_: Exception) { /* Keep legacy data on failure; never log private file addresses. */ }
        }
    }

    internal fun migrateLegacy(c: Context, database: RecentDatabase) {
        val settings = c.getSharedPreferences(SETTINGS, 0)
        if (!settings.getBoolean("legacy_migrated", false)) {
            val old = c.getSharedPreferences(LEGACY, 0)
            val uri = old.getString("uri", null)
            val name = old.getString("name", null)
            if (uri != null && name != null && RecentHistoryPolicy.accepts(uri)) {
                database.runInTransaction {
                    database.recent().put(RecentFile(uri, name, old.getLong("pos", 0).coerceAtLeast(0), old.getLong("at", 0)))
                    database.recent().trim()
                }
            }
            // Leave the source intact on failure; repeating a committed insert is idempotent.
            if (settings.edit().putBoolean("legacy_migrated", true).commit()) old.edit().clear().commit()
        }
    }

    private fun publish(c: Context) {
        entries.value = db?.recent()?.list() ?: emptyList()
        PlayerWidgetProvider.refresh(c)
    }

    fun setPrivate(context: Context, enabled: Boolean) = synchronized(lock) {
        start(context)
        generation++
        privateState.value = enabled
        context.getSharedPreferences(SETTINGS, 0).edit().putBoolean("private", enabled).commit()
        PlayerWidgetProvider.refresh(context)
    }

    /** Explicit picker/history selection is the only way to permit a cleared current item again. */
    fun selected(context: Context) = synchronized(lock) {
        start(context)
        context.getSharedPreferences(SETTINGS, 0).edit().remove("suppressed_uri").commit()
    }

    fun save(context: Context, uri: String, name: String, positionMs: Long) = synchronized(lock) {
        start(context)
        if (privateState.value || !RecentHistoryPolicy.accepts(uri)) return@synchronized
        val capturedGeneration = generation
        val c = context.applicationContext
        queue.execute {
            synchronized(lock) {
                if (capturedGeneration != generation || privateState.value || c.getSharedPreferences(SETTINGS, 0).getString("suppressed_uri", null) == uri) return@synchronized
                try {
                    db?.runInTransaction {
                        db!!.recent().put(RecentFile(uri, name, positionMs.coerceAtLeast(0), System.currentTimeMillis()))
                        db!!.recent().trim()
                    }
                    publish(c)
                } catch (_: Exception) { }
            }
        }
    }

    fun remove(context: Context, uri: String, currentUri: String?) = change(context, currentUri?.takeIf { it == uri }) { it.remove(uri) }
    fun clear(context: Context, currentUri: String?) = change(context, currentUri) { it.clear() }

    private fun change(context: Context, suppressed: String?, action: (RecentDao) -> Unit) = synchronized(lock) {
        start(context)
        generation++
        val c = context.applicationContext
        if (suppressed != null) c.getSharedPreferences(SETTINGS, 0).edit().putString("suppressed_uri", suppressed).commit()
        c.getSharedPreferences(LEGACY, 0).edit().clear().commit()
        queue.execute { synchronized(lock) { try { db?.recent()?.let(action); publish(c) } catch (_: Exception) { } } }
    }
}

object RecentHistoryPolicy {
    fun accepts(uri: String): Boolean = uri.startsWith("content://") || uri.startsWith("file://")
}
