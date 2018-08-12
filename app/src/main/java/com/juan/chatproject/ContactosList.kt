package com.juan.chatproject

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import com.juan.chatproject.chat.User
import com.squareup.picasso.Picasso
import com.thetechnocafe.gurleensethi.liteutils.RecyclerAdapterUtil
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_contactos_list.*
import kotlinx.android.synthetic.main.activity_main.*

class ContactosList : AppCompatActivity() {

    var allUsers: ArrayList<User> = arrayListOf()
    var adapter: RecyclerAdapterUtil<User>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contactos_list)
        rvList.layoutManager = LinearLayoutManager(this@ContactosList, LinearLayoutManager.VERTICAL, false)
//        loadChatContacts()
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                    if (it.count() > 0) {
                        Common.searchUsersByName(s.toString())
                    } else {
                        allUsers.clear()
                        adapter?.notifyDataSetChanged()
                    }
                }
            }
        })
        loadChatContacts()
        LocalBroadcastManager.getInstance(this@ContactosList).registerReceiver(getContacts, IntentFilter("SERCH_USERS_DATA"))

    }

    private val getContacts = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val list: ArrayList<User> = it.getSerializableExtra("users") as ArrayList<User>
                allUsers.clear()
                allUsers.addAll(list)
                adapter?.notifyDataSetChanged()
            }
        }
    }

    fun loadChatContacts() {

        adapter = RecyclerAdapterUtil.Builder(this, allUsers, R.layout.row_contacto)
                .viewsList(R.id.rowName, R.id.rowPic)
                .bindView { itemView, item, position, innerViews ->
                    val textView = innerViews[R.id.rowName] as TextView
                    val imageView = innerViews[R.id.rowPic] as CircleImageView
                    textView.text = allUsers[position].name
                    Picasso.with(this@ContactosList).load(allUsers[position].avatar).into(imageView)

                }
                .addClickListener { item, position ->
                    val builder = AlertDialog.Builder(this@ContactosList)
                    builder.setTitle(getString(R.string.informacion))
                    val view = LayoutInflater.from(this@ContactosList).inflate(R.layout.ad_contacto, null)
                    val tvName = view.findViewById<TextView>(R.id.tvName)
                    val btnAction = view.findViewById<Button>(R.id.btnAction)
                    val civPic = view.findViewById<CircleImageView>(R.id.civPic)
                    tvName.text = allUsers[position].name
                    btnAction.text = getString(R.string.enviar_solicitud)
                    Picasso.with(this@ContactosList).load(allUsers[position].avatar).into(civPic)
//
                    builder.setView(view)
                    builder.show()

                }
                .addLongClickListener { item, position ->
                    //Take action when item is long pressed
                }.build()
        rvList.adapter = adapter

    }

}
