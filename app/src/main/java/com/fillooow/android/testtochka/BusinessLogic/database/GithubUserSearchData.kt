package com.fillooow.android.testtochka.BusinessLogic.database

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "githubUserSearchData")
data class GithubUserSearchData(@ColumnInfo(name = "login") var login: String = "",
                                @ColumnInfo(name = "githubID") var githubID: Long = 0,
                                @ColumnInfo(name = "type") var type: String = "",
                                @ColumnInfo(name = "avatar_url") var avatarUrl: String = ""){
    @ColumnInfo(name = "id")
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}