package io.therms.rpc_client_kotlin.transport

import io.therms.rpc_client_kotlin.requests.RpcRequest

interface Transport {
    fun isConnected(): Boolean
    fun performRequest(request: RpcRequest<*>): String?
    fun disconnect()
    fun clearIdentityCache()
}