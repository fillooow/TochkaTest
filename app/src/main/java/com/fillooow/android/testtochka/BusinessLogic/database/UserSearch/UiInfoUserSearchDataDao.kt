package com.fillooow.android.testtochka.BusinessLogic.database.UserSearch

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.reactivex.Single

@Dao
interface UiInfoUserSearchDataDao{

    @Query("SELECT * from uiInfoUserSearchData WHERE id = (SELECT MAX(id) FROM uiInfoUserSearchData)")
    fun getRecord(): Single<UiInfoUserSearchData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(uiInfoUserSearchData: UiInfoUserSearchData)

    @Query("DELETE from uiInfoUserSearchData")
    fun deleteAll()

}