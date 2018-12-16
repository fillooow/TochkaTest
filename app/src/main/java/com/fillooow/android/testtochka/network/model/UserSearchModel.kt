package com.fillooow.android.testtochka.network.model

import java.util.ArrayList

object UserSearchModel {

    data class Result(
        var total_count: Int,
        var incomplete_results: Boolean,
        var items: ArrayList<Items>,

        // If error occurred
        var message: String,
        var errors: ArrayList<Errors>
    )

    data class Items(
        var login: String,
        var id: Long,
        var type: String,
        var avatar_url: String
    )

    data class Errors(
        var resource: String,
        var field: String,
        var code: String
    )
}