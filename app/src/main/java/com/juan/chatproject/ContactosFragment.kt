package com.juan.chatproject


import android.content.*
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
import de.hdodenhof.circleimageview.CircleImageView

class ContactosFragment : Fragment() {

    var allUsers: ArrayList<User> = arrayListOf()
    var adapter: RecyclerAdapterUtil<User>? = null
    var dialog: AlertDialog? = null
    var tipoFragment = ""
    var mList: RecyclerView? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater?.inflate(R.layout.fragment_contactos, container, false)

        mList = view?.findViewById<RecyclerView>(R.id.rvContactos)
        mList?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        setUpContactsAdapter()

        if (arguments.getString(ContactosActivity.TIPO) == ContactosActivity.TIPO_PENDIENTES) {
            tipoFragment = arguments.getString(ContactosActivity.TIPO)
            loadSolicitudes(ContactosActivity.TIPO_PENDIENTES)
        } else {
            tipoFragment = arguments.getString(ContactosActivity.TIPO)
            loadSolicitudes(ContactosActivity.TIPO_ENVIADAS)
        }

        // TODO: validar funcionamiento, recibir peticion, cambiar alertdialog, validar bbdd, que passa si es offline, etc.

        LocalBroadcastManager.getInstance(context).registerReceiver(reloadSolicitudes, IntentFilter("RELOAD_SOLICITUDES"))

        return view
    }

    val reloadSolicitudes = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val type = intent?.extras?.getString("TYPE")

            type?.let {

                if (ForegroundCheckTask().execute(context).get())
                    loadSolicitudes(it)
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

                    if (arguments.getString(ContactosActivity.TIPO) == ContactosActivity.TIPO_PENDIENTES) {

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
                        btnAction.text = getString(R.string.cancelar_solicitud)

                        btnAction.setOnClickListener {

                            Common.setContactoStatus(Common.getClientId(), allUsers[position].id, Common.CANCELAR_ENVIO_SOLICITUD)
                            dialog?.dismiss()
                            return@setOnClickListener
                        }
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

    fun loadSolicitudes(tipo: String) {


        var ids: List<String>? = emptyList<String>()

        ids = LocalDataBase.getUserIdsSentOrGet(tipo)


        Log.e(Common.TAGGER, "TIPO: $tipo , Solicitudes: " + ids?.toString())

        ids?.let {

            if (tipoFragment == tipo) {
                allUsers.clear()
                allUsers.addAll(LocalDataBase.getUsersById(it))
                Log.e(Common.TAGGER, "Te actualizamos data: " + allUsers.size)

                adapter?.notifyDataSetChanged()
            }

        }
    }

}
