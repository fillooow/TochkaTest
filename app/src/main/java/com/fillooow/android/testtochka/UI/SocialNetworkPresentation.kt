package com.fillooow.android.testtochka.ui

import com.fillooow.android.testtochka.BusinessLogic.database.SocialNetwork.SocialNetworkData

// TODO: удалить
interface SocialNetworkPresentation {

    fun setSocialNetworkResults(t: SocialNetworkData)
    fun errorEmptyResult()
}