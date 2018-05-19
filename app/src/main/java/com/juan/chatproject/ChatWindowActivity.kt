package com.juan.chatproject

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.Toast
import com.juan.chatproject.chat.Message
import com.juan.chatproject.chat.User
import com.squareup.picasso.Picasso
import com.stfalcon.chatkit.commons.ImageLoader
import com.stfalcon.chatkit.messages.MessagesListAdapter
import kotlinx.android.synthetic.main.activity_chat_window.*


class ChatWindowActivity : AppCompatActivity(), MessagesListAdapter.OnLoadMoreListener {


    val TAGGER = "TAGGER"
    var chatAdapter: MessagesListAdapter<Message>? = null
    var cachedUsers: Array<String> = emptyArray()
    var CLIENT_ID = ""
    var TARGET_ID = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_window)
        CLIENT_ID = intent.getStringExtra("FROM")
        TARGET_ID = intent.getStringExtra("TO")
        Common.connectWebSocket(CLIENT_ID, TARGET_ID)
        // Observers
        LocalBroadcastManager.getInstance(this@ChatWindowActivity).registerReceiver(responseCapitulos, IntentFilter("GET_MESSAGES"))
        LocalBroadcastManager.getInstance(this@ChatWindowActivity).registerReceiver(getNewMessage, IntentFilter("INTENT_GET_SINGLE_MESSAGE"))
        val urlImage = "http://lorempixel.com/g/200/200"
        val imageLoader = ImageLoader { imageView, url -> Picasso.with(this@ChatWindowActivity).load(urlImage).into(imageView) }

        chatAdapter = MessagesListAdapter<Message>("0", imageLoader)
        chatAdapter!!.setLoadMoreListener(this)
        chatList.setAdapter(chatAdapter)
        // Cargamos los antiguos
        chatAdapter!!.addToEnd(LocalDataBase().getOlderMessages(), true)

        input.setInputListener({ input ->
            chatAdapter!!.addToStart(Common.getMessageConstuctor(true, TARGET_ID, input.toString()), true)
            Common.addNewMessageToServer(input.toString(), TARGET_ID)
            true
        })



    }

    fun postMessage(inputMessage: Boolean, idUserFrom: String, message: String) {
        val m1 = Common.getMessageConstuctor(inputMessage, TARGET_ID, message)

        if (!cachedUsers.contains(idUserFrom)) {
            // Request userFrom data.. name, photo.. Emit
            //user =
        }

        chatAdapter!!.addToStart(m1, true)

    }

    val responseCapitulos = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                //postMessage(it.getStringExtra("DATA_TO_ACTIVITY"))
            }
        }
    }

    val getNewMessage = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                postMessage(false,TARGET_ID, it.getStringExtra("MESSAGE_TO_ACTIVITY"))
            }
        }
    }

    override fun onLoadMore(page: Int, totalItemsCount: Int) {
        Log.e(TAGGER, "PAGINA: " + page + " TOTAL: " + totalItemsCount)
        chatAdapter!!.addToEnd(LocalDataBase().getOlderMessages(), true)
    }
}
