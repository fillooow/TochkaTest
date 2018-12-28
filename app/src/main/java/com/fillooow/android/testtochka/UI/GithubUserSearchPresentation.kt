package com.fillooow.android.testtochka.ui

import com.fillooow.android.testtochka.BusinessLogic.network.model.UserSearchModel

interface GithubUserSearchPresentation {

    fun setSuccessfulLoadedItemsList(items: ArrayList<UserSearchModel.Items>)
}