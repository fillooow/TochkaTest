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
