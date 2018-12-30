package com.fillooow.android.testtochka.di.module

import android.arch.persistence.room.Room
import android.content.Context
import com.fillooow.android.testtochka.BusinessLogic.database.SocialNetwork.SocialNetworkDataBase
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class SocialNetworkDbModule {


    @Singleton
    @Provides
    fun provideSocialNetworkDatabase(context: Context): SocialNetworkDataBase =
            Room.databaseBuilder(context,
                SocialNetworkDataBase::class.java,
                "social_network.db")
                .build()

    @Provides
    @Singleton
    fun providesSocialNetworkDao(dataBase: SocialNetworkDataBase) = dataBase.socialNetworkDataDao()

}