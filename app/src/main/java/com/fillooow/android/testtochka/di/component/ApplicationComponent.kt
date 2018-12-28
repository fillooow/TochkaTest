package com.fillooow.android.testtochka.di.component

import com.fillooow.android.testtochka.di.module.ApplicationModule
import com.fillooow.android.testtochka.di.module.GithubUserSearchDbModule
import com.fillooow.android.testtochka.di.module.SocialNetworkDbModule
import com.fillooow.android.testtochka.ui.MainActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [
    ApplicationModule::class,
    SocialNetworkDbModule::class,
    GithubUserSearchDbModule::class])
interface ApplicationComponent {

    fun inject(target: MainActivity)

}