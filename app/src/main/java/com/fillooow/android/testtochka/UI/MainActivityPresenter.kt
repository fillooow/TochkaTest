package com.fillooow.android.testtochka.ui

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.facebook.AccessToken
import com.facebook.login.LoginManager
import com.fillooow.android.testtochka.BusinessLogic.database.SocialNetwork.SocialNetworkData
import com.fillooow.android.testtochka.BusinessLogic.database.SocialNetwork.SocialNetworkDataDao
import com.fillooow.android.testtochka.BusinessLogic.database.UserSearch.GithubUserSearchData
import com.fillooow.android.testtochka.BusinessLogic.database.UserSearch.GithubUserSearchDataDao
import com.fillooow.android.testtochka.BusinessLogic.database.UserSearch.UiInfoUserSearchData
import com.fillooow.android.testtochka.BusinessLogic.database.UserSearch.UiInfoUserSearchDataDao
import com.fillooow.android.testtochka.BusinessLogic.network.ConnectivityUtils
import com.fillooow.android.testtochka.BusinessLogic.network.model.UserSearchModel
import com.fillooow.android.testtochka.R
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import com.vk.sdk.VKAccessToken
import com.vk.sdk.VKAccessTokenTracker
import com.vk.sdk.VKSdk
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.lang.Exception
import javax.inject.Inject

