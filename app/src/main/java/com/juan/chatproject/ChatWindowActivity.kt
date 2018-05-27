package com.juan.chatproject

import android.content.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View

import com.juan.chatproject.chat.Message
import com.squareup.picasso.Picasso
import com.stfalcon.chatkit.messages.MessagesListAdapter
import kotlinx.android.synthetic.main.activity_chat_window.*
import java.util.*
import kotlin.concurrent.schedule


class ChatWindowActivity : AppCompatActivity(), MessagesListAdapter.OnLoadMoreListener {

    val TAGGER = "TAGGER"
    var chatAdapter: MessagesListAdapter<Message>? = null
    var cachedUsers: Array<String> = emptyArray()
    var CLIENT_ID = ""
    var TARGET_ID = ""
    var sharedPreferences: SharedPreferences? = null
    var fromBack = false
    var DELAY_TIME_IS_TYPING = 10000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_window)
        sharedPreferences = getSharedPreferences(Common.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        CLIENT_ID = sharedPreferences!!.getString("FROM", "")
        TARGET_ID = intent.extras.getString("TO")

        val urlImage = LocalDataBase().getAllUsers(CLIENT_ID).filter { p -> p.id == TARGET_ID }.first().avatar
        Picasso.with(this@ChatWindowActivity).load(urlImage).into(profile_image)
        // Observers
        LocalBroadcastManager.getInstance(this@ChatWindowActivity).registerReceiver(getUserIsTyping, IntentFilter("INTENT_GET_USER_IS_TYPING"))
        LocalBroadcastManager.getInstance(this@ChatWindowActivity).registerReceiver(getNewMessage, IntentFilter("INTENT_GET_SINGLE_MESSAGE"))
//        val urlImage = "http://lorempixel.com/g/200/200"
//        val imageLoader = ImageLoader { imageView, url -> Picasso.with(this@ChatWindowActivity).load(urlImage).into(imageView) }

        chatAdapter = MessagesListAdapter<Message>(CLIENT_ID, null)
        chatAdapter!!.setLoadMoreListener(this)
        chatList.setAdapter(chatAdapter)
        // Cargamos los antiguos


//        chatAdapter!!.addToEnd(LocalDataBase().getOlderMessages(), true)

        input.inputEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Excluimos la tecla enter como evento escritura
                if (!s.isNullOrEmpty())
                    Common.notifyTyping(TARGET_ID)
            }
        })

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

    val getNewMessage = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (isCurrentChatUser(it.getStringExtra("ID_FROM_TO_ACTIVITY"), it.getStringExtra("ID_TO_TO_ACTIVITY"))) {
                    postMessage(clientFrom = TARGET_ID, clientTo = Common.getClientId(), message = it.getStringExtra("MESSAGE_TO_ACTIVITY"))
                    tvWritting.visibility = View.GONE
                }
            }
        }
    }

    val getUserIsTyping = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            intent?.let {
                if (isCurrentChatUser(it.getStringExtra("ID_FROM_TO_ACTIVITY"), it.getStringExtra("ID_TO_TO_ACTIVITY"))) {
                    if (tvWritting.visibility == View.VISIBLE)
                        return
                    tvWritting.visibility = View.VISIBLE
                    Timer().schedule(DELAY_TIME_IS_TYPING) {
                        runOnUiThread {
                            tvWritting.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }


    fun isCurrentChatUser(idFrom: String, idTo: String): Boolean {
        return TARGET_ID.equals(idFrom) && CLIENT_ID.equals(idTo)
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