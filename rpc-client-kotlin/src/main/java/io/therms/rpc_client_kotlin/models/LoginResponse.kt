package io.therms.rpc_client_kotlin.models

class LoginResponse : BaseResponse() {

    var data: Any? = null

    override fun toString(): String {
        return super.toString() + " | data: $data"
    }
}