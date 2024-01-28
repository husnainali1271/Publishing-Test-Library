package io.therms.rpc_client_kotlin.transport

import org.json.JSONObject

interface WsEventListener {
    fun onWsConnected()
    fun onWsDisconnected()
    fun onServerMessage(msg: String)
    fun onServerMessage(msg: JSONObject)
}