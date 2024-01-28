package io.therms.rpc_client_kotlin.delivery;

import io.therms.rpc_client_kotlin.models.RpcException;
import io.therms.rpc_client_kotlin.requests.RpcRequest;
import io.therms.rpc_client_kotlin.models.RpcResponse;

public interface ResponseDelivery {
    /** Parses a response from the network or cache and delivers it. */
    void postResponse(RpcRequest request, RpcResponse<?> response);

    /**
     * Parses a response from the network or cache and delivers it. The provided Runnable will be
     * executed after delivery.
     */
    void postResponse(RpcRequest request, RpcResponse<?> response, Runnable runnable);

    /** Posts an error for the given request. */
    void postError(RpcRequest request, RpcException error);
}
