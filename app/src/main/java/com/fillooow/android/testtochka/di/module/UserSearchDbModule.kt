package com.fillooow.android.testtochka.di.module

import android.arch.persistence.room.Room
import android.content.Context
import com.fillooow.android.testtochka.BusinessLogic.database.UserSearch.GithubUserSearchDataBase
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class UserSearchDbModule(private val context: Context){

    @Singleton
    @Provides
    fun provideUserSearchDatabase(context: Context): GithubUserSearchDataBase =
            Room.databaseBuilder(context,
                GithubUserSearchDataBase::class.java,
                "github.db")
                .build()

    @Singleton
    @Provides
    fun providesGithubUserSearchDao(dataBase: GithubUserSearchDataBase) = dataBase.githubUserSearchDataDao()

    @Singleton
    @Provides
    fun providesUiInfoUserSearchDao(dataBase: GithubUserSearchDataBase) = dataBase.uiUserSearchDataDao()

}