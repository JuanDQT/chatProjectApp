package com.juan.chatproject

import android.content.*
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.widget.TextView
import com.juan.chatproject.chat.User
import com.squareup.picasso.Picasso
import com.thetechnocafe.gurleensethi.liteutils.RecyclerAdapterUtil
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_main.*
import android.view.MenuItem
import android.view.View
import io.realm.Realm
import io.realm.annotations.Ignore
import android.content.IntentFilter

class MainActivity : AppCompatActivity(), NetworkStateReceiver.NetworkStateReceiverListener, View.OnClickListener {

    val TAGGER = "TAGGER"
    var allUsers: ArrayList<User> = arrayListOf()
    val GO_CHAT_WINDOW = "GO_CHAT_WINDOW"
    var sharedPreferences: SharedPreferences? = null
    var realm: Realm? = null
    var adapter: RecyclerAdapterUtil<User>? = null

    private var networkStateReceiver: NetworkStateReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        networkStateReceiver = NetworkStateReceiver(this@MainActivity)
        networkStateReceiver?.addListener(this)
        this.registerReceiver(networkStateReceiver, IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION))

        realm = Realm.getDefaultInstance()
        setContentView(R.layout.activity_main)
        sharedPreferences = getSharedPreferences(Common.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        startService(Intent(this@MainActivity, MyService::class.java))
        @Ignore
        rv.layoutManager = LinearLayoutManager(this@MainActivity)
        intent?.let {
            it.extras?.let {
                if (it.getBoolean(GO_CHAT_WINDOW)) {
                    val intent = Intent(this@MainActivity, ChatWindowActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    intent.putExtra("TO", it.getString("TO"))
                    startActivity(intent)
                    return
                }
            }
        }
        setupAdapter()
        loadContacts()

        ibSearch.setOnClickListener(this)
        ibContacts.setOnClickListener(this)
    }

    val getNewMessage = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if (Common.isActivityInMain()) {
                intent?.let { int ->
                    val chats = int.getSerializableExtra("DATA") as HashMap<String, List<String>>
                        Log.e(TAGGER, "Nos llego data")

                    if (chats.count() > 0) {
                        for (chat in chats) {
                            markChatMessage(chat.key, chat.value[0], chat.value[1].toInt())
                        }
                    }
                }
            }
        }
    }

    fun markChatMessage(clientID: String, message: String, counter: Int = 0) {
        // TODO: Cuando se carguen los contactos de la bbdd automaticamente ya no hara falta ese if
        if (allUsers.isEmpty()) {
            return
        }
        val position = allUsers.indexOf(allUsers.filter { p -> p.id == clientID }.first())
        rv.findViewHolderForAdapterPosition(position).let { rv ->

            rv?.let {
                val tvDescription = rv.itemView.findViewById<TextView>(R.id.tvDescription)
                val tvCounter = rv.itemView.findViewById<TextView>(R.id.tvNumber)
                tvDescription.text = message

                if (counter > 0) {
                    rv.itemView.setBackgroundColor(Color.parseColor(getColorByNumberMessages(counter)))
                    tvCounter.text = if (counter > 3) "*" else "$counter"
                    tvCounter.visibility = View.VISIBLE
                } else {
                    rv.itemView.setBackgroundColor(Color.parseColor("#dddddd"))
                    tvCounter.text = ""
                    tvCounter.visibility = View.GONE
                }
            }
        }
    }

    fun getColorByNumberMessages(number: Int): String {
        return when (number) {
            1 -> "#92A6D4"
            2 -> "#667FB7"
            3 -> "#45609D"
            4 -> "#2C488A"
            else -> "#2C488A"
        }
    }

    val getUsers = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadContacts()
        }
    }


    fun loadContacts() {
        realm?.let {
            val data = LocalDataBase.getAllUsers(it) as ArrayList<User>
            allUsers.clear()
            allUsers.addAll(data)
            adapter?.notifyDataSetChanged()
        }
    }

    override fun onResume() {
        super.onResume()
        Common.setAppForeground(true)
        Common.setActivityInMain(true)
        LocalBroadcastManager.getInstance(this@MainActivity).registerReceiver(getNewMessage, IntentFilter("INTENT_GET_SINGLE_ROW_MESSAGE"))
        LocalBroadcastManager.getInstance(this@MainActivity).registerReceiver(getUsers, IntentFilter("MAIN_ACTIVITY_GET_CONTACTS"))

        val realm = Realm.getDefaultInstance()
        // De momento, solo recargaremos los chats que hayan sufrido cambios en la mensajeria. No todos.

        val chats = LocalDataBase.getLastMessage(realm, realm.where(User::class.java).equalTo("banned", false).notEqualTo("id", Common.getClientId()).findAll())

        Log.e(TAGGER, "Mesajes nuevos total: " + chats)


        // Se podria prescindir?
        if (chats.count() > 0) {
            for (chat in chats) {
                markChatMessage(chat.key, chat.value[0], chat.value[1].toInt())
            }
        }

        realm.close()
    }

    fun setupAdapter() {

        adapter = RecyclerAdapterUtil.Builder(this, allUsers, R.layout.row_user_chat)
                .viewsList(R.id.tvName, R.id.ivPic)
                .bindView { itemView, item, position, innerViews ->
                    val textView = innerViews[R.id.tvName] as TextView
                    val imageView = innerViews[R.id.ivPic] as CircleImageView
                    textView.text = allUsers[position].name
                    Picasso.with(this@MainActivity).load(allUsers[position].avatar).into(imageView)
                    imageView.borderWidth = if (!allUsers[position].isOnline) 0 else 5

                }
                .addClickListener { item, position ->
                    Log.e(TAGGER, "Quieres hablar con: " + allUsers[position].name + "(" + allUsers[position].id + ")")
                    val intent = Intent(this@MainActivity, ChatWindowActivity::class.java)
                    intent.putExtra("TO", allUsers[position].id)
                    startActivity(intent)
                }
                .addLongClickListener { item, position ->
                    //Take action when item is long pressed
                }.build()

        rv.adapter = adapter

    }

    override fun onPause() {
        super.onPause()
        Common.setAppForeground(false)
        LocalBroadcastManager.getInstance(this@MainActivity).unregisterReceiver(getNewMessage)
    }

    override fun onDestroy() {
        super.onDestroy()
        realm?.close()
        Common.setAppForeground(false)
        Common.setActivityInMain(false)
        networkStateReceiver?.removeListener(this);
        this.unregisterReceiver(networkStateReceiver);
    }

    override fun onNetworkAvailable() {
        // TODO: Quizas en un futuro enviarlos todos de golpe y no 1 x 1
        //Common.sendAllMessagesPending(realm)
        //Log.d(TAGGER, "Se ha recuperado la conexion a la red")
    }

    override fun onNetworkUnavailable() {
        Log.d(TAGGER, "Se ha perdido la conexion a la red")
    }

    override fun onClick(v: View?) {
        var intent = Intent(this@MainActivity, ContactosActivity::class.java)
        when (v?.id) {
            R.id.ibSearch -> {
                intent = Intent(this@MainActivity, ContactosList::class.java)
            }
        }
        startActivity(intent)
    }
}
