package io.therms.rpc_client_kotlin.transport

import io.therms.rpc_client_kotlin.queues.RequestQueue
import io.therms.rpc_client_kotlin.requests.RpcRequest
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit


class HttpTransport(val url: String, val headers: HashMap<String,String>, val queue: RequestQueue) : Transport {
    val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
    var client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .apply { queue.onOkhttpInitialize(this) }
        .build()

    override fun isConnected(): Boolean {
        return true
    }

    override fun disconnect() {
    }

    override fun performRequest(request: RpcRequest<*>): String? {
        val response = client.newCall(createHttpRequest(request)).execute()
        return response.body?.string()
    }

    private fun createHttpRequest(rpcRequest: RpcRequest<*>): Request {
        val body: RequestBody = rpcRequest.createBody().toRequestBody(MEDIA_TYPE_JSON)
        val request: Request = Request.Builder()
            .url(url)
            .apply {
                for(i in headers)
                    addHeader(i.key,i.value)
            }
            .post(body)
            .build()
        return request
    }

    override fun clearIdentityCache() {
    }
}