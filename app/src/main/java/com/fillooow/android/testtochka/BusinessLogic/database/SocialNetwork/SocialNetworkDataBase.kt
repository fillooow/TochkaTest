package com.fillooow.android.testtochka.BusinessLogic.database.SocialNetwork

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context

@Database(entities = [SocialNetworkData::class], version = 1)
abstract class SocialNetworkDataBase : RoomDatabase(){

    abstract fun socialNetworkDataDao(): SocialNetworkDataDao
}