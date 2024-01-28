package io.therms.rpc_client_kotlin.models

import com.google.gson.Gson
import io.therms.rpc_client_kotlin.requests.RpcRequest


/**
 * Encapsulates a parsed response for delivery.
 *
 * @param <T> Parsed type of this response
</T> */
class RpcResponse<T> {
    /** Callback interface for delivering parsed responses.  */
    interface Listener<T> {
        /** Called when a response is received.  */
        fun onResponse(response: T?)
        fun onError(error: RpcException?)
    }

    /** Parsed response, can be null; always null in the case of error.  */
    val result: T?

    val fromCache: Boolean

    /** Detailed error information if `errorCode != OK`.  */
    val error: RpcException?


    /** Returns whether this response is considered successful.  */
    val isSuccess: Boolean
        get() = error == null

    constructor(result: T?) {
        this.result = result
        this.fromCache = false
        this.error = null
    }

    constructor(result: T?, fromCache: Boolean) {
        this.result = result
        this.fromCache = fromCache
        this.error = null
    }


    constructor(error: RpcException) {
        this.result = null
        this.fromCache = false
        this.error = error
    }

    companion object {
        /** Returns a successful response containing the parsed result.  */
        @JvmStatic
        fun <T> success(result: T, fromCache: Boolean): RpcResponse<T> {
            return RpcResponse(result,fromCache)
        }


        /**
         * Returns a failed response containing the given error code and an optional localized message
         * displayed to the user.
         */
        @JvmStatic
        fun error(request: RpcRequest<*>, error: RpcException): RpcResponse<*> {
            return RpcResponse<Any>(error)
        }

        @JvmStatic
        fun parseResponse(request: RpcRequest<*>, body: String?, fromCache: Boolean = false): RpcResponse<*> {
            return success(Gson().fromJson(body,request.getReturnType()),fromCache)
        }
    }
}
