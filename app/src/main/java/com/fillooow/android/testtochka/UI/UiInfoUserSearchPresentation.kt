package com.fillooow.android.testtochka.ui

interface UiInfoUserSearchPresentation{

    fun setSuccessfulLoadedUiResults(
        lastSearchTextDB: String?,
        nextBtnEnabledDB: Boolean,
        prevBtnEnabledDB: Boolean,
        totalPagesDB: Int,
        currentPageDB: Int,
        totalCountDB: Int,
        hasBtnClickedDB: Boolean
    )
}