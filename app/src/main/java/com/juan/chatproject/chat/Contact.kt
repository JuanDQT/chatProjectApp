package com.juan.chatproject.chat

import com.juan.chatproject.Common
import io.realm.Realm
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

open class Contact(@Index private var id_user_from: String = "", @Index private var id_user_to: String = "", private var status: String = ""): RealmObject() {

    fun getIdUserTo(): String {
        return id_user_to
    }

    fun getIdUserFrom(): String {
        return id_user_from
    }


    companion object access {
        fun getUsersIdJSONArray(realm: Realm): JSONArray {
            val listId = realm.where(User::class.java).notEqualTo("id", Common.getClientId()).findAll().map { u -> u.id }

            if (listId.isEmpty())
                return JSONArray()

            try {

                val jsonIdArray = JSONArray()

                for (id in listId) {
                    jsonIdArray.put(id)
                }

                return jsonIdArray
            } catch (e: JSONException) {

            }

            return JSONArray()
        }
    }
}