package com.juan.chatproject.chat

import com.juan.chatproject.Common
import com.stfalcon.chatkit.commons.models.IUser
import io.realm.Realm
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

open class User(@PrimaryKey private var id: String? = "", private var name: String? = "", private var avatar: String? = "", var isOnline: Boolean = false, private var lastSeen: Date? = null, private var latestMessage: String? = null, var banned: Boolean = false, var pending: Boolean? = true, var available: Boolean = true) : RealmObject(), IUser, Serializable {

    // Leyenda
    // Pending: // NULL = no hay registros de nada, true = pendiente, false = ya lo tienes anadido
    override fun getId(): String? {
        return id
    }

    override fun getName(): String? {
        return name
    }

    override fun getAvatar(): String? {
        return avatar
    }

    companion object access {
        fun getIdCurrentContacts(realm: Realm): List<String>? {
            val data = realm.where(User::class.java).notEqualTo("id", Common.getClientId()).equalTo("available", true).findAll()

            if (data == null || data.count() == 0)
                return null
            else
                return realm.copyFromRealm(data).map { it.id!! }
        }

    }
}