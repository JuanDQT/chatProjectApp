package com.juan.chatproject

import android.util.Log
import com.juan.chatproject.chat.Message
import com.juan.chatproject.chat.User
import io.realm.Realm
import org.xml.sax.SAXParseException


class LocalDataBase {

    val TAGGER = "TAGGER"

    fun getOlderMessages(): List<Message> {

        var messageList = mutableListOf<Message>()

        for (x in 0..30) {
            var from = if (x % 2 == 0) "PHONE" else Common.getClientId()
            var to = if (x % 2 != 0) "PHONE" else Common.getClientId()
            messageList.add(Common.getMessageConstuctor(from, to, "Mensaje: " + x))
//            messageList.add(Common.getMessageConstuctor(false, "PHONE", "Mensaje: " + x))
        }
        return messageList
    }


    companion object access {

        fun updateUsers(realm: Realm, list: List<User>) {
            realm.executeTransaction {
                for (item in list) {
                        it.copyToRealmOrUpdate(item)
                }
            }
        }

        fun getAllUsers(realm: Realm, exceptUser: String = Common.getClientId()): List<User> {
            var ent = 0
//            var u = realm.where(User::class.java).notEqualTo("id", exceptUser).and().equalTo("banned", ent).findAll().toList()
            var u = ArrayList(realm.where(User::class.java).notEqualTo("id", exceptUser).and().equalTo("banned", ent).findAll())
            return u
        }

    }

}
