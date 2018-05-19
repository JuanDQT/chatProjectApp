package com.juan.chatproject

import com.juan.chatproject.chat.Message


class LocalDataBase {

    fun getOlderMessages(): List<Message> {

        var messageList = mutableListOf<Message>()

        for (x in 0..30) {
            messageList.add(Common.getMessageConstuctor((x%2 == 0), "PHONE", "Mensaje: " + x))
//            messageList.add(Common.getMessageConstuctor(false, "PHONE", "Mensaje: " + x))
        }
        return messageList
    }

}