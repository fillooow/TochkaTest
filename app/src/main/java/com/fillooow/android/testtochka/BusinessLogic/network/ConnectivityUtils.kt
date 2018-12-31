package com.fillooow.android.testtochka.BusinessLogic.network

import android.content.Context
import android.net.ConnectivityManager

class ConnectivityUtils{

    fun hasConnection(context: Context?): Boolean{
        val cm: ConnectivityManager = context?.applicationContext
            ?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val connectInfo = cm.activeNetworkInfo
        if (connectInfo != null && connectInfo.isConnected){
            return true
        }
        return false
    }

}