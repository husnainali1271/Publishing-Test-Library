package io.therms.rpc_client_kotlin.requests

import org.json.JSONObject

interface BodyInterceptor {
    fun intercept(body: JSONObject)
}