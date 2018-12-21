package com.fillooow.android.testtochka.BusinessLogic.database.UserSearch

import android.arch.persistence.room.*

@Entity(tableName = "uiInfoUserSearchData")
data class UiInfoUserSearchData(@PrimaryKey(autoGenerate = true) var id: Long?,
                                @ColumnInfo(name = "lastSearchTextDB") var lastSearchTextDB: String?,
                                @ColumnInfo(name = "isNextBtnEnabledDB") var isNextBtnEnabledDB: Boolean,
                                @ColumnInfo(name = "isPrevBtnEnabledDB") var isPrevBtnEnabledDB: Boolean,
                                @ColumnInfo(name = "totalPagesDB") var totalPagesDB: Int,
                                @ColumnInfo(name = "currentPageDB") var currentPageDB: Int,
                                @ColumnInfo(name = "totalCountDB") var totalCountDB: Int,
                                @ColumnInfo(name = "hasBtnClicked") var hasBtnClickedDB: Boolean) {
    constructor(): this(null, null, false, false, 0, 0 , 0, false)
}