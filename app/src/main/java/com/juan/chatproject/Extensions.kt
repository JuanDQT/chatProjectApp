package com.juan.chatproject

import org.json.JSONArray
import java.util.*

fun List<String>?.toJsonArray(): JSONArray? {

    val arrayData = JSONArray()
    this?.let {
        for (item in it) {
            arrayData.put(item)
        }
    }

    return arrayData
}