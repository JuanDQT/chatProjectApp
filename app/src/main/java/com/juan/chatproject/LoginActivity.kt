package com.juan.chatproject

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.juan.chatproject.chat.User
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    val numbersAvailable = mapOf<String, String>("1" to "1", "2" to "2", "3" to "3")
//    val numbersAvailable = mapOf<String, String>("JQUISPE" to "1", "ANNEMIJN" to "2", "EMULATOR" to "3")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val sharedPreferences = getSharedPreferences(Common.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

        if (!sharedPreferences.getString("FROM", "").isNullOrEmpty()) {
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
        }

        btnLogin.setOnClickListener {
            if (!etLogin.text.toString().isEmpty() && numbersAvailable.containsKey(etLogin.text.toString())) {

                sharedPreferences.edit().putString("FROM", numbersAvailable.getValue(etLogin.text.toString())).apply()

                val realm = Realm.getDefaultInstance()
                realm.executeTransaction {
                    it.insert(User(numbersAvailable.getValue(etLogin.text.toString()), "JUAN", "", true, null, null, false, false))
//                    it.copyToRealm(User(numbersAvailable.getValue(etLogin.text.toString()), "JUAN", "", true, null, null, false))
                }
                realm.close()

                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                startActivity(intent)
            }
        }
    }
}
