package com.example.testrealm

import android.app.Application
import io.realm.Realm

class MyApplication: Application() {

    override fun onCreate(){
        super.onCreate()

        //Realm 初始化
        Realm.init(this)
    }
}