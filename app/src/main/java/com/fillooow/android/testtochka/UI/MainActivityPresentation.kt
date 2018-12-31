package com.fillooow.android.testtochka.ui

interface MainActivityPresentation {

    fun setSuccessfulLoadedUiResults(lastSearchTextDB: String?)
    fun startLoginIntent()
    fun updateUI(currentPage: Int,totalPages: Int, totalCount: Int)
    fun setButtonsState(isNextEnables: Boolean, isPrevEnabled: Boolean)
    fun setSupportActionBarTitle(barTitle: String)
    fun setUserProfile(username: String?)

}