package com.fillooow.android.testtochka

import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.vk.sdk.VKSdk

class Application : android.app.Application(){

    override fun onCreate() {
        super.onCreate()
        VKSdk.initialize(applicationContext)
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)
    }
}

// TODO:
// 1) "clear button in edittext android"
// 2) дефолтное фото
// 3) Adapter ?= presenter/VM
// 4) backstack
//