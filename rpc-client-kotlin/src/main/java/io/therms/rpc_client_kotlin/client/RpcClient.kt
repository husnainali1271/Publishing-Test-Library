package io.therms.rpc_client_kotlin.client

import android.content.Context
import io.therms.rpc_client_kotlin.cache.CacheLookup
import io.therms.rpc_client_kotlin.cache.CacheManager
import io.therms.rpc_client_kotlin.interfaces.EventLogger
import io.therms.rpc_client_kotlin.models.RpcResponse
import io.therms.rpc_client_kotlin.requests.RpcRequest
import io.therms.rpc_client_kotlin.queues.RequestQueue
import io.therms.rpc_client_kotlin.requests.BodyInterceptor
import io.therms.rpc_client_kotlin.requests.ClientMessageRequest
import io.therms.rpc_client_kotlin.requests.ErrorInterceptor
import io.therms.rpc_client_kotlin.transport.WsEventListener
import io.therms.rpc_client_kotlin.util.Parameters
import org.json.JSONObject

class RpcClient(val context: Context): WsEventListener {
    private var webSocketUrl: String? = null
    private var httpUrl: String? = null
    private var headers: HashMap<String,String> = HashMap()
    private var shouldCacheResponses: Boolean = false
    private var cacheMaxAgeMs: Long = DEFAULT_CACHE_MAX_AGE
    private var useDuplicateInFlightCall: Boolean = false
    private var requestQueue: RequestQueue? = null
    private var evenListener: EventLogger? = null
    private var bodyInterceptor: BodyInterceptor? = null
    private var errorInterceptor: ErrorInterceptor? = null
    private var useWebsocketInBackground: Boolean = false

    fun setWebSocketUrl(url: String) = apply { this.webSocketUrl = url }
    fun setHttpUrl(url: String) = apply { this.httpUrl = url }
    fun setHeaders(headers: HashMap<String,String>) = apply { this.headers = headers }
    fun shouldCacheResponses(enable: Boolean) = apply { this.shouldCacheResponses = enable }
    fun cacheMaxAge(age: Long) = apply { this.cacheMaxAgeMs = age }
    fun useDuplicateInFlightCall(enable: Boolean) = apply { this.useDuplicateInFlightCall = enable }
    fun setEventListener(listener: EventLogger) = apply { this.evenListener = listener }
    fun setBodyInterceptor(interceptor: BodyInterceptor?) = apply { this.bodyInterceptor = interceptor }
    fun setErrorInterceptor(interceptor: ErrorInterceptor?) =apply { this.errorInterceptor =  interceptor}
    fun useWebSocketInBackground(enable: Boolean) = apply { this.useWebsocketInBackground = enable }

    fun getWebSocketUrl() = webSocketUrl
    fun getHttpUrl() = httpUrl
    fun getHeaders() = headers
    fun shouldCacheResponses() = shouldCacheResponses
    fun getCacheMaxAgeMs() = cacheMaxAgeMs
    fun getErrorInterceptor() = errorInterceptor
    fun useWebSocketInBackground() = useWebsocketInBackground
    fun getEventListener() = evenListener
    fun disconnectClient() = requestQueue?.disconnect()
    fun clearIdentityCache() = requestQueue?.clearIdentityCache()
    fun isWebSocketConnected() = requestQueue?.isWebSocketConnected()

    fun build() = apply {
        requestQueue = RequestQueue(this)
        requestQueue?.start()
    }

    fun sendClientMessageToServer(request: ClientMessageRequest){
        bodyInterceptor?.intercept(request.getPayload())
        requestQueue?.sendClientMessageToServer(request)
    }

    fun call(request: RpcRequest<*>){
        bodyInterceptor?.intercept(request.getPayload())
        requestQueue?.add(request)
    }

    fun callCache(request: RpcRequest<*>, cacheLookup: CacheLookup = CacheLookup.Default){
        request.cacheLookup(cacheLookup)
        bodyInterceptor?.intercept(request.getPayload())
        call(request)
    }

    fun onDestroy(){
        requestQueue?.stop()
    }

    fun logEvent(msg: String){
        evenListener?.onLog(msg)
    }

    companion object{
        val DEFAULT_CACHE_MAX_AGE: Long = 60 * 1000 * 5

        /**
         * Can be used to clear all request cache of client, Should be called from couroutine
         */
        suspend fun clearCache(context: Context){
            val manager = CacheManager(context, DEFAULT_CACHE_MAX_AGE)
            manager.clearAll()
        }
    }

    override fun onWsConnected() {
        evenListener?.onWebSocketStatusChange(true)

        val identityBody = JSONObject()
        bodyInterceptor?.intercept(identityBody)
        if(identityBody.has(Parameters.IDENTITY)) {
            val request = RpcRequest<RpcResponse<*>>(identityBody, RpcResponse::class.java)
            requestQueue?.add(request)
        }
    }

    override fun onWsDisconnected() {
        evenListener?.onWebSocketStatusChange(false)
    }

    override fun onServerMessage(msg: String) {
        evenListener?.onServerMessage(msg)
    }

    override fun onServerMessage(msg: JSONObject) {
        evenListener?.onServerMessage(msg)
    }
}