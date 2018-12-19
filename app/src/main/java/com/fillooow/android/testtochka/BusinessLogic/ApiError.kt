package com.fillooow.android.testtochka.BusinessLogic

import com.google.gson.JsonParser
import retrofit2.HttpException

// Required to check error "API rate limit exceeded for IP"
class ApiError constructor(error: Throwable){
    var errorMessage = "An API error occured"

    init {
        if (error is HttpException) {
            val errorJsonString = error.response()
                .errorBody()?.string()

            errorMessage = JsonParser().parse(errorJsonString)
                .asJsonObject["message"]
                .asString
        } else {
            errorMessage = error.message ?: errorMessage
        }
    }
}