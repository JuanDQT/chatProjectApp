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

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater?.inflate(R.layout.fragment_contactos,
                container, false)

        mList = view?.findViewById<RecyclerView>(R.id.rvContactos)

        mList?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)


        setUpContactsAdapter()

        if (arguments.getString(ContactosActivity().TIPO) == ContactosActivity().TIPO_PENDIENTES) {

            tipoActivity = ContactosActivity().TIPO_PENDIENTES
            val sp: SharedPreferences = context.getSharedPreferences(Common.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
            val data = sp.getStringSet(Common.IDS_REQUEST_CONTACT_RECEIVED, hashSetOf())
            context.shortToast("IDS Entrantes: " + data.size)
            Common.searchUsersById(ArrayList(data))
            Log.e(Common.TAGGER, "Cargamos Enviadas")
            LocalBroadcastManager.getInstance(context).registerReceiver(getContacts, IntentFilter("SERCH_USERS_DATA"))
        } else {
            //view?.setBackgroundColor(Color.RED)
            tipoActivity = ContactosActivity().TIPO_ENVIADAS
            loadRequestContactSent()
            Log.e(Common.TAGGER, "Cargamos Pendientes")

        }


        // Boradcaster listener... De si alguien te acepto en la misma pantallla...

        // TODO: validar funcionamiento, recibir peticion, cambiar alertdialog, validar bbdd, que passa si es offline, etc.

        return view
    }

    private val getContacts = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            tipoActivity?.let {

                if (it == ContactosActivity().TIPO_ENVIADAS)
                return


                intent?.let {
                    val list: ArrayList<User> = it.getSerializableExtra("users") as ArrayList<User>
                    allUsers.clear()
                    allUsers.addAll(list)
                    Log.e(Common.TAGGER, "Te actualizamos data: " + list.size)

                    adapter?.notifyDataSetChanged()
                }

            }

        }
    }


    fun loadRequestContactSent() {
        Realm.getDefaultInstance().use { r ->

            allUsers.clear()
            allUsers.addAll(r.copyFromRealm(r.where(User::class.java).notEqualTo("id", Common.getClientId()).and().equalTo("pending", true).findAll()))
            adapter?.notifyDataSetChanged()
        }

    }

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
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle(getString(R.string.informacion))
                    val view = LayoutInflater.from(context).inflate(R.layout.ad_contacto, null)
                    val tvName = view.findViewById<TextView>(R.id.tvName)
                    val btnAction = view.findViewById<Button>(R.id.btnAction)
                    val civPic = view.findViewById<CircleImageView>(R.id.civPic)
                    tvName.text = allUsers[position].name
                    var action = "A" // A --> Add, R --> Remove contact, U --> Remove linea solicitud. Por defecti es A


                    if (allUsers[position].pending == null) {
                        btnAction.text = getString(R.string.enviar_solicitud)
//                        action = "A"
                    } else {
                        allUsers[position].pending?.let { condition ->
                            btnAction.text = if (condition) getString(R.string.solicitud_enviada) else getString(R.string.eliminar_contacto)

                            action = if (condition) "U" else "R"
                        }
                    }
                    Picasso.with(context).load(allUsers[position].avatar).into(civPic)

                    btnAction.setOnClickListener {

                        if (Common.setContactoStatus(allUsers[position].id, action)) {

                            var valorContacto: Boolean? = null
                            when (action) {
                                "A" -> {
                                    btnAction.text = getString(R.string.solicitud_enviada)
                                    valorContacto = true
                                    action = "U"
                                }
                                "R", "U" -> {
                                    btnAction.text = getString(R.string.enviar_solicitud)
                                    valorContacto = null
                                    action = "A"

                                }

                            }
                            // TODO: VALIDAR...

                            allUsers[position].pending = valorContacto
                            Realm.getDefaultInstance().executeTransaction { r ->
                                if (action == "U") {
                                    r.insertOrUpdate(allUsers[position])
                                    Log.e(Common.TAGGER, "Contacto anadido")
                                } else {
                                    r.where(User::class.java).equalTo("id", allUsers[position].id).findFirst()?.deleteFromRealm()
                                }
                            }
                        }

                        if (tipoActivity == "LOCAL") {
                            loadRequestContactSent()
                            dialog?.dismiss()
                        }
                    }

//
                    builder.setView(view)

                    dialog = builder.create()
                    dialog?.show()

                }
                .addLongClickListener { item, position ->
                    //Take action when item is long pressed
                }.build()
        mList?.adapter = adapter

    }

}
