package com.juan.chatproject

import android.content.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.juan.chatproject.chat.Message
import com.squareup.picasso.Picasso
import com.stfalcon.chatkit.messages.MessagesListAdapter
import kotlinx.android.synthetic.main.activity_chat_window.*


class ChatWindowActivity : AppCompatActivity(), MessagesListAdapter.OnLoadMoreListener {

    val TAGGER = "TAGGER"
    var chatAdapter: MessagesListAdapter<Message>? = null
    var cachedUsers: Array<String> = emptyArray()
    var CLIENT_ID = ""
    var TARGET_ID = ""
    var sharedPreferences: SharedPreferences? = null
    var fromBack = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_window)
        sharedPreferences = getSharedPreferences(Common.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        CLIENT_ID = sharedPreferences!!.getString("FROM", "")
//        TARGET_ID = intent.getStringExtra("TO")
        TARGET_ID = intent.extras.getString("TO")

        val urlImage = LocalDataBase().getAllUsers(CLIENT_ID).filter { p -> p.id ==  TARGET_ID}.first().avatar
        Picasso.with(this@ChatWindowActivity).load(urlImage).into(profile_image )
        // Observers
        LocalBroadcastManager.getInstance(this@ChatWindowActivity).registerReceiver(responseCapitulos, IntentFilter("GET_MESSAGES"))
        LocalBroadcastManager.getInstance(this@ChatWindowActivity).registerReceiver(getNewMessage, IntentFilter("INTENT_GET_SINGLE_MESSAGE"))
//        val urlImage = "http://lorempixel.com/g/200/200"
//        val imageLoader = ImageLoader { imageView, url -> Picasso.with(this@ChatWindowActivity).load(urlImage).into(imageView) }

        chatAdapter = MessagesListAdapter<Message>(CLIENT_ID, null)
        chatAdapter!!.setLoadMoreListener(this)
        chatList.setAdapter(chatAdapter)
        // Cargamos los antiguos


//        chatAdapter!!.addToEnd(LocalDataBase().getOlderMessages(), true)

        input.setInputListener({ input ->
            chatAdapter!!.addToStart(Common.getMessageConstuctor(Common.getClientId(), TARGET_ID, input.toString()), true)
            Common.addNewMessageToServer(input.toString(), TARGET_ID)
            true
        })



    }

    // No siempre el clientFrom es el owner.
    fun postMessage(clientFrom: String, clientTo: String, message: String) {
        val m1 = Common.getMessageConstuctor(clientFrom, clientTo, message)

        if (!cachedUsers.contains("")) {
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
                postMessage(clientFrom = TARGET_ID, clientTo = Common.getClientId(), message = it.getStringExtra("MESSAGE_TO_ACTIVITY"))
            }
        }
    }

    override fun onLoadMore(page: Int, totalItemsCount: Int) {
        Log.e(TAGGER, "PAGINA: " + page + " TOTAL: " + totalItemsCount)
//        chatAdapter!!.addToEnd(LocalDataBase().getOlderMessages(), true)
    }

    override fun onResume() {
        super.onResume()
        Common.setAppForeground(true)
    }

    override fun onPause() {
        super.onPause()
        Common.setAppForeground(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!fromBack) {
            Common.setAppForeground(false)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        fromBack = true
    }
}
