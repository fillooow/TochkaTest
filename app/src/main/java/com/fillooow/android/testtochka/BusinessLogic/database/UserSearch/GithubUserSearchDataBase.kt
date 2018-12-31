package com.fillooow.android.testtochka.BusinessLogic.database.UserSearch

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context

@Database(entities = [GithubUserSearchData::class, UiInfoUserSearchData::class], version = 1)
abstract class GithubUserSearchDataBase : RoomDatabase(){

    abstract fun githubUserSearchDataDao(): GithubUserSearchDataDao
    abstract fun uiUserSearchDataDao(): UiInfoUserSearchDataDao

}