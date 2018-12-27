package com.fillooow.android.testtochka.ui

import com.fillooow.android.testtochka.BusinessLogic.database.SocialNetwork.SocialNetworkData

interface SocialNetworkPresentation {

    fun setSocialNetworkResults(t: SocialNetworkData)
    fun errorEmptyResult()
}