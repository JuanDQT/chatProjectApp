package com.juan.chatproject.chat

import com.stfalcon.chatkit.commons.models.IUser
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey


open class Author : RealmObject(), IUser {

    @PrimaryKey
    private var id: String? = null
    private var name: String? = null
    private var avatar: String? = null

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