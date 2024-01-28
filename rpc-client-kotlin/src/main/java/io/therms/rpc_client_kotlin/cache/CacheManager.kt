package io.therms.rpc_client_kotlin.cache

import android.content.Context
import org.json.JSONObject

class CacheManager(context: Context, private val cacheMaxAge: Long): Cache {
    private val db = CacheDatabase.getDatabase(context)

    override fun addToCache(key: String?, response: String?, endTimeMs: Long){
        try {
            if (key != null && response != null) {
                val json = JSONObject(response)
                if(json.optBoolean("success",false)){
                    json.put("fromCache",true)
                    //Response is Success, Request is Valid for caching
                    put(key,json.toString(),endTimeMs,endTimeMs + cacheMaxAge)
                }else{
                    //Response is Error, Ignore the Caching
                }
            }
            //Deleting expired entires
            clearExpired()
        }catch (ex: Exception){
            //Error Occured when Caching the request
            ex.printStackTrace()
        }
    }

    override fun get(key: String): CacheEntry? {
        return db?.cacheDao()?.getActiveCacheEntry(key,System.currentTimeMillis())?.getOrNull(0)
    }

    override fun put(key: String, data: String, requestTimestamp: Long, expiryTimestamp: Long) {
        val entry = CacheEntry(key,data,requestTimestamp,expiryTimestamp)
        db?.cacheDao()?.insertCacheEntry(entry)
    }

    override fun remove(key: String) {
        db?.cacheDao()?.deleteCacheEntry(key)
    }

    override fun clearAll() {
        db?.cacheDao()?.deleteAll()
    }

    override fun clearExpired() {
        db?.cacheDao()?.deleteExpiredEntries(System.currentTimeMillis())
    }
}