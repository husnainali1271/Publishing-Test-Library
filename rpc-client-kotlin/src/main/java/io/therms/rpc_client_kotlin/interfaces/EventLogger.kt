package io.therms.rpc_client_kotlin.interfaces

import okhttp3.OkHttpClient
import org.json.JSONObject

interface EventLogger {
    fun onLog(msg: String)
    fun onOkhttpInitialize(builder: OkHttpClient.Builder)
    fun onWebSocketStatusChange(isConnected: Boolean)
    fun onServerMessage(msg: String)
    fun onServerMessage(msg: JSONObject)
}