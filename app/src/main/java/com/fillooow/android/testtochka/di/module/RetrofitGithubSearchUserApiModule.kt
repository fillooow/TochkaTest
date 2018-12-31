package com.fillooow.android.testtochka.di.module

import com.fillooow.android.testtochka.BusinessLogic.network.GithubApiService
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import javax.inject.Singleton

@Module
class RetrofitGithubSearchUserApiModule{

    @Singleton
    @Provides
    fun provideGoogleSearchUserApi(): GithubApiService =  Retrofit.Builder()
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.github.com/")
            .build()
            .create(GithubApiService::class.java)

}