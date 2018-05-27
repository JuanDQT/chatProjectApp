package com.juan.chatproject

import android.content.*
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.widget.TextView
import com.juan.chatproject.chat.User
import com.squareup.picasso.Picasso
import com.thetechnocafe.gurleensethi.liteutils.RecyclerAdapterUtil
import com.thetechnocafe.gurleensethi.liteutils.longToast
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    val TAGGER = "TAGGER"
    var allUsers: List<User>? = null
    val GO_CHAT_WINDOW = "GO_CHAT_WINDOW"
    var sharedPreferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPreferences = getSharedPreferences(Common.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        startService(Intent(this@MainActivity, MyService::class.java))
        rv.layoutManager = LinearLayoutManager(this@MainActivity)
        intent?.let {
            Log.e(TAGGER, "TIene gochat 0")
            it.extras?.let {
                Log.e(TAGGER, "TIene gochat 1")
                if (it.getBoolean(GO_CHAT_WINDOW)) {
                    Log.e(TAGGER, "TIene gochat 2")

                    val intent = Intent(this@MainActivity, ChatWindowActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    intent.putExtra("TO", it.getString("TO"))
                    startActivity(intent)
                    Log.e(TAGGER, "TIene gochat")


                    return
                }
            }
        }
    }

    val getNewMessage = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val messsage = it.getStringExtra("MESSAGE_TO_ACTIVITY")
                val position = allUsers!!.indexOf(allUsers!!.filter { p -> p.id == it.getStringExtra("ID_FROM_TO_ACTIVITY") }.first())
                Log.e(TAGGER, "Posicion es: " + position)
                rv.findViewHolderForAdapterPosition(position).let {
                    val tvDescription= it.itemView.findViewById<TextView>(R.id.tvDescription)
                    tvDescription.text = messsage
                    it.itemView.setBackgroundColor(Color.parseColor("#667fb7"))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Common.setAppForeground(true)
        Common.setActivityInMain(true)

        longToast("Hola: " + sharedPreferences!!.getString("FROM", ""))
//        longToast("Hola: " + intent.getStringExtra("FROM"))
        allUsers = LocalDataBase().getAllUsers(exceptUser = sharedPreferences!!.getString("FROM", ""))
        RecyclerAdapterUtil.Builder(this, allUsers!!, R.layout.row_user_chat)
                .viewsList(R.id.tvName, R.id.ivPic)
                .bindView { itemView, item, position, innerViews ->
                    val textView = innerViews[R.id.tvName] as TextView
                    val imageView = innerViews[R.id.ivPic] as CircleImageView
                    textView.text = allUsers!![position].name
                    Picasso.with(this@MainActivity).load(allUsers!![position].avatar).into(imageView)
                    imageView.borderWidth = 0

                }
                .addClickListener { item, position ->
                    Log.e(TAGGER, "Quieres hablar con: " + allUsers!![position].name + "(" + allUsers!![position].id + ")")
                    val intent = Intent(this@MainActivity, ChatWindowActivity::class.java)
                    intent.putExtra("TO", allUsers!![position].id)
                    startActivity(intent)
                }
                .addLongClickListener { item, position ->
                    //Take action when item is long pressed
                }
                .into(rv)
        LocalBroadcastManager.getInstance(this@MainActivity).registerReceiver(getNewMessage, IntentFilter("INTENT_GET_SINGLE_MESSAGE"))
    }

    override fun onPause() {
        super.onPause()
        Common.setAppForeground(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        Common.setAppForeground(false)
        Common.setActivityInMain(false)
    }
}
