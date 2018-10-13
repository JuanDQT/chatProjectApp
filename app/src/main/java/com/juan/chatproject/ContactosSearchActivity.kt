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
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import com.juan.chatproject.chat.User
import com.squareup.picasso.Picasso
import com.thetechnocafe.gurleensethi.liteutils.RecyclerAdapterUtil
import de.hdodenhof.circleimageview.CircleImageView
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_contactos_list.*

class ContactosSearchActivity : AppCompatActivity() {

    var allUsers: ArrayList<User> = arrayListOf()
    var adapter: RecyclerAdapterUtil<User>? = null
    var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contactos_list)

        setUpContactsAdapter()
        rvList.layoutManager = LinearLayoutManager(this@ContactosSearchActivity, LinearLayoutManager.VERTICAL, false)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Do nothing
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing
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
    }

    fun updateContactList() {
        Realm.getDefaultInstance().use { r ->
            allUsers.clear()
            allUsers.addAll(r.copyFromRealm(r.where(User::class.java).notEqualTo("id", Common.getClientId()).and().notEqualTo("pending", true).findAll()))
            adapter?.notifyDataSetChanged()
        }

    }

    private val getContacts = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            intent?.let {
                val list: ArrayList<User> = it.getSerializableExtra("users") as ArrayList<User>
                allUsers.clear()
//                allUsers.addAll(list.filter { u -> u.pending == false || u.pending == null })

                adapter?.notifyDataSetChanged()
            }
        }
    }

    fun setUpContactsAdapter() {

        adapter = RecyclerAdapterUtil.Builder(this, allUsers, R.layout.row_contacto)
                .viewsList(R.id.rowName, R.id.rowPic)
                .bindView { itemView, item, position, innerViews ->
                    val textView = innerViews[R.id.rowName] as TextView
                    val imageView = innerViews[R.id.rowPic] as CircleImageView
                    textView.text = allUsers[position].name
                    allUsers[position].avatar?.let {
                        if (it.isNotEmpty())
                            Picasso.with(this@ContactosSearchActivity).load(it).into(imageView)
                    }

                }
                .addClickListener { item, position ->
                    val builder = AlertDialog.Builder(this@ContactosSearchActivity)
                    builder.setTitle(getString(R.string.informacion))
                    val view = LayoutInflater.from(this@ContactosSearchActivity).inflate(R.layout.ad_contacto, null)
                    val tvName = view.findViewById<TextView>(R.id.tvName)
                    val btnAction = view.findViewById<Button>(R.id.btnAction)
                    val civPic = view.findViewById<CircleImageView>(R.id.civPic)
                    tvName.text = allUsers[position].name

                    btnAction.text = getString(R.string.enviar_solicitud)

                    Picasso.with(this@ContactosSearchActivity).load(allUsers[position].avatar).into(civPic)

                    btnAction.setOnClickListener {

                        if (Common.setContactoStatus(allUsers[position].id, Common.SOLICITAR_CONTACTO)) {
//                            allUsers[position].pending = true
                            Realm.getDefaultInstance().executeTransaction { r ->
                                r.insertOrUpdate(allUsers[position])
                            }

                            dialog?.dismiss()
                            updateContactList()
                        }
                    }

                    builder.setView(view)
                    dialog = builder.create()
                    dialog?.show()

                }
                .addLongClickListener { item, position ->
                    //Take action when item is long pressed
                }.build()
        rvList.adapter = adapter

    }

}
