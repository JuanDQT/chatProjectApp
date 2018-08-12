package com.juan.chatproject.chat

import com.stfalcon.chatkit.commons.models.IUser
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.io.Serializable
import java.util.*

open class User(@PrimaryKey private var id: String? = "", private var name: String? = "", private var avatar: String? = "", var isOnline: Boolean = false, private var lastSeen: Date? = null, private var latestMessage: String? = null, var banned: Int = 0) : RealmObject(), IUser, Serializable {

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