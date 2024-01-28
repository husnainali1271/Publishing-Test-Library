package io.therms.rpc_client_kotlin.requests

import io.therms.rpc_client_kotlin.models.BaseResponse
import org.json.JSONObject

interface ErrorInterceptor {
    fun intercept(request: RpcRequest<*>, response: BaseResponse)
}