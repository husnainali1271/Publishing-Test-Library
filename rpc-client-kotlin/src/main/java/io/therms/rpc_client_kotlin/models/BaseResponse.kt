package io.therms.rpc_client_kotlin.models

open class BaseResponse {
    var correlationId: String? = null
    var success: Boolean = false
    var code: Int = 0
    var message: String = ""
    var fromCache: Boolean? = false

    override fun toString(): String {
        return  "correlationId: $correlationId | fromCache: $fromCache | success: $success | code: $code | mesage: $message"
    }
}