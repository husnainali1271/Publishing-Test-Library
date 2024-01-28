package io.therms.rpc_client_kotlin.cache

import androidx.room.*

@Dao
interface CacheDao {
    @Query("SELECT * FROM cache_table")
    fun getAllCacheEntries(): List<CacheEntry>

    @Query("SELECT * FROM cache_table WHERE uid = :key AND expiry_timestamp > :currentTime")
    fun getActiveCacheEntry(key: String, currentTime: Long): List<CacheEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCacheEntry(cache: CacheEntry)

    @Query("DELETE from cache_table where uid = :key")
    fun deleteCacheEntry(key: String)

    @Query("DELETE from cache_table where expiry_timestamp < :currentTime")
    fun deleteExpiredEntries(currentTime: Long)

    @Query("DELETE from cache_table")
    fun deleteAll()
}