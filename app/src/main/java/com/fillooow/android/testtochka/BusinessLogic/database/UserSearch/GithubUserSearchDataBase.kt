package com.fillooow.android.testtochka.BusinessLogic.database.UserSearch

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context

@Database(entities = [GithubUserSearchData::class, UiInfoUserSearchData::class], version = 1)
abstract class GithubUserSearchDataBase : RoomDatabase(){

    abstract fun githubUserSearchDataDao(): GithubUserSearchDataDao
    abstract fun uiUserSearchDataDao(): UiInfoUserSearchDataDao

    companion object {
        private var INSTANCE: GithubUserSearchDataBase? = null

        fun getInstance(context: Context): GithubUserSearchDataBase?{
            if (INSTANCE == null){
                synchronized(GithubUserSearchDataBase::class){
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                        GithubUserSearchDataBase::class.java,
                        "github.db")
                        .build()
                }
            }
            return INSTANCE
        }

        fun destroyInstance(){
            INSTANCE = null
        }
    }
}