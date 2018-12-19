package com.fillooow.android.testtochka

import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.vk.sdk.VKSdk

class Application : android.app.Application(){

    private val FACEBOOK_REQUEST_CODE = 1212

    override fun onCreate() {
        super.onCreate()
        VKSdk.initialize(applicationContext)
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)
    }
}

// TODO:
// 1) "clear button in edittext android"
// 2)