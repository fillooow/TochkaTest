package com.fillooow.android.testtochka.di.module

import android.app.Application
import android.content.Context
import com.fillooow.android.testtochka.BaseApp
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class ApplicationModule(private val app: Application){

    @Provides
    @Singleton
    fun provideContext(): Context = app

}