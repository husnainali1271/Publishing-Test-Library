package io.therms.rpc_client_kotlin.cache

import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

object CacheKeyGenerator {

    fun fromPayload(json: JSONObject): String?{
        try{
            val treeMap = parseJSONObject(json)
            return treeMap.toString()
        }catch (ex: Exception){
        }

        return null
    }

    private fun parseJSONArray(array: JSONArray): ArrayList<Any>{
        val list = ArrayList<Any>()
        for(i in 0 until array.length()){
            val value = array.get(i)
            when(value){
                is JSONArray -> {
                    list.add(parseJSONArray(value))
                }
                is JSONObject -> {
                    list.add(parseJSONObject(value))
                }
                else -> {
                    list.add(value)
                }
            }
        }
        return list
    }

    private fun parseJSONObject(obj: JSONObject): TreeMap<String, Any> {
        val map = TreeMap<String,Any>()
        for(i in obj.keys()){
            val value = obj.get(i)
            when(value){
                is JSONArray -> {
                    map[i] = parseJSONArray(value)
                }
                is JSONObject -> {
                    map[i] = parseJSONObject(value)
                }
                else ->{
                    map[i] = value
                }
            }
        }
        return map
    }

}