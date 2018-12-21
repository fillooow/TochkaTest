package com.fillooow.android.testtochka.BusinessLogic.database.SocialNetwork

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "socialNetworkData")
data class SocialNetworkData(@PrimaryKey(autoGenerate = true) var id: Long?,
                             @ColumnInfo(name = "label") var label: String?,
                             @ColumnInfo(name = "username") var username: String,
                             @ColumnInfo(name = "photoURL") var photoURL: String?){
    constructor():this(null, null, "user name", null)
}