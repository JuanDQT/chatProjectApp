package com.juan.chatproject.chat

import android.util.Log
import com.juan.chatproject.Common
import com.stfalcon.chatkit.commons.models.IMessage
import java.util.*
import com.stfalcon.chatkit.commons.models.IUser
import io.realm.Realm
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.io.Serializable
import kotlin.math.max

open class Message : RealmObject(), IMessage, Serializable {
    @PrimaryKey
    public var id: Int = 0
    private var mID: String = ""
    private var text: String = ""
    public var userFrom: User? = null
    public var userToId: String? = null
    private var fechaCreado: Date? = null
    private var fechaRecibido: Date? = null
    private var fechaLectura: Date? = null

    override fun getId(): String? {
        return mID
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

    companion object Static {

        fun getMessageConstuctor(realm: Realm, clientFrom: String, clientTo: String, message: String, fechaCreado: Date = Date(), fechaRecibido: Date?): Message {

            val m1 = Message()

            realm.executeTransaction {

                var maxID = realm.where(Message::class.java).max("id")

                if (maxID == null)
                    maxID = 1

                m1.id = maxID.toInt() + 1
                m1.mID = clientFrom
                m1.text = message
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