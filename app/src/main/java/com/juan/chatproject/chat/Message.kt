package com.juan.chatproject.chat

import com.stfalcon.chatkit.commons.models.IMessage
import java.util.*
import com.juan.chatproject.chat.Author
import com.stfalcon.chatkit.commons.models.IUser


class Message(): IMessage {

    var mId: String? = null
    var mIuser: IUser? = null
    var mMessage: String? = null

    override fun getId(): String {
        return mId!!
    }

    override fun getCreatedAt(): Date {
        return Date()
    }

    override fun getText(): String {
        return mMessage!!
    }
    override fun getUser(): IUser {
        return mIuser!!
    }

}