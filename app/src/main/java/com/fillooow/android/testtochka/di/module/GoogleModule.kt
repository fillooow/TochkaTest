package com.fillooow.android.testtochka.di.module

import android.content.Context
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

// TODO: удалить
@Module
class GoogleModule(private val context: Context){

    @Singleton
    @Provides
    fun provideGoogleSignInOptions(): GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

    @Singleton
    @Provides
    fun provideGoogleApiClient(): GoogleApiClient = GoogleApiClient.Builder(context)
    .addApi(Auth.GOOGLE_SIGN_IN_API, provideGoogleSignInOptions())
    .build()
}