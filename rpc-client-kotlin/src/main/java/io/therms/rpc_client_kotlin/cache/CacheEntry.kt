package io.therms.rpc_client_kotlin.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cache_table")
data class CacheEntry(
    @PrimaryKey val uid: String,
    @ColumnInfo(name = "data") val data: String,
    @ColumnInfo(name = "request_timestamp") val requestTimestamp: Long,
    @ColumnInfo(name = "expiry_timestamp") val expiryTimestamp: Long,
)