class MainActivityPresenter @Inject constructor(private val githubUserSearchDataDao: GithubUserSearchDataDao,
                                                private val uiInfoUserSearchDataDao: UiInfoUserSearchDataDao,
                                                private val socialNetworkDataDao: SocialNetworkDataDao,
                                                private val googleApiClient: GoogleApiClient){

    val compositeDisposable = CompositeDisposable()
    var context: Context? = null
    var mainActivityPresentation: MainActivityPresentation? = null
    val connectivityUtils = ConnectivityUtils()

    fun initInterfaces(context: Context){
        this.context = context
        mainActivityPresentation = context as MainActivityPresentation
    }

    fun loadResponseList(items: ArrayList<UserSearchModel.Items>){
        githubUserSearchDataDao.getAll()
            .subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe(object : SingleObserver<List<GithubUserSearchData>> {
                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onError(e: Throwable) {

                }

                override fun onSuccess(t: List<GithubUserSearchData>) {
                    items.clear()
                    if (t.isNotEmpty()) {
                        for (item in t) {
                            items.add(
                                UserSearchModel.Items(
                                    item.login,
                                    item.githubID,
                                    item.type,
                                    item.avatarUrl
                                )
                            )
                        }
                    }
                    mainActivityPresentation?.setSuccessfulLoadedItemsList(items)

                }
            })
    }

    fun saveResponseList(items: ArrayList<UserSearchModel.Items>){
        for (item in items){
            val githubUserSearchData = GithubUserSearchData()
            githubUserSearchData.githubID = item.id
            githubUserSearchData.avatarUrl = item.avatar_url
            githubUserSearchData.login = item.login
            githubUserSearchData.type = item.type

            Completable.fromAction {
                githubUserSearchDataDao.insert(githubUserSearchData)
            }.subscribeOn(Schedulers.io())
                .subscribe(object : CompletableObserver {
                    override fun onComplete() {
                    }

                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onError(e: Throwable) {
                    }

                })
        }
    }

    fun restoreResponseList(items: ArrayList<UserSearchModel.Items>){
        Completable.fromAction {
            githubUserSearchDataDao.deleteAll()
        }.subscribeOn(Schedulers.io())
            .subscribe(object : CompletableObserver{
                override fun onComplete() {
                    saveResponseList(items)
                }

                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onError(e: Throwable) {

                }

            })
    }


    fun loadUiInfo(){
        uiInfoUserSearchDataDao.getRecord().subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<UiInfoUserSearchData> {
                override fun onSuccess(t: UiInfoUserSearchData) {
                    mainActivityPresentation?.setSuccessfulLoadedUiResults(t.lastSearchTextDB,
                        t.isNextBtnEnabledDB,
                        t.isPrevBtnEnabledDB,
                        t.totalPagesDB,
                        t.currentPageDB,
                        t.totalCountDB,
                        t.hasBtnClickedDB)
                    //inputET.setText(lastSearchText)
                    //updateUI()
                }

                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onError(e: Throwable) {
                    Log.d("Error", "Load UI info error $e")
                }

            })
    }

    fun saveUiInfo(lastSearchText: String?, isNextBtnEnabled: Boolean, isPrevBtnEnabled: Boolean,
                   totalPages: Int, currentPage: Int, totalCount: Int, hasBtnClicked: Boolean){
        val uiUserSearchData = UiInfoUserSearchData()
        uiUserSearchData.lastSearchTextDB = lastSearchText
        uiUserSearchData.isNextBtnEnabledDB = isNextBtnEnabled
        uiUserSearchData.isPrevBtnEnabledDB = isPrevBtnEnabled
        uiUserSearchData.totalPagesDB = totalPages
        uiUserSearchData.currentPageDB = currentPage
        uiUserSearchData.totalCountDB = totalCount
        uiUserSearchData.hasBtnClickedDB = hasBtnClicked

        Completable.fromAction {
            uiInfoUserSearchDataDao.insert(uiUserSearchData)
        }.subscribeOn(Schedulers.io())
            .subscribe(object : CompletableObserver {
                override fun onComplete() {
                }

                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onError(e: Throwable) {
                    Log.d("Error", "Save ui info error $e")}

            })
    }

    fun restoreUiInfo(lastSearchText: String?, isNextBtnEnabled: Boolean, isPrevBtnEnabled: Boolean,
                      totalPages: Int, currentPage: Int, totalCount: Int, hasBtnClicked: Boolean){
        Completable.fromAction {
            uiInfoUserSearchDataDao.deleteAll()
        }.subscribeOn(Schedulers.io())
            .subscribe(object : CompletableObserver{
                override fun onComplete() {
                    saveUiInfo(lastSearchText, isNextBtnEnabled, isPrevBtnEnabled,
                        totalPages, currentPage, totalCount, hasBtnClicked)
                }

                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onError(e: Throwable) {}

            })
    }


    fun loadSocialNetworkLabel() {
        socialNetworkDataDao.getRecord()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(object : SingleObserver<SocialNetworkData> {
                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onSuccess(t: SocialNetworkData) {
                    mainActivityPresentation?.setSocialNetworkResults(t)
                }

                override fun onError(e: Throwable) {
                    if (e.message?.contains("Query returned empty result")!!){
                        mainActivityPresentation?.errorEmptyResult()
                    }
                }

            })
    }

    fun saveSocialNetworkLabel(label: String?, photoURL: String?, userName: String?){
        val socialNetworkData = SocialNetworkData()
        socialNetworkData.label = label
        socialNetworkData.photoURL = photoURL
        socialNetworkData.username = userName

        Completable.fromAction {
            socialNetworkDataDao.insert(socialNetworkData)
        }.subscribeOn(Schedulers.io())
            .subscribe(object : CompletableObserver {
                override fun onComplete() {
                }

                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onError(e: Throwable) {
                    Log.d("Error", "Load label error $e")
                }

            })
    }

    fun restoreNetworkDB(label: String?, photoURL: String?, userName: String?) {
        Completable.fromAction {
            socialNetworkDataDao.deleteAll()
        }.subscribeOn(Schedulers.io())
            .subscribe(object : CompletableObserver{
                override fun onComplete() {
                    saveSocialNetworkLabel(label, photoURL, userName)
                }

                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onError(e: Throwable) {
                    Log.d("Error", "Load label error $e")
                }

            })
    }


    fun setUserPhoto(url: String?, imageView: ImageView){
        // Google+ may return "null" as an answer, if user don't have profile picture
        Picasso.get()
            .load(url)
            .error(R.drawable.default_user_profile_image_png_5)
            .networkPolicy(NetworkPolicy.OFFLINE)
            .into(imageView, object : Callback {
                override fun onSuccess() {

                }

                override fun onError(e: Exception?) {
                    Picasso.get()
                        .load(url)
                        .error(R.drawable.default_user_profile_image_png_5)
                        .into(imageView, object : Callback {
                            override fun onSuccess() {

                            }

                            override fun onError(e: Exception?) {
                                Log.e("Picasso error", "Error $e")
                            }

                        })
                }

            })
    }


    fun showToast(toastText: String){
        Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
    }

    fun checkSocialNetworkTokenState(label: String?) {
        if (connectivityUtils.hasConnection(context)) {
            when (label) {
                LoginActivity.GOOGLE_LABEL -> {
                    googleApiClient.connect()
                    val opr = Auth.GoogleSignInApi.silentSignIn(googleApiClient)
                    if (!opr.isDone) {
                        expiredSession()
                    }
                }
                LoginActivity.FACEBOOK_LABEL -> {
                    val token = AccessToken.getCurrentAccessToken()
                    if (token == null || token.isExpired) {
                        expiredSession()
                    }
                }
                LoginActivity.VKONTAKTE_LABEL -> {
                    val vkAccessTokenTracker = object : VKAccessTokenTracker() {
                        override fun onVKAccessTokenChanged(oldToken: VKAccessToken?, newToken: VKAccessToken?) {
                            if (newToken == null) {
                                expiredSession()
                            }
                        }
                    }
                    vkAccessTokenTracker.startTracking()
                }
                null -> {
                    mainActivityPresentation?.startLoginIntent()
                }
            }
        }
    }

    fun initializeLogoutBtn(label: String?, photoURL: String?, username: String?){
        if (connectivityUtils.hasConnection(context?.applicationContext)) {
            when (label) {
                LoginActivity.GOOGLE_LABEL -> {
                    Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback {

                    }
                }
                LoginActivity.FACEBOOK_LABEL -> {
                    LoginManager.getInstance().logOut()
                }
                LoginActivity.VKONTAKTE_LABEL -> {
                    VKSdk.logout()
                }
                null -> {
                    showToast(context?.getString(R.string.logout_null_error)!!)
                }
                else -> {
                    showToast(context?.getString(R.string.logout_null_error)!!)
                }
            }
            restoreNetworkDB(label, photoURL, username)
            mainActivityPresentation?.startLoginIntent()
        } else {
            showToast(context?.getString(R.string.no_internet_connection)!!)
        }
    }

    fun expiredSession(){
        showToast(context?.getString(R.string.session_token_expired) ?: "Session token expired")
        mainActivityPresentation?.startLoginIntent()
    }

    fun onDestroyPresenter(){
        compositeDisposable.dispose()
        mainActivityPresentation = null
        context = null
    }

}