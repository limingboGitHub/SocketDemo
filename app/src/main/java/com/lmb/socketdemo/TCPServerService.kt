package com.lmb.socketdemo

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class TCPServerService : Service() {
    val TAG = "tcpServer"

    private var mIsServiceDestroyed = false

    private val mDefinedMessages = arrayOf(
        "你好啊，哈哈",
        "请问你叫什么名字",
        "今天武汉天气不错啊，shy",
        "我可以和多个人同时聊天",
        "给你讲个笑话吧"
    )

    override fun onCreate() {
        Thread(TCPServer()).start()
        super.onCreate()
    }

    override fun onBind(intent: Intent): IBinder {
        return Binder()
    }

    override fun onDestroy() {
        mIsServiceDestroyed = true
        super.onDestroy()
    }

    inner class TCPServer : Runnable{
        override fun run() {
            val serverSocket = ServerSocket(8688)
            while (!mIsServiceDestroyed){
                val client = serverSocket.accept()
                Log.e(TAG, "accept")
                Thread{
                    responseClient(client)
                }.start()
            }
        }

        private fun responseClient(client:Socket){
            //接受客户端消息
            val bufferedReader = BufferedReader(InputStreamReader(client.getInputStream()))
            //向客户端发送消息
            val printWriter = PrintWriter(BufferedWriter(OutputStreamWriter(client.getOutputStream())),true)
            printWriter.println("欢迎来到聊天室！")
            while (!mIsServiceDestroyed){
                val readMessage = bufferedReader.readLine()
                Log.e(TAG, "msg from client:$readMessage")
                if (readMessage==null){
                    //客户端断开连接
                    break
                }
                val randomIndex = mDefinedMessages.indices.random()
                val messageToSend = mDefinedMessages[randomIndex]
                printWriter.println(messageToSend)
                Log.e(TAG,"send:$messageToSend")
            }
            Log.e(TAG, "client quit.")
            bufferedReader.close()
            printWriter.close()
            client.close()
        }
    }
}