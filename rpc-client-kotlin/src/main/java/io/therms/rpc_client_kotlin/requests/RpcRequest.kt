package io.therms.rpc_client_kotlin.requests

import io.therms.rpc_client_kotlin.cache.CacheLookup
import io.therms.rpc_client_kotlin.models.RpcException
import io.therms.rpc_client_kotlin.models.RpcResponse
import io.therms.rpc_client_kotlin.queues.RequestQueue
import io.therms.rpc_client_kotlin.util.Parameters
import org.json.JSONObject
import java.util.*

class RpcRequest<T>(private val payload: JSONObject,private val returnType: Class<*>): Comparable<RpcRequest<T>> {
    private val correlationId: String = UUID.randomUUID().toString()
    private var mSequence: Int = 0
    private var mRequestQueue: RequestQueue? = null
    private var mListener: RpcResponse.Listener<T?>? = null

    private var mCacheLookup = CacheLookup.NoCache
    private var mCancelled = false
    private var mResponseDelivered = false

    fun getReturnType() = returnType

    fun createBody(): String {
        val json = JSONObject(payload.toString())
        json.put(Parameters.CORRELATION_ID, correlationId)
        return json.toString()
    }

    fun createBody(excludeIdentity: Boolean): String {
        val json = JSONObject(payload.toString())
        json.put(Parameters.CORRELATION_ID, correlationId)
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

    /**
     * Notifies the request queue that this request has finished (successfully or with error).
     *
     *
     * Also dumps all events from this request's event log; for debugging.
     */
    fun finish(tag: String?) {
        mRequestQueue?.finishRequest(this)
    }



    fun setRequestQueue(queue: RequestQueue) = apply { this.mRequestQueue = queue }
    fun listener(listener: RpcResponse.Listener<T?>) = apply { this.mListener = listener }
    fun setSequence(value: Int) = apply { this.mSequence = value }
    fun cacheLookup(value: CacheLookup) = apply { this.mCacheLookup = value }
    fun cancelled(value: Boolean) = apply { this.mCancelled = value }
    fun markDelivered() = apply { this.mResponseDelivered = true }

    fun getCorrelationId() = correlationId
    fun getRequestQueue() = mRequestQueue
    fun getListener() = mListener
    fun cacheLookup() = mCacheLookup
    fun isCancelled() = mCancelled
    fun responseDelivered() = mResponseDelivered


    override fun compareTo(other: RpcRequest<T>): Int {
        return this.mSequence.compareTo(other.mSequence)
    }


    fun deliverResponse(response: T?){
        mListener?.onResponse(response)
    }

    fun deliverError(error: RpcException?) {
        mListener?.onError(error)
    }

    fun logEvent(dispatcher: String, msg: String){
        var output =  "Req# $mSequence ($dispatcher): $msg"
        mRequestQueue?.logEvent(output)
    }

    companion object{
    }
}