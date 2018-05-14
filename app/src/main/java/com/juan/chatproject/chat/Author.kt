package com.juan.chatproject.chat

import com.stfalcon.chatkit.commons.models.IUser


class Author : IUser {

    /*...*/

    override fun getId(): String {
        return id
    }

    override fun getName(): String {
        return name
    }

    override fun getAvatar(): String {
        return avatar
    }
}