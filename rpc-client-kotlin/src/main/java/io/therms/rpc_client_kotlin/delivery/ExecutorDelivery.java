package io.therms.rpc_client_kotlin.delivery;

import android.os.Handler;

import io.therms.rpc_client_kotlin.models.RpcException;
import io.therms.rpc_client_kotlin.requests.RpcRequest;
import io.therms.rpc_client_kotlin.models.RpcResponse;

import java.util.concurrent.Executor;

/** Delivers responses and errors. */
public class ExecutorDelivery implements ResponseDelivery {
    /** Used for posting responses, typically to the main thread. */
    private final Executor mResponsePoster;

    /**
     * Creates a new response delivery interface.
     *
     * @param handler {@link Handler} to post responses on
     */
    public ExecutorDelivery(final Handler handler) {
        // Make an Executor that just wraps the handler.
        mResponsePoster =
                new Executor() {
                    @Override
                    public void execute(Runnable command) {
                        handler.post(command);
                    }
                };
    }

    /**
     * Creates a new response delivery interface, mockable version for testing.
     *
     * @param executor For running delivery tasks
     */
    public ExecutorDelivery(Executor executor) {
        mResponsePoster = executor;
    }

    @Override
    public void postResponse(RpcRequest request, RpcResponse<?> response) {
        postResponse(request, response, null);
    }

    @Override
    public void postResponse(RpcRequest request, RpcResponse<?> response, Runnable runnable) {
        request.markDelivered();
        //request.addMarker("post-response");
        mResponsePoster.execute(new ResponseDeliveryRunnable(request, response, runnable));
    }

    @Override
    public void postError(RpcRequest request, RpcException error) {
        //request.addMarker("post-error");
        RpcResponse<?> response = RpcResponse.error(request, error);
        mResponsePoster.execute(new ResponseDeliveryRunnable(request, response, null));
    }

    /** A Runnable used for delivering network responses to a listener on the main thread. */
    @SuppressWarnings("rawtypes")
    private static class ResponseDeliveryRunnable implements Runnable {
        private final RpcRequest mRequest;
        private final RpcResponse mResponse;
        private final Runnable mRunnable;

        public ResponseDeliveryRunnable(RpcRequest request, RpcResponse response, Runnable runnable) {
            mRequest = request;
            mResponse = response;
            mRunnable = runnable;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            // NOTE: If cancel() is called off the thread that we're currently running in (by
            // default, the main thread), we cannot guarantee that deliverResponse()/deliverError()
            // won't be called, since it may be canceled after we check isCanceled() but before we
            // deliver the response. Apps concerned about this guarantee must either call cancel()
            // from the same thread or implement their own guarantee about not invoking their
            // listener after cancel() has been called.

            // If this request has canceled, finish it and don't deliver.
            if (mRequest.isCancelled()) {
                mRequest.finish("canceled-at-delivery");
                return;
            }

            // Deliver a normal response or error, depending.
            if (mResponse.isSuccess()) {
                mRequest.deliverResponse(mResponse.getResult());
                //Notifying request queue about request is finished with success or error so it can handle additional cases
                mRequest.getRequestQueue().onRequestFinished(mRequest,mResponse.getResult());
            } else {
                mRequest.deliverError(mResponse.getError());
                //Notifying request queue about request is finished with success or error so it can handle additional cases
                mRequest.getRequestQueue().onRequestFinished(mRequest,mResponse.getError());
            }

            mRequest.finish("done");

            // If we have been provided a post-delivery runnable, run it.
            if (mRunnable != null) {
                mRunnable.run();
            }
        }
    }
}
