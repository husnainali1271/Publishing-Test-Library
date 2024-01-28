package io.therms.rpc_client_kotlin.requests

import io.therms.rpc_client_kotlin.cache.CacheLookup
import io.therms.rpc_client_kotlin.models.RpcException
import io.therms.rpc_client_kotlin.models.RpcResponse
import io.therms.rpc_client_kotlin.queues.RequestQueue
import io.therms.rpc_client_kotlin.util.Parameters
import org.json.JSONObject
import java.util.*

class ClientMessageRequest(private val payload: JSONObject){

    fun createBody(): String {
        val json = JSONObject(payload.toString())
        return json.toString()
    }

    fun createBody(excludeIdentity: Boolean): String {
        val json = JSONObject(payload.toString())
        if(excludeIdentity)
            json.remove(Parameters.IDENTITY)
        return json.toString()
    }

    fun getPayload(): JSONObject{
        return payload
    }

    fun hasIdentity(): Boolean{
        return payload.has(Parameters.IDENTITY)
    }

    companion object{
    }
}