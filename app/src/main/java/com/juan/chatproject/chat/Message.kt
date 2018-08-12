package com.juan.chatproject.chat

import com.stfalcon.chatkit.commons.models.IMessage
import java.util.*
import io.realm.Realm
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.io.Serializable

open class Message : RealmObject(), IMessage, Serializable {
    @PrimaryKey
    public var id: Int = 0
    private var idServidor: Int = 0
    // Solo selecciona el tipo de chat que es.
    private var userFromId: String = ""
    private var text: String = ""
    public var userFrom: User? = null
    public var userToId: String? = null
    private var fechaCreado: Date? = null
    private var fechaRecibido: Date? = null
    private var fechaLectura: Date? = null

    override fun getId(): String? {
        return userFromId
    }

    override fun getText(): String? {
        return text
    }

    override fun getUser(): User? {
        return userFrom
    }

    override fun getCreatedAt(): Date? {
        return fechaCreado
    }

    fun getID(): Int {
        return this.id
    }

    fun setFechaLectura(fecha: Date) {
        this.fechaLectura = fecha
    }

    fun getFechaLectura(): Date? {
        return this.fechaLectura
    }

    fun setIdServidor(value: Int) {
        this.idServidor = value
    }

    fun getIdServidor(): Int {
        return this.idServidor
    }

    companion object Static {

        fun getMessageConstuctor(realm: Realm, idServidor: Int = 0, clientFrom: String, clientTo: String, message: String, fechaCreado: Date = Date(), fechaRecibido: Date?): Message {
//        fun getMessageConstuctor(realm: Realm, idServidor: Int = 0, clientFrom: String, clientTo: String, message: String, fechaCreado: Date = Date(), fechaRecibido: Date?): Message {

            val m1 = Message()

            realm.executeTransaction {

//                // Check if User exist in our database
//                var user = realm.where(User::class.java).equalTo("id", clientFrom).findFirst()
//                if (user == null) {
//                    user = User(clientFrom, null, null, false, null, null, 0)
//                    it.copyToRealm(user)
//                }

                var maxID = realm.where(Message::class.java).max("id")

                if (maxID == null)
                    maxID = 1

                m1.idServidor = idServidor
                m1.id = maxID.toInt() + 1
                m1.userFromId = clientFrom
                m1.text = message


//                m1.userFrom = user
                m1.userFrom = realm.where(User::class.java).equalTo("id", clientFrom).findFirst()!!
                m1.userToId = clientTo
                m1.fechaCreado = fechaCreado
                m1.fechaRecibido = fechaRecibido

                realm.copyToRealm(m1)
            }

            return m1
        }

    }

}