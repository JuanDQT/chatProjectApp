package com.juan.chatproject.chat

import com.stfalcon.chatkit.commons.models.IUser
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class User(@PrimaryKey private var id: String? = "", private var name: String? = "", private var avatar: String? = "", private var isOnline: Boolean? = false, private var lastSeen: Date? = null) : RealmObject(), IUser {

    override fun getId(): String? {
        return id
    }

    override fun getName(): String? {
        return name
    }

    override fun getAvatar(): String? {
        return avatar
    }

    fun setId(id: String) {
        this.id = id
    }

    fun setName(name: String) {
        this.name = name
    }

    fun setAvatar(avatar: String) {
        this.avatar = avatar
    }



}