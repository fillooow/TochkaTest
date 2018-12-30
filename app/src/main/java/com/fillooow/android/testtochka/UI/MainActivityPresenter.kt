package com.fillooow.android.testtochka.ui

import android.content.Context
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.fillooow.android.testtochka.BusinessLogic.database.SocialNetwork.SocialNetworkData
import com.fillooow.android.testtochka.BusinessLogic.database.SocialNetwork.SocialNetworkDataDao
import com.fillooow.android.testtochka.BusinessLogic.database.UserSearch.GithubUserSearchData
import com.fillooow.android.testtochka.BusinessLogic.database.UserSearch.GithubUserSearchDataDao
import com.fillooow.android.testtochka.BusinessLogic.database.UserSearch.UiInfoUserSearchData
import com.fillooow.android.testtochka.BusinessLogic.database.UserSearch.UiInfoUserSearchDataDao
import com.fillooow.android.testtochka.BusinessLogic.network.model.UserSearchModel
import com.fillooow.android.testtochka.R
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
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
                                                private val socialNetworkDataDao: SocialNetworkDataDao){

    val compositeDisposable = CompositeDisposable()
    var context: Context? = null
    var mainActivityPresentation: MainActivityPresentation? = null

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
        //runOnUiThread {
            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
        //}
    }

    fun onDestroyPresenter(){
        compositeDisposable.dispose()
        mainActivityPresentation = null
        context = null
    }

}