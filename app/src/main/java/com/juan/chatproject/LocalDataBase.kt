package com.juan.chatproject

import android.util.Log
import com.juan.chatproject.chat.Message
import com.juan.chatproject.chat.User
import io.realm.Realm
import io.realm.Sort
import org.xml.sax.SAXParseException
import java.util.*
import kotlin.collections.ArrayList


class LocalDataBase {

    companion object access {

        fun getOlderMessages(realm: Realm, userTalking: String, currentIndex: Int? = null): List<Message> {

            currentIndex?.let {
                if (realm.where(Message::class.java).lessThan("id", it).and().equalTo("userToId", userTalking).or().equalTo("userFrom.id", userTalking).findAll().count() > 10)
                    return ArrayList(realm.where(Message::class.java).lessThan("id", it).and().beginGroup().equalTo("userToId", userTalking).or().equalTo("userFrom.id", userTalking).endGroup().sort("id", Sort.ASCENDING).findAll().takeLast(10))
                else
                    return ArrayList(realm.where(Message::class.java).lessThan("id", it).and().beginGroup().equalTo("userToId", userTalking).or().equalTo("userFrom.id", userTalking).endGroup().sort("id", Sort.ASCENDING).findAll())
            }
            if (realm.where(Message::class.java).equalTo("userToId", userTalking).or().equalTo("userFrom.id", userTalking).findAll().count() > 10)
                return ArrayList(realm.where(Message::class.java).equalTo("userToId", userTalking).or().equalTo("userFrom.id", userTalking).sort("id", Sort.ASCENDING).findAll().takeLast(10))
            else
                return ArrayList(realm.where(Message::class.java).equalTo("userToId", userTalking).or().equalTo("userFrom.id", userTalking).sort("id", Sort.ASCENDING).findAll())
        }

        fun updateUsers(realm: Realm, list: List<User>) {
            realm.executeTransaction {
                for (item in list) {
                    it.copyToRealmOrUpdate(item)
                }
            }
        }

        fun getAllUsers(realm: Realm, exceptUser: String = Common.getClientId()): List<User> {
            var ent = 0
            return ArrayList(realm.where(User::class.java).notEqualTo("id", exceptUser).and().equalTo("banned", ent).findAll())
        }

        fun saveMessage(realm: Realm, message: Message): Int {
            realm.executeTransaction {
                realm.copyToRealm(message)
            }

            val total = realm.where(Message::class.java).findAll()

            Log.e("TAGGER", "Todos: " + total.count())

            return message.id
        }

        fun getMessageById(realm: Realm, id: Int): Message? {
            return realm.where(Message::class.java).equalTo("id", id).findFirst()
        }

        fun markMessageAsRead(realm: Realm, withClientID: String) {
            realm.executeTransaction {
                val messages = realm.where(Message::class.java).equalTo("userFrom.id", withClientID).isNull("fechaLectura").findAll()
                Log.e("TAGGER", "A actualizar: " + messages.count())
                for (m in messages) {
                    m.setFechaLectura(Date())
                    realm.copyToRealmOrUpdate(m)
                }
            }
        }
    }

}
