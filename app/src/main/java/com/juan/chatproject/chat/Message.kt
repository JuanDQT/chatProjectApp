package com.juan.chatproject.chat

import android.R.attr.author
import com.stfalcon.chatkit.commons.models.IMessage


class Message : IMessage {

    /*...*/

    override fun getId(): String {
        return id
    }

    override fun getText(): String {
        return text
    }

    override fun getUser(): Author {
        return author
    }

    override fun getCreatedAt(): Date {
        return createdAt
    }
}