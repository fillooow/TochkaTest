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
// 2) "retrofit get error body content kotlin"
//  https://chunksofco.de/getting-json-body-from-retrofit-error-kotlin-6a2281875503
// 3) дефолтное фото
// 4) Adapter ?= presenter/VM
// 5) backstack
//