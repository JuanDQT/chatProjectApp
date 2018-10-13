package com.juan.chatproject.chat

import com.juan.chatproject.Common
import com.stfalcon.chatkit.commons.models.IUser
import io.realm.Realm
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

open class User(@PrimaryKey private var id: String? = "", private var name: String? = "", private var avatar: String? = "", var isOnline: Boolean = false, private var lastSeen: Date? = null, private var latestMessage: String? = null, var banned: Boolean = false) : RealmObject(), IUser, Serializable {

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
}