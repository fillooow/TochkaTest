package com.fillooow.android.testtochka.BusinessLogic.database.SocialNetwork

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.Query
import io.reactivex.Single

@Dao
interface SocialNetworkDataDao{

    @Query("SELECT * from socialNetworkData WHERE id = (SELECT MAX(id) FROM socialNetworkData)")
    fun getRecord(): Single<SocialNetworkData>

    @Insert(onConflict = REPLACE)
    fun insert(socialNetworkData: SocialNetworkData)

    @Query("DELETE from socialNetworkData")
    fun deleteAll()
}