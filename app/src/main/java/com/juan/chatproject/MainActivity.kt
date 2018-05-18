package com.juan.chatproject

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnGoChat.setOnClickListener(){
            val intent = Intent(this@MainActivity, ChatWindowActivity::class.java)
            intent.putExtra("FROM","PHONE")
            intent.putExtra("TO","EMULATOR")
            startActivity(intent)
        }

        btnGoChatEmuladpr.setOnClickListener {
            val intent = Intent(this@MainActivity, ChatWindowActivity::class.java)
            intent.putExtra("FROM","EMULATOR")
            intent.putExtra("TO","PHONE")
            startActivity(intent)
        }

    }
}
