package com.fillooow.android.testtochka

import android.app.Application
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.fillooow.android.testtochka.di.component.ApplicationComponent
import com.fillooow.android.testtochka.di.component.DaggerApplicationComponent
import com.fillooow.android.testtochka.di.module.ApplicationModule
import com.fillooow.android.testtochka.di.module.GoogleModule
import com.fillooow.android.testtochka.di.module.UserSearchDbModule
import com.fillooow.android.testtochka.di.module.SocialNetworkDbModule
import com.vk.sdk.VKSdk

class BaseApp : Application(){


    lateinit var appComponent: ApplicationComponent

    override fun onCreate() {
        super.onCreate()

        VKSdk.initialize(applicationContext)
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)

        appComponent = initDagger(this)
    }

    private fun initDagger(app: BaseApp): ApplicationComponent =
            DaggerApplicationComponent.builder()
                .applicationModule(ApplicationModule(app))
                .socialNetworkDbModule(SocialNetworkDbModule())
                .userSearchDbModule(UserSearchDbModule(app))
                .googleModule(GoogleModule(app))
                .build()
}
