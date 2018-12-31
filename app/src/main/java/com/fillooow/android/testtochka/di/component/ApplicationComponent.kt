package com.fillooow.android.testtochka.di.component

import com.fillooow.android.testtochka.di.module.*
import com.fillooow.android.testtochka.ui.MainActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [
    ApplicationModule::class,
    SocialNetworkDbModule::class,
    UserSearchDbModule::class,
    GoogleModule::class,
    RetrofitGithubSearchUserApiModule::class])
interface ApplicationComponent {

    fun inject(target: MainActivity)

}