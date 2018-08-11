package com.juan.chatproject

import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.juan.chatproject.chat.Message
import com.juan.chatproject.chat.User
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import org.xml.sax.SAXParseException
import java.util.*
import java.util.logging.Handler
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class LocalDataBase {

    companion object access {

        val TAGGER = "TAGGER"

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

//            Thread.sleep(2000)
            val m = realm.where(Message::class.java).equalTo("id", id).findFirst()
            return m
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

        fun getLastMessage(realm: Realm, users: RealmResults<User>): HashMap<String, List<String>> {

            val lastUsersMessages = hashMapOf<String, List<String>>()

            for (u in users) {
//            for (u in realm.where(User::class.java).equalTo("banned", i).notEqualTo("id", Common.getClientId()).findAll()) {
//                val lastMessage = realm.where(Message::class.java).equalTo("userFrom.id", u.id).sort("id", Sort.ASCENDING).findFirst()
                val lastMessage = realm.where(Message::class.java).equalTo("userToId", u.id).or().equalTo("userFrom.id", u.id).sort("id", Sort.DESCENDING).findFirst()
                lastMessage?.let {
                    Log.i(TAGGER, "Ultimo mensaje: " + it.text)
                    if (it.userFrom!!.id.equals(u.id) && it.getFechaLectura() == null) {
                        // Si el ultimo mensaje no lo has leido, buscamos mas no leidos. Limite 5
                        Log.e(TAGGER, "El ultimo mensaje esta sin leer")
                        val lastMessagesNotReaded = realm.where(Message::class.java).equalTo("userToId", u.id).or().equalTo("userFrom.id", u.id).sort("id", Sort.DESCENDING).findAll().take(4)
                        var totalMensajesNoLeidos = 0
                        // Validad que sean los ultimos... Aplicar condicion de fechalectura = null y from = u.id

                        for (m in lastMessagesNotReaded) {

                            if (m.getFechaLectura() == null && it.userFrom!!.id.equals(u.id)) {
                                totalMensajesNoLeidos += 1
                                Log.e(TAGGER, "Mensaje sin leer: " + m.text)
                            }

                            if (!it.userFrom!!.id.equals(u.id))
                                break
                        }

                        Log.e(TAGGER,"Total mensajes sin leer: " + totalMensajesNoLeidos)

                        lastUsersMessages[u.id!!] = listOf(lastMessage.text!!, totalMensajesNoLeidos.toString())
                    } else {
                        // recogemos solo un mensaje, el ultimo
                        lastUsersMessages[u.id!!] = listOf(lastMessage.text!!, "0")
                    }
                }
            }

            return lastUsersMessages
        }

        fun getLastMessageAsync(realm: Realm, users: ArrayList<User>) {

            val lastUsersMessages = hashMapOf<String, List<String>>()

            for (u in users) {
                val lastMessage = realm.where(Message::class.java).equalTo("userToId", u.id).or().equalTo("userFrom.id", u.id).sort("id", Sort.DESCENDING).findFirst()
                lastMessage?.let {
                    Log.i(TAGGER, "Ultimo mensaje: " + it.text)
                    if (it.userFrom!!.id.equals(u.id) && it.getFechaLectura() == null) {
                        // Si el ultimo mensaje no lo has leido, buscamos mas no leidos. Limite 5
                        Log.e(TAGGER, "El ultimo mensaje esta sin leer")
                        val lastMessagesNotReaded = realm.where(Message::class.java).equalTo("userToId", u.id).or().equalTo("userFrom.id", u.id).sort("id", Sort.DESCENDING).findAll().take(4)
                        var totalMensajesNoLeidos = 0

                        for (m in lastMessagesNotReaded) {

                            if (it.userFrom!!.id.equals(Common.getClientId()))
                                continue

                            if (!it.userFrom!!.id.equals(u.id))
                                continue

                            if (m.user!!.id == Common.getClientId())
                                continue

                            if (m.getFechaLectura() == null) {
                                totalMensajesNoLeidos += 1
                                Log.e(TAGGER, "Mensaje sin leer: " + m.text)
                            }
                        }

                        Log.e(TAGGER,"Total mensajes sin leer: " + totalMensajesNoLeidos)

                        lastUsersMessages[u.id!!] = listOf(lastMessage.text!!, totalMensajesNoLeidos.toString())
                    } else {
                        // recogemos solo un mensaje, el ultimo
                        lastUsersMessages[u.id!!] = listOf(lastMessage.text!!, "0")
                    }
                }
            }

            val intentMain = Intent("INTENT_GET_SINGLE_ROW_MESSAGE")
            intentMain.putExtra("DATA", lastUsersMessages)
            LocalBroadcastManager.getInstance(Common.getContext()).sendBroadcast(Intent(intentMain))
//            return lastUsersMessages
        }

        fun updateMessageAsSent(realm: Realm, idPda: Int, idServer: Int) {
            val m = realm.where(Message::class.java).equalTo("id", idPda).findFirst()

            realm.executeTransaction {r ->
                m?.let {
                    m.setIdServidor(idServer)
                    r.insertOrUpdate(m)
                }
            }
        }
    }

}
