package io.therms.rpc_client_kotlin.cache

interface Cache {
    fun addToCache(key: String?, response: String?, endTimeMs: Long)
    fun get(key: String): CacheEntry?
    fun put(key: String, data: String, requestTimestamp: Long, expiryTimestamp: Long)
    fun remove(key: String)
    fun clearAll()
    fun clearExpired()
}