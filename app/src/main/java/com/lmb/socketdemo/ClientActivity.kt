package com.lmb.socketdemo

import android.content.Intent
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import com.lmb.socketdemo.databinding.ActivityClientBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*

class ClientActivity : AppCompatActivity(),View.OnClickListener{
    val TAG = "tcpServer"

    val MSG_RECEIVE_NEW_MSG = 1
    val MSG_SOCKET_CONNECT = 2

    lateinit var binding: ActivityClientBinding

    var mPrintWriter : PrintWriter? = null
    var mClientSocket : Socket? = null

    val handler = object : Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
             when(msg.what){
                 MSG_RECEIVE_NEW_MSG -> binding.messageTv.apply {
                     val oldText = text.toString()
                     text = oldText + msg.obj.toString()
                 }
                 MSG_SOCKET_CONNECT -> {
                    binding.sendBt.isEnabled = true
                 }
                 else ->{}
             }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.sendBt.setOnClickListener(this)

        //启动服务端的服务
        val service = Intent(this,TCPServerService::class.java)
        startService(service)
        //客户端开启线程连接服务端
        Thread{
            connectTCPServer()
        }.start()
    }

    override fun onDestroy() {
        mClientSocket?.shutdownInput()
        mClientSocket?.close()
        super.onDestroy()
    }

    override fun onClick(v: View?) {
        val msg = binding.messageEditText.text.toString()
        if (!TextUtils.isEmpty(msg) && mPrintWriter!=null){
            GlobalScope.launch(Dispatchers.IO) {
                mPrintWriter?.println(msg)
            }
            binding.messageEditText.setText("")
            val time = formatDataTime(System.currentTimeMillis())
            val showMessage = "self$time:$msg \n"
            binding.messageTv.apply {
                text = text.toString()+ showMessage
            }
        }
    }

    private fun formatDataTime(time:Long):String{
        return SimpleDateFormat("(HH:mm:ss)").format(Date(time))
    }

    private fun connectTCPServer(){
        var socket : Socket? = null
        while(socket==null) {
            //连接服务器
            try {
                socket = Socket("localhost",8688)
                mClientSocket = socket
                mPrintWriter = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())),true)
                handler.sendEmptyMessage(MSG_SOCKET_CONNECT)
                Log.e(TAG, "connect server success")
            }catch (e:IOException){
                SystemClock.sleep(1000)
                Log.e(TAG, "connect tcp failed ,retry..." )
            }
        }
        //接受服务器消息
        val bufferReader = BufferedReader(InputStreamReader(socket.getInputStream()))
        while (!isFinishing){
            val msg = bufferReader.readLine()
            Log.e(TAG, "receive:$msg")
            if (msg!=null){
                val time = formatDataTime(System.currentTimeMillis())
                val showMessage = "server $time :$msg \n"
                handler.sendMessage(Message.obtain(handler,MSG_RECEIVE_NEW_MSG,showMessage))
            }
        }
        Log.e(TAG, "quit...")
        mPrintWriter?.close()
        bufferReader.close()
        socket?.close()
    }
}