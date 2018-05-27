package com.juan.chatproject

import com.juan.chatproject.chat.Message
import com.juan.chatproject.chat.User


class LocalDataBase {

    fun getOlderMessages(): List<Message> {

        var messageList = mutableListOf<Message>()

        for (x in 0..30) {
            var from = if (x%2 == 0) "PHONE" else Common.getClientId()
            var to = if (x%2 != 0) "PHONE" else Common.getClientId()
            messageList.add(Common.getMessageConstuctor(from, to, "Mensaje: " + x))
//            messageList.add(Common.getMessageConstuctor(false, "PHONE", "Mensaje: " + x))
        }
        return messageList
    }


    fun getAllUsers(exceptUser: String): List<User> {

        val u1 = User("JQUISPE", "Juan Daniel", "https://i1.wp.com/superflag.com/wp-content/uploads/2015/11/argentinian-flag-medium.jpg", false)
        val u2 = User("ANNEMIJN", "Annemijn", "https://i.ebayimg.com/images/g/ge4AAOxyuOtRaQfY/s-l300.jpg", false)
        val u3 = User("EMULATOR", "Emulador", "http://www.acclaimclipart.com/free_clipart_images/globe_with_north_america_at_the_center_of_the_world_0515-1012-2103-2907_TN.jpg", false)

        var list = mutableListOf(u1, u2, u3)

        list = list.filter { x -> x.id != exceptUser}.toMutableList()

        return list
//        return list.asReversed()
    }
}
