package io.therms.rpc_client_kotlin.queues

import android.os.Handler
import android.os.Looper
import io.therms.rpc_client_kotlin.cache.CacheManager
import io.therms.rpc_client_kotlin.delivery.ExecutorDelivery
import io.therms.rpc_client_kotlin.dispatchers.RequestDispatcher
import io.therms.rpc_client_kotlin.client.RpcClient
import io.therms.rpc_client_kotlin.models.BaseResponse
import io.therms.rpc_client_kotlin.requests.ClientMessageRequest
import io.therms.rpc_client_kotlin.requests.RpcRequest
import io.therms.rpc_client_kotlin.transport.HttpTransport
import io.therms.rpc_client_kotlin.transport.WsTransport
import okhttp3.OkHttpClient
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

private val THREAD_POOL_SIZE = 4

class RequestQueue(val client: RpcClient) {

    var wsTransport: WsTransport? = null
    var httpTransport: HttpTransport? = null
    var cacheManager: CacheManager? = null

    init {
        if (client.getWebSocketUrl() != null)
            wsTransport = WsTransport(client.getWebSocketUrl()!!,client.getHeaders(),client.context,client.useWebSocketInBackground(),client,this)
        if (client.getHttpUrl() != null)
            httpTransport = HttpTransport(client.getHttpUrl()!!, client.getHeaders(), this)
        if(client.shouldCacheResponses()){
            cacheManager = CacheManager(client.context,client.getCacheMaxAgeMs())
        }
    }


    /** Used for generating monotonically-increasing sequence numbers for requests.  */
    private val mSequenceGenerator = AtomicInteger()

    /**
     * The set of all requests currently being processed by this RequestQueue. A Request will be in
     * this set if it is waiting in any queue or currently being processed by any dispatcher.
     */
    private val mCurrentRequests: MutableSet<RpcRequest<*>> = HashSet()

    /** The queue of requests that are actually going out to the network.  */
    private val mBlockingQueue: PriorityBlockingQueue<RpcRequest<*>> =
        PriorityBlockingQueue<RpcRequest<*>>()

    /** The network dispatchers. */
    var dispatchers: Array<RequestDispatcher?> = arrayOfNulls(size = THREAD_POOL_SIZE)


    /**
     * Adds a Request to the dispatch queue.
     */
    fun add(request: RpcRequest<*>): RpcRequest<*> {
        // Tag the request as belonging to this queue and add it to the set of current requests.
        request.setRequestQueue(this)
        synchronized(mCurrentRequests) {
            mCurrentRequests.add(request)
        }

        // Process requests in the order they are added.
        request.setSequence(mSequenceGenerator.incrementAndGet())
        beginRequest(request)
        return request
    }


    private fun beginRequest(request: RpcRequest<*>) {
        mBlockingQueue.add(request)
    }

    fun finishRequest(request: RpcRequest<*>) {
        // Remove from the set of requests currently being processed.
        synchronized(mCurrentRequests) {
            mCurrentRequests.remove(request)
        }
    }

    /** Starts the dispatchers in this queue.  */
    fun start() {
        stop() // Make sure any currently running dispatchers are stopped.
        // Create network dispatchers (and corresponding threads) up to the pool size.
        for (i in dispatchers.indices) {
            val networkDispatcher = RequestDispatcher(
                "Dispatcher# $i",
                mBlockingQueue,
                ExecutorDelivery(Handler(Looper.getMainLooper())),
                wsTransport,
                httpTransport,
                cacheManager
            )
            dispatchers[i] = networkDispatcher
            networkDispatcher.start()
        }
    }

    /** Stops the cache and network dispatchers.  */
    fun stop() {
        for (dispatcher in dispatchers) {
            dispatcher?.quit()
        }
    }

    fun disconnect(){
        wsTransport?.disconnect()
        httpTransport?.disconnect()
    }

    fun clearIdentityCache(){
        wsTransport?.clearIdentityCache()
        httpTransport?.clearIdentityCache()
    }

    fun isWebSocketConnected(): Boolean{
        return wsTransport?.isConnected() == true
    }

    fun onRequestFinished(request: RpcRequest<*>, response: Any?){
        if(response is BaseResponse && !response.success) {
            client?.getErrorInterceptor()?.intercept(request,response)
        }
    }

    fun logEvent(msg: String){
        client.logEvent(msg)
    }

    fun onOkhttpInitialize(builder: OkHttpClient.Builder){
        client.getEventListener()?.onOkhttpInitialize(builder)
    }

    fun sendClientMessageToServer(request: ClientMessageRequest){
        if(wsTransport?.isConnected() == true){
            wsTransport?.performRequest(request)
        }
    }

}