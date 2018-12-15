package com.fillooow.android.testtochka.network

import com.fillooow.android.testtochka.network.model.UserSearchModel
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

    companion object {
        fun create(): GithubApiService{
            val retrofit = Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("https://api.github.com/")
                .build()

            return retrofit.create(GithubApiService::class.java)
        }
    }
}
