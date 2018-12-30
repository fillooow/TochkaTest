package com.fillooow.android.testtochka.ui

import com.fillooow.android.testtochka.BusinessLogic.database.SocialNetwork.SocialNetworkData
import com.fillooow.android.testtochka.BusinessLogic.network.model.UserSearchModel

// TODO: один интерфейс для MainActivity
interface MainActivityPresentation {

    fun setSuccessfulLoadedItemsList(items: ArrayList<UserSearchModel.Items>)

    fun setSocialNetworkResults(t: SocialNetworkData)
    fun errorEmptyResult()

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