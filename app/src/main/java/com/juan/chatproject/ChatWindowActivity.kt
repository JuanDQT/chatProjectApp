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
import io.realm.Realm
import java.util.*
import kotlin.concurrent.schedule
import kotlinx.android.synthetic.main.activity_chat_window.*


class ChatWindowActivity : AppCompatActivity(), MessagesListAdapter.OnLoadMoreListener {

    val TAGGER = "TAGGER"
    var chatAdapter: MessagesListAdapter<Message>? = null
    var cachedUsers: Array<String> = emptyArray()
    var CLIENT_ID = ""
    var TARGET_ID = ""
    var sharedPreferences: SharedPreferences? = null
    var fromBack = false
    var DELAY_TIME_IS_TYPING = 5000L
    var realm: Realm? = null
    var lastMessageId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        realm = Realm.getDefaultInstance()
        setContentView(R.layout.activity_chat_window)
        sharedPreferences = getSharedPreferences(Common.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        CLIENT_ID = sharedPreferences!!.getString("FROM", "")
        TARGET_ID = intent.extras.getString("TO")

        val urlImage = LocalDataBase.getAllUsers(realm = realm!!, exceptUser = CLIENT_ID).filter { p -> p.id == TARGET_ID }.first().avatar
        Picasso.with(this@ChatWindowActivity).load(urlImage).into(profile_image)
        // Observers
        LocalBroadcastManager.getInstance(this@ChatWindowActivity).registerReceiver(getUserIsTyping, IntentFilter("INTENT_GET_USER_IS_TYPING"))
        LocalBroadcastManager.getInstance(this@ChatWindowActivity).registerReceiver(getNewMessage, IntentFilter("INTENT_GET_SINGLE_MESSAGE"))

        chatAdapter = MessagesListAdapter<Message>(CLIENT_ID, null)
        chatAdapter!!.setLoadMoreListener(this)
        chatList.setAdapter(chatAdapter)
        // Cargamos los antiguos
        val olderMessages = LocalDataBase.access.getOlderMessages(realm!!, TARGET_ID)
        checkIfNotReaded(realm!!, olderMessages)
        chatAdapter!!.addToEnd(olderMessages, true)

        if (!olderMessages.isEmpty())
            lastMessageId = olderMessages.last().id

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

        input.setInputListener { input ->
            realm?.let {
                val message = Message.Static.getMessageConstuctor(realm = it, idServidor = 0,clientFrom = Common.getClientId(), clientTo = TARGET_ID, message = input.toString(), fechaRecibido = null)
                Common.addNewMessageToServer(message)
                chatAdapter!!.addToStart(message, true)
            }
            true
        }


    }

    // No siempre el clientFrom es el owner.
    fun postMessage(message: Message) {
        if (!cachedUsers.contains("")) {
            // Request userFrom data.. name, photo.. Emit
            //user =
        }

        chatAdapter!!.addToStart(message, true)
    }

    val getNewMessage = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val idMessage: Int = it.getIntExtra("MESSAGE_ID", 0)

                realm?.let { r ->
                    val mm = r.where(Message::class.java).equalTo("id", idMessage).findFirstAsync()

                    mm.addChangeListener<Message> { m ->

                        if (m.isValid) {
                            if (m.userFrom!!.id!!.equals(TARGET_ID)) {
                                postMessage(m)
                                tvWritting.visibility = View.GONE
                                Log.e(TAGGER, "Mensaje entrada, guardamos en memoria")
                                r.executeTransaction {realm ->
                                    m.setFechaLectura(Date())
                                    realm.copyToRealmOrUpdate(m)
                                }
                                // Notificamos la fecha lectura a los mensajes entrantes
                                if (m.getIdServidor() != 0 && Common.getClientId() != m.userFrom!!.id)
                                    Common.notifyMessageReaded(m.getIdServidor(), Date())
                            } else {
                                Log.e(TAGGER, "Recibodo mensaje en otra ventana")
                            }
                        }

                    }

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
        return TARGET_ID.equals(idFrom)
//        return TARGET_ID.equals(idFrom) && CLIENT_ID.equals(idTo)
    }


    override fun onLoadMore(page: Int, totalItemsCount: Int) {
        if (totalItemsCount > 10) {
            Log.e(TAGGER, "PAGINA: " + page + " TOTAL: " + totalItemsCount)
            realm?.let {
                val olderMessages = LocalDataBase.access.getOlderMessages(it, TARGET_ID, lastMessageId)

                if (!olderMessages.isEmpty()) {
                    lastMessageId = olderMessages.first().id
                    chatAdapter!!.addToEnd(olderMessages, true)

                    checkIfNotReaded(it, olderMessages)
                }
            }
        }
    }

    // Nada mas entrar, te asigna una hora de lectura
    fun checkIfNotReaded(realm: Realm, messages: List<Message>) {

        realm.executeTransaction {
            for (m in messages) {
                // Actualmente ya te viene filtrados el userfrom
                if (m.getFechaLectura() == null && m.userFrom!!.id!! != Common.getClientId()) {
                    m.setFechaLectura(Date())
                    it.insertOrUpdate(m)
                    // Se supone que si te llegan mensajes, es porque ya tienen idServidor
                    Common.notifyMessageReaded(m.getIdServidor(), Date())
                }
            }

        }

    }

    override fun onResume() {
        super.onResume()
        Common.setAppForeground(true)
        Common.setActivityInMain(false)
    }

    override fun onPause() {
        super.onPause()
        Common.setAppForeground(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        realm?.close()
        if (!fromBack) {
            Common.setAppForeground(false)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        LocalBroadcastManager.getInstance(this@ChatWindowActivity).unregisterReceiver(getNewMessage)
        LocalBroadcastManager.getInstance(this@ChatWindowActivity).unregisterReceiver(getUserIsTyping)
        fromBack = true
    }
}