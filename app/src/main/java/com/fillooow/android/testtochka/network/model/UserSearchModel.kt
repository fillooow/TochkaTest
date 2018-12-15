package com.fillooow.android.testtochka.network.model

import java.util.ArrayList

object UserSearchModel {

    data class Result(
        var totalCount: Int,
        var incomplete_results: Boolean,
        var items: ArrayList<Items>
    )

    data class Items(
        var login: String,
        var id: Long,
        var type: String
    )
}