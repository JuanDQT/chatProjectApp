package com.juan.chatproject

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.juan.chatproject.R.id.chatList
import com.juan.chatproject.chat.Message
import com.juan.chatproject.chat.User
import com.stfalcon.chatkit.commons.ImageLoader
import com.stfalcon.chatkit.commons.models.IUser
import com.stfalcon.chatkit.messages.MessagesListAdapter
import kotlinx.android.synthetic.main.activity_chat_window.*
import java.util.ArrayList


class ChatWindowActivity : AppCompatActivity() {

    val TAGGER = "TAGGER"
    var adapter: MessagesListAdapter<Message>? = null
    var cachedUsers: Array<String> = emptyArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_window)
        Common.connectWebSocket()
        // Observers
        LocalBroadcastManager.getInstance(this@ChatWindowActivity).registerReceiver(responseCapitulos, IntentFilter("GET_MESSAGES"))
        LocalBroadcastManager.getInstance(this@ChatWindowActivity).registerReceiver(getNewMessage, IntentFilter("INTENT_GET_SINGLE_MESSAGE"))

        adapter = MessagesListAdapter<Message>("JUANDQT", null)
        chatList.setAdapter(adapter)

    }

    fun postMessage(idUserFrom: String, message: String = "") {
        val m1 = Message()
        m1.mId = idUserFrom
        m1.mMessage = message

        var user: User? = null
        if (!cachedUsers.contains(idUserFrom)) {
            // Request userFrom data.. name, photo.. Emit
            //user =
        } else {
            user = User("1", "kaka", "avatar", true)
        }

        user = User("1", "kaka", "avatar", true)


        m1.mIuser = user

        adapter!!.addToStart(m1, true)

    }

    val responseCapitulos = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                postMessage(it.getStringExtra("DATA_TO_ACTIVITY"))
            }
        }
    }

    val getNewMessage = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                postMessage("KK", it.getStringExtra("MESSAGE_TO_ACTIVITY"))
            }
        }
    }

}
