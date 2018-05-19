package com.juan.chatproject

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.juan.chatproject.chat.User
import com.squareup.picasso.Picasso
import com.thetechnocafe.gurleensethi.liteutils.RecyclerAdapterUtil
import com.thetechnocafe.gurleensethi.liteutils.longToast
import com.thetechnocafe.gurleensethi.liteutils.shortToast
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    val TAGGER = "TAGGER"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        longToast("Hola: " + intent.getStringExtra("FROM"))

        rv.layoutManager = LinearLayoutManager(this@MainActivity)

        val allUsers = LocalDataBase().getAllUsers(exceptUser = intent.getStringExtra("FROM"))
        RecyclerAdapterUtil.Builder(this, allUsers, R.layout.row_user_chat)
                .viewsList(R.id.tvName, R.id.ivPic)
                .bindView { itemView, item, position, innerViews ->
                    val textView = innerViews[R.id.tvName] as TextView
                    val imageView = innerViews[R.id.ivPic] as CircleImageView
                    textView.text = allUsers[position].name
                    Picasso.with(this@MainActivity).load(allUsers[position].avatar).into(imageView)
                    imageView.borderWidth = 0

                }
                .addClickListener { item, position ->
                    Log.e(TAGGER, "Quieres hablar con: " + allUsers[position].name + "(" + allUsers[position].id + ")")
                    val intent = Intent(this@MainActivity, ChatWindowActivity::class.java)
                    intent.putExtra("TO",allUsers[position].id)
                    startActivity(intent)
                }
                .addLongClickListener { item, position ->
                    //Take action when item is long pressed
                }
                .into(rv)
        /*
        btnGoChat.setOnClickListener(){
            val intent = Intent(this@MainActivity, ChatWindowActivity::class.java)
            intent.putExtra("TO","EMULATOR")
            startActivity(intent)
        }
        */
    }

}
