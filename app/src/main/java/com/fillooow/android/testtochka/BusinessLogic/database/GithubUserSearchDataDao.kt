package com.fillooow.android.testtochka.BusinessLogic.database

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.Query
import io.reactivex.Flowable

@Dao
interface GithubUserSearchDataDao{

    @Query("SELECT * from githubUserSearchData")
    fun getAll(): List<GithubUserSearchData>

    @Insert(onConflict = REPLACE)
    fun insert(githubUserSearchData: GithubUserSearchData)

    @Query("DELETE from githubUserSearchData")
    fun deleteAll()

}