package com.juan.chatproject


import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.juan.chatproject.chat.User
import com.squareup.picasso.Picasso
import com.thetechnocafe.gurleensethi.liteutils.RecyclerAdapterUtil
import com.thetechnocafe.gurleensethi.liteutils.shortToast
import de.hdodenhof.circleimageview.CircleImageView
import io.realm.Realm

class ContactosFragment : Fragment() {

    var allUsers: ArrayList<User> = arrayListOf()
    var adapter: RecyclerAdapterUtil<User>? = null
    var tipoActivity: String? = null
    var dialog: AlertDialog? = null

    var mList: RecyclerView? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater?.inflate(R.layout.fragment_contactos, container, false)

        mList = view?.findViewById<RecyclerView>(R.id.rvContactos)
        mList?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)


        setUpContactsAdapter()

        if (arguments.getString(ContactosActivity().TIPO) == ContactosActivity().TIPO_PENDIENTES) {

            tipoActivity = ContactosActivity().TIPO_PENDIENTES
            val idsContacts = LocalDataBase.getUserIdsSentOrGet("P")
            loadSolicitudesEnviadas(idsContacts)
        } else {
            tipoActivity = ContactosActivity().TIPO_ENVIADAS
            val idsContacts = LocalDataBase.getUserIdsSentOrGet("E")
            loadSolicitudesEnviadas(idsContacts)
        }

        // TODO: validar funcionamiento, recibir peticion, cambiar alertdialog, validar bbdd, que passa si es offline, etc.

        LocalBroadcastManager.getInstance(context).registerReceiver(getContactStatus, IntentFilter(""))

        return view
    }

    val getContactStatus = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            Realm.getDefaultInstance().use { realm ->

                realm.executeTransaction { }
            }

        }
    }

    // OBSERVER - CONTACTS

    fun setUpContactsAdapter() {

        adapter = RecyclerAdapterUtil.Builder(context, allUsers, R.layout.row_contacto)
                .viewsList(R.id.rowName, R.id.rowPic)
                .bindView { itemView, item, position, innerViews ->
                    val textView = innerViews[R.id.rowName] as TextView
                    val imageView = innerViews[R.id.rowPic] as CircleImageView
                    textView.text = allUsers[position].name
                    allUsers[position].avatar?.let {
                        if (it.isNotEmpty())
                            Picasso.with(context).load(it).into(imageView)
                    }

                }
                .addClickListener { item, position ->

                    // TODO: al enviar solicitud, guardar usuario en bbdd
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle(getString(R.string.informacion))
                    val view = LayoutInflater.from(context).inflate(R.layout.ad_contacto, null)
                    val tvName = view.findViewById<TextView>(R.id.tvName)
                    val btnAction = view.findViewById<Button>(R.id.btnAction)
                    val btnActionDeny = view.findViewById<Button>(R.id.btnActionDeny)
                    val civPic = view.findViewById<CircleImageView>(R.id.civPic)
                    tvName.text = allUsers[position].name

                    Picasso.with(context).load(allUsers[position].avatar).into(civPic)

                    if (arguments.getString(ContactosActivity().TIPO) == ContactosActivity().TIPO_PENDIENTES) {

                        btnAction.text = getString(R.string.aceptar_solicitud)
                        btnActionDeny.text = getString(R.string.no_aceptar_solicitud)
                        btnActionDeny.visibility = View.VISIBLE

                        btnAction.setOnClickListener {

                            Common.setContactoStatus(allUsers[position].id, Common.getClientId(), Common.ACEPTAR_CONTACTO)
                            dialog?.dismiss()
                            return@setOnClickListener
                        }

                        btnActionDeny.setOnClickListener {

                            Common.setContactoStatus(allUsers[position].id, Common.getClientId(), Common.DENEGAR_CONTACTO)
                            dialog?.dismiss()
                            return@setOnClickListener
                        }

                    } else {

/*
                        btnAction.setOnClickListener {

                            if (Common.setContactoStatus(allUsers[position].id, Common.CANCELAR_CONTACTO)) {
                                var valorContacto: Boolean? = null
                                when (action) {
                                    Common.SOLICITAR_CONTACTO -> {
                                        valorContacto = true
                                    }
                                    "U" -> {
                                        valorContacto = null
                                    }

                                }
                                // TODO: VALIDAR...

//                                allUsers[position].pending = valorContacto
                                Realm.getDefaultInstance().executeTransaction { r ->
                                    if (action == Common.SOLICITAR_CONTACTO) {
                                        r.insertOrUpdate(allUsers[position])
                                        Log.e(Common.TAGGER, "Contacto anadido")
                                    } else {
                                        r.where(User::class.java).equalTo("id", allUsers[position].id).findFirst()?.deleteFromRealm()
                                    }
                                }
                            }

                            if (arguments.getString(ContactosActivity().TIPO) == ContactosActivity().TIPO_ENVIADAS) {
                                loadRequestContactSent()
                                dialog?.dismiss()
                            }
                        }
*/


//                        allUsers[position].pending?.let { condition ->
//                            btnAction.text = getString(R.string.cancelar_solicitud)
//                            action = Common.CANCELAR_CONTACTO
//                        }
                    }



                    builder.setView(view)

                    dialog = builder.create()
                    dialog?.show()

                }
                .addLongClickListener { item, position ->
                    //Take action when item is long pressed
                }.build()
        mList?.adapter = adapter
    }

    fun loadSolicitudesEnviadas(ids: List<String>?) {

        ids?.let {
            allUsers.clear()
            allUsers.addAll(LocalDataBase.getUsersById(ids))
            Log.e(Common.TAGGER, "Te actualizamos data: " + allUsers.size)

            adapter?.notifyDataSetChanged()
        }
    }

}
