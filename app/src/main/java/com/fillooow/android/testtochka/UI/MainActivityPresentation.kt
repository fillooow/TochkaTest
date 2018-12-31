package com.fillooow.android.testtochka.ui

import com.fillooow.android.testtochka.BusinessLogic.database.SocialNetwork.SocialNetworkData
import com.fillooow.android.testtochka.BusinessLogic.network.model.UserSearchModel

interface MainActivityPresentation {

    //fun setSuccessfulLoadedItemsList(items: ArrayList<UserSearchModel.Items>, editTextString: String)

    //fun setSocialNetworkResults(t: SocialNetworkData)

    fun setSuccessfulLoadedUiResults(lastSearchTextDB: String?)

    fun startLoginIntent()
    fun updateUI(currentPage: Int,totalPages: Int, totalCount: Int)
    //fun updateItems(items: ArrayList<UserSearchModel.Items>)
    //fun onErrorLoadItems()
    fun setButtonsState(isNextEnables: Boolean, isPrevEnabled: Boolean)
    fun setSupportActionBarTitle(barTitle: String)
    //fun textChangeAndHasConnection()
    fun setUserProfile(username: String?)
}