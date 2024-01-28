package io.therms.rpc_client_kotlin.dispatchers

import android.os.Process
import android.util.Log
import io.therms.rpc_client_kotlin.cache.CacheKeyGenerator
import io.therms.rpc_client_kotlin.cache.CacheLookup
import io.therms.rpc_client_kotlin.cache.CacheManager
import io.therms.rpc_client_kotlin.delivery.ResponseDelivery
import io.therms.rpc_client_kotlin.models.RpcException
import io.therms.rpc_client_kotlin.requests.RpcRequest
import io.therms.rpc_client_kotlin.models.RpcResponse
import io.therms.rpc_client_kotlin.transport.HttpTransport
import io.therms.rpc_client_kotlin.transport.WsTransport
import java.util.concurrent.PriorityBlockingQueue


class RequestDispatcher(
    private val tag: String,
    private val queue: PriorityBlockingQueue<RpcRequest<*>>,
    private val mDelivery: ResponseDelivery,
    private val wsTransport: WsTransport?,
    private val httpTransport: HttpTransport?,
    private val cacheManager: CacheManager?
) :
    Thread() {

    var mQuit = false

    override fun run() {
        super.run()
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        while (true) {
            try {
                processRequest()
            } catch (e: InterruptedException) {
                // We may have been interrupted because it was time to quit.
                if (mQuit) {
                    currentThread().interrupt()
                    return
                }
            }
        }
    }

    @Throws(InterruptedException::class)
    private fun processRequest() {
        // Take a request from the queue.
        val request: RpcRequest<*> = queue.take()
        request.logEvent(tag, "Request Taken by dispatcher - $tag")
        processRequest(request)
    }

    private fun processRequest(request: RpcRequest<*>) {
        try {
            var cacheKey: String? = if(cacheManager != null) CacheKeyGenerator.fromPayload(request.getPayload()) else null
            var cachedResponse: String? = null
            if (cacheManager != null && request.cacheLookup() != CacheLookup.NoCache) {
                if (cacheKey != null) {
                    request.logEvent(tag, "Cache Key Generated - $cacheKey")
                    cachedResponse = cacheManager?.get(cacheKey)?.data
                }
            }

            //Found Active Response from Cache, lets use it
            if (cachedResponse != null && request.cacheLookup() != CacheLookup.NoCache) {
                try {
                    Log.d("RequestDispatcher:cache", "" + cachedResponse)
                    request.logEvent(tag, "Result Received from Cache - $cachedResponse")
                    request.markDelivered()
                    mDelivery.postResponse(
                        request,
                        RpcResponse.parseResponse(request, cachedResponse,true)
                    )
                    if(request.cacheLookup() != CacheLookup.StaleWhileRevalidate) {
                        return
                    }
                } catch (ex: Exception) {
                    request.logEvent(tag, "Error Occured During Cache Parsing - ${ex.message}")
                }
            }

            if (wsTransport?.isConnected() == true) {
                request.logEvent(tag, "Performing Request via Socket - ${request.createBody()}")
                val response = wsTransport?.performRequest(request)
                if (response != null) {
                    Log.d("RequestDispatcher:ws", "" + response)
                    request.logEvent(tag, "Result Received from Socket - $response")
                    request.markDelivered()
                    mDelivery.postResponse(request, RpcResponse.parseResponse(request, response))
                    cacheManager?.addToCache(cacheKey, response, System.currentTimeMillis())
                    return
                } else {
                    request.logEvent(
                        tag,
                        "Request Timeout from Socket, Switching to Http - ${request.createBody()}"
                    )
                    if (httpTransport?.isConnected() == true) {
                        request.logEvent(tag, "Performing Request via HTTP - ${request.createBody()}")
                        val response = httpTransport?.performRequest(request)
                        if (response != null)
                            request.logEvent(tag, "Result Received from Http - $response")
                        Log.d("RequestDispatcher:http2", "" + response)
                        request.markDelivered()
                        mDelivery.postResponse(
                            request,
                            RpcResponse.parseResponse(request, response)
                        )
                        cacheManager?.addToCache(cacheKey, response, System.currentTimeMillis())
                        return
                    }
                }
            } else if (httpTransport?.isConnected() == true) {
                request.logEvent(
                    tag,
                    "Socket was not connected, Performing request via Http - ${request.createBody()}"
                )
                //Reconnecting wsTransport so onward RPC calls can be made via socket
                wsTransport?.reconnect()
                val response = httpTransport?.performRequest(request)
                if (response != null)
                    request.logEvent(tag, "Result Received from Http - $response")
                Log.d("RequestDispatcher:http1", "" + response)
                request.markDelivered()
                mDelivery.postResponse(request, RpcResponse.parseResponse(request, response))
                cacheManager?.addToCache(cacheKey, response, System.currentTimeMillis())
                return
            }

        } catch (e: Exception) {
            request.logEvent(tag, "Error Occured During Request - ${e.message}")
            request.markDelivered()
            mDelivery.postError(request, RpcException(e.message))
            return
        }
    }

    /**
     * Forces this dispatcher to quit immediately. If any requests are still in the queue, they are
     * not guaranteed to be processed.
     */
    fun quit() {
        mQuit = true
        interrupt()
    }
}