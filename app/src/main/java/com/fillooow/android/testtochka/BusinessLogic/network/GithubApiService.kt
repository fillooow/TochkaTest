package com.fillooow.android.testtochka.BusinessLogic.network

import com.fillooow.android.testtochka.BusinessLogic.network.model.UserSearchModel
import io.reactivex.Observable
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface GithubApiService{

    // max page for unregistered api = 34
    @GET("search/users")
    fun searchUser(@Query("q") q: String,
                   @Query("page") page: Int) : Observable<UserSearchModel.Result>

}
