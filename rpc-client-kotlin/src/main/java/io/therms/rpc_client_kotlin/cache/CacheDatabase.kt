package io.therms.rpc_client_kotlin.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.util.concurrent.Executors

@Database(
    entities = [CacheEntry::class],
    version = 1,
    exportSchema = false
)
abstract class CacheDatabase : RoomDatabase() {

    abstract fun cacheDao(): CacheDao

    companion object {

        @Volatile
        private var INSTANCE: CacheDatabase? = null
        private val NUMBER_OF_THREADS = 4
        internal val databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS)

        fun getDatabase(context: Context): CacheDatabase? {
            if (INSTANCE == null) {
                synchronized(CacheDatabase::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            CacheDatabase::class.java, "cache_database"
                        ).addMigrations()
                            .fallbackToDestructiveMigration()
                            .build()
                    }
                }
            }
            return INSTANCE
        }

    }
}