package io.therms.rpc_client_kotlin.transport

import android.content.Context
import android.os.Handler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import io.therms.rpc_client_kotlin.client.RpcClient
import io.therms.rpc_client_kotlin.queues.RequestQueue
import io.therms.rpc_client_kotlin.requests.ClientMessageRequest
import io.therms.rpc_client_kotlin.util.NetworkUtil
import io.therms.rpc_client_kotlin.requests.RpcRequest
import io.therms.rpc_client_kotlin.util.Parameters
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class WsTransport(val url: String, val headers: HashMap<String,String>, val context: Context, val useInBackground: Boolean, val listener: WsEventListener?,val queue: RequestQueue) : Transport,LifecycleObserver,
    WebSocketListener() {

    private val responseMap = ConcurrentHashMap<String,String>()

    private val NORMAL_CLOSURE_STATUS = 1000

    private val SOCKET_TIMEOUT_TIME = 4000L

    private val SOCKET_AUTO_BACKGROUND_CLOSE_TIME = 15000L

    private val SOCKET_AUTO_RECONNECT_TIME = 1000L

    private lateinit var request: Request
    private lateinit var client: OkHttpClient
    private lateinit var webSocket: WebSocket
    private var isWebSocketConnected = false
    private var isForeground = true

    init {
        initWebSocket()
    }

    private fun initWebSocket(){
        request = Request.Builder()
            .url(url)
            .apply {
                for(i in headers)
                    addHeader(i.key,i.value)
            }.build()

        client = OkHttpClient.Builder()
            .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS))
            .connectTimeout(10,TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .apply { queue.onOkhttpInitialize(this) }
            .build()
        webSocket = client.newWebSocket(request, this)
        Handler(context.mainLooper).post {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        }
    }


    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        setIsSocketConnected(true)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        if(NetworkUtil.hasServerMessage(text)){
            handleServerMessage(text)
        }else if(NetworkUtil.hasCorrelationId(text)) {
            updateSocketAuthenticationStatus(text)
            responseMap[NetworkUtil.getCorrelationId(text)] = text
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosing(webSocket, code, reason)
        setIsSocketConnected(false)
        setIsAlreadyAuthenticated(false)
        webSocket.close(NORMAL_CLOSURE_STATUS, null);
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
        setIsSocketConnected(false)
        setIsAlreadyAuthenticated(false)
    }


    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        setIsSocketConnected(false)
        setIsAlreadyAuthenticated(false)
    }

    /**
     * can be used to check status of socket connection
     */
    override fun isConnected(): Boolean {
        return isWebSocketConnected
    }

    /**
     * can be used to disconnect the socket connection
     */
    override fun disconnect() {
        try {
            if (isWebSocketConnected) {
                webSocket.cancel()
                isWebSocketConnected = false
                isWsAuthenticated = false
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    /**
     * can be used to send RPC Request to Server
     */
    override fun performRequest(request: RpcRequest<*>): String? {
        val correlationId = request.getCorrelationId()


        val requestStartTime = System.currentTimeMillis()
        var response: String? = null

        if(request.hasIdentity()){
            if(isAlreadyAuthenticated()){
                //Sending request via socket
                webSocket.send(request.createBody(true))
            }else {
                //note authenticated request correlationId so we can mark socket as authenticated on success response
                lastAuthCorrelationId = request.getCorrelationId()
                //Sending request via socket
                webSocket.send(request.createBody())
            }
        }else{
            //Sending request via socket
            webSocket.send(request.createBody())
        }

        //Waiting for Response til Response is received or Timeout
        while (System.currentTimeMillis() - requestStartTime <= SOCKET_TIMEOUT_TIME) {
            response = responseMap[correlationId]
            if(response != null) {
                responseMap.remove(correlationId)
                return response
            }else {
                Thread.sleep(400L)
            }
        }

        //When no response receive from socket between SOCKET_TIMEOUT_TIME Period
        response = responseMap[correlationId]
        if(response != null){
            responseMap.remove(correlationId)
            return response
        }else{
            return null
        }
    }

    /**
     * can be used to send Client Message to Server
     */
    fun performRequest(request: ClientMessageRequest){
        if(request.hasIdentity()){
            if(isAlreadyAuthenticated()){
                //Sending request via socket
                webSocket.send(request.createBody(true))
            }else {
                //Sending request via socket
                webSocket.send(request.createBody())
            }
        }else{
            //Sending request via socket
            webSocket.send(request.createBody())
        }
    }

    private fun handleServerMessage(jsonStr: String) {
        try{
            val json = JSONObject(jsonStr)
            val serverMessage = json.get(Parameters.SERVER_MESSAGE)
            if(serverMessage is JSONObject){
                listener?.onServerMessage(serverMessage)
            }else if(serverMessage is String){
                listener?.onServerMessage(serverMessage)
            }
        }catch (ex: Exception){
        }
    }

    fun reconnect(){
        if(!isWebSocketConnected){
            initWebSocket()
        }
    }

    private fun setIsSocketConnected(value: Boolean){
        this.isWebSocketConnected = value
        if(value){
            listener?.onWsConnected()
            stopAutoConnectJob()
        }else{
            listener?.onWsDisconnected()
            startAutoConnectJob()
        }
    }

    private var lastAuthCorrelationId: String = ""
    private var isWsAuthenticated: Boolean = false
    private var authenticationStatusChangeTimestamp:Long = 0

    private fun setIsAlreadyAuthenticated(value: Boolean){
        isWsAuthenticated = value
        authenticationStatusChangeTimestamp = Calendar.getInstance().timeInMillis
    }

    private fun isAlreadyAuthenticated(): Boolean{
        if(isWsAuthenticated){
            if(authenticationStatusChangeTimestamp != 0L && Calendar.getInstance().timeInMillis - authenticationStatusChangeTimestamp > 2000L){
                return true
            }
        }
        return false
    }

    private fun updateSocketAuthenticationStatus(response: String){
        val identityFromServer = NetworkUtil.getCorrelationId(response)
        if(!lastAuthCorrelationId.isNullOrEmpty() && !identityFromServer.isNullOrEmpty()){
            if(identityFromServer == lastAuthCorrelationId && NetworkUtil.isSuccess(response)){
                setIsAlreadyAuthenticated(true)
            }
        }
    }

    override fun clearIdentityCache(){
        setIsAlreadyAuthenticated(false)
    }

    private var timestamp: Long = Calendar.getInstance().timeInMillis
    private var autoCloseJob: Job? = null
    private var autoConnectJob: Job? = null

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun appInForeground(){
        isForeground = true
        timestamp = Calendar.getInstance().timeInMillis

        //closing existing job if it was already running as App is back to Foreground
        stopAutoCloseJob()

        //Reconnect socket if its already not connected
        startAutoConnectJob()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun appInBackground(){
        isForeground = false
        timestamp = Calendar.getInstance().timeInMillis

        //Closing existing auto connect job if it was already running
        stopAutoConnectJob()

        //closing existing job if it was already running
        stopAutoCloseJob()

        if(!useInBackground){
            //Starting a coroutine which will disconnect the socket after an interval
            startAutoCloseJob()
        }
    }

    private fun startAutoCloseJob(){
        autoCloseJob = GlobalScope.launch {
            delay(SOCKET_AUTO_BACKGROUND_CLOSE_TIME)
            if(Calendar.getInstance().timeInMillis - timestamp > SOCKET_AUTO_BACKGROUND_CLOSE_TIME){
                webSocket.cancel()
            }
            autoCloseJob = null
        }
    }

    private fun stopAutoCloseJob(){
        if(autoCloseJob?.isActive == true){
            autoCloseJob?.cancel()
            autoCloseJob = null
        }
    }

    private fun stopAutoConnectJob(){
        if(autoConnectJob?.isActive == true){
            autoConnectJob?.cancel()
            autoConnectJob = null
        }
    }

    private fun startAutoConnectJob(){
        if(autoConnectJob?.isActive == true) //Auto Connect Job is already running
            return

        if(isForeground && !isWebSocketConnected) {
            autoConnectJob = GlobalScope.launch {
                while (isForeground && !isWebSocketConnected){
                    //Initial Wait time before reconnecting
                    delay(SOCKET_AUTO_RECONNECT_TIME)
                    if(isForeground && !isWebSocketConnected) {
                        //Reconnect socket if its already not connected
                        reconnect()
                    }
                    //1x Wait time before another reconnection attempt
                    delay(SOCKET_AUTO_RECONNECT_TIME*1)
                }
                autoConnectJob = null
            }
        }
    }

}