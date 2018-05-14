package com.juan.chatproject

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter



class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Common.connectWebSocket()
//        Common.setActivityConext(this)
//        Common.setSocketStatus(true, true)
//        Common.addNewMessageToServer("dsds", input.toString().trim({ it <= ' ' }))

    }
}
