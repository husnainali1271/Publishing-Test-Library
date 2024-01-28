package io.therms.rpc_client_kotlin.util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.therms.rpc_client_kotlin.cache.CacheKeyGenerator
import org.json.JSONObject

object NetworkUtil {
    fun createCallDTO(
        procedure: String?,
        scope: String?,
        version: String?,
        args: HashMap<String, Any>?,
    ): JSONObject {
        val schema = JSONObject()
        if (procedure != null)
            schema.put(Parameters.PROCEDURE, procedure)
        if (scope != null)
            schema.put(Parameters.SCOPE, scope)
        if (version != null)
            schema.put(Parameters.VERSION, version)
        if (args != null)
            schema.put(Parameters.ARGS, JSONObject(args as Map<*, *>))

        return schema
    }

    fun createCallDTO(
        procedure: String?,
        scope: String?,
        version: String?,
        args: JSONObject?,
    ): JSONObject {
        val schema = JSONObject()
        if (procedure != null)
            schema.put(Parameters.PROCEDURE, procedure)
        if (scope != null)
            schema.put(Parameters.SCOPE, scope)
        if (version != null)
            schema.put(Parameters.VERSION, version)
        if (args != null)
            schema.put(Parameters.ARGS, args)

        return schema
    }

    fun createClientMessageDTO(
        msg: JSONObject,
    ): JSONObject {
        val schema = JSONObject()
        schema.put(Parameters.CLIENT_MESSAGE,msg)
        return schema
    }

    fun createClientMessageDTO(
        msg: String,
    ): JSONObject {
        val schema = JSONObject()
        schema.put(Parameters.CLIENT_MESSAGE,msg)
        return schema
    }

    fun createClientMessageDTO(
        msgObject: Any
    ): JSONObject {
        val schema = JSONObject()
        schema.put(Parameters.CLIENT_MESSAGE,JSONObject(Gson().toJson(msgObject)))
        return schema
    }

    fun hasServerMessage(string: String): Boolean{
        return string.contains(Parameters.SERVER_MESSAGE)
    }

    fun hasCorrelationId(string: String): Boolean{
        return string.contains(Parameters.CORRELATION_ID)
    }

    fun getCorrelationId(jsonText: String?): String {
        try{
            return JSONObject(jsonText).optString(Parameters.CORRELATION_ID,"")
        }catch (ex: Exception){
            return ""
        }
    }

    fun isSuccess(jsonText: String?): Boolean {
        try{
            return JSONObject(jsonText).optBoolean(Parameters.SUCCESS,false)
        }catch (ex: Exception){
            return false
        }
    }

    fun test2(){
        val input = "{\"d\":0.55,\"a\":\"0001\",\"e\":{\"a\":[{\"b\":\"Regular\",\"a\":\"1001\"},{\"b\":\"1002\",\"a\":\"Chocolate\"},{\"b\":\"1003\",\"a\":\"Blueberry\"},{\"a\":\"1004\",\"b\":\"Devil's Food\"}]},\"b\":\"donut\",\"f\":[{\"a\":\"5001\",\"b\":\"None\"},{\"a\":\"5002\",\"b\":\"Glazed\"},{\"a\":\"5005\",\"b\":\"Sugar\"},{\"a\":\"5007\",\"b\":\"Powdered Sugar\"},{\"a\":\"5006\",\"b\":\"Chocolate with Sprinkles\"},{\"a\":\"5003\",\"b\":\"Chocolate\"},{\"a\":\"5004\",\"b\":\"Maple\"}],\"c\":\"Cake\"}"
        val inputJson = JSONObject(input)
        val outputMap = CacheKeyGenerator.fromPayload(inputJson)
        Log.d("test",outputMap.toString())
    }


